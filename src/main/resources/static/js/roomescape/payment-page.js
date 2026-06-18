import { app } from "./shared.js";

const {
  state,
  elements,
  savePaymentDraft,
  loadPaymentDraft,
  clearStoredPaymentDraft,
  redirectToPaymentFail,
  setPaymentMessage,
  setFormMessage,
  formatDate,
  formatPrice,
  normalizeTime,
  postJson,
  endpointMessageOr,
  showErrorPopup,
  showToast,
  isUserPage,
  setSourceStatus
} = app;

function moveToPaymentPage(payload, paymentReservation, theme, time) {
  const draft = {
    reservationId: paymentReservation.reservationId,
    amount: paymentReservation.amount,
    orderName: paymentReservation.orderName,
    payload,
    themeName: theme.name,
    timeLabel: normalizeTime(time.startAt)
  };

  savePaymentDraft(draft);
  window.location.assign(`/payments?reservationId=${encodeURIComponent(draft.reservationId)}`);
}

function renderCheckoutSummary(draft) {
  const date = draft.payload?.date;
  const guestName = draft.payload?.guestName || "-";
  const themeName = draft.themeName || draft.orderName || "방탈출 예약";
  const timeLabel = draft.timeLabel || "-";
  const summary = `${formatDate(date)} · ${themeName} · ${timeLabel}`;

  elements.checkoutReservationId.textContent = `예약번호 ${draft.reservationId}`;
  elements.checkoutOrderName.textContent = draft.orderName || themeName;
  elements.checkoutGuestName.textContent = guestName;
  elements.checkoutDateTime.textContent = summary;
  elements.paymentSummary.textContent = summary;
  elements.paymentAmount.textContent = formatPrice(draft.amount);
  elements.paymentButton.disabled = true;
}

async function initializePaymentPage() {
  const params = new URLSearchParams(window.location.search);
  const reservationId = Number(params.get("reservationId"));
  const draft = loadPaymentDraft(reservationId);
  if (!reservationId || !draft) {
    elements.paymentButton.disabled = true;
    setPaymentMessage("결제 정보를 찾을 수 없습니다. 메인 화면에서 다시 예약해주세요.");
    return;
  }

  state.paymentDraft = draft;
  renderCheckoutSummary(draft);

  if (state.mode !== "live") {
    elements.paymentMethod.innerHTML = `<p class="payment-summary">데모 모드에서는 실제 결제위젯을 렌더링하지 않습니다.</p>`;
    setPaymentMessage("라이브 서버에서 결제수단을 선택할 수 있습니다.", "ok");
    return;
  }

  const clientKey = window.ROOMESCAPE_PAYMENT?.clientKey;
  if (!clientKey) {
    setPaymentMessage("Toss 클라이언트 키가 설정되지 않아 결제수단을 렌더링할 수 없습니다.");
    return;
  }
  if (typeof window.TossPayments !== "function") {
    setPaymentMessage("Toss 결제위젯 SDK를 불러오지 못했습니다.");
    return;
  }

  state.paymentRendering = true;
  setPaymentMessage("결제수단을 불러오고 있습니다.");
  try {
    const tossPayments = window.TossPayments(clientKey);
    const widgets = tossPayments.widgets({ customerKey: window.TossPayments.ANONYMOUS });
    state.paymentWidgets = widgets;

    await widgets.setAmount({ currency: "KRW", value: Number(draft.amount) });
    await Promise.all([
      widgets.renderPaymentMethods({ selector: "#paymentMethod", variantKey: "DEFAULT" }),
      widgets.renderAgreement({ selector: "#paymentAgreement", variantKey: "AGREEMENT" })
    ]);

    elements.paymentButton.disabled = false;
    setPaymentMessage("결제수단을 선택한 뒤 결제를 진행해주세요.", "ok");
  } catch (error) {
    setPaymentMessage(endpointMessageOr(error, "결제수단을 불러오지 못했습니다."));
  } finally {
    state.paymentRendering = false;
  }
}

async function submitPayment() {
  const draft = state.paymentDraft;
  if (!draft || state.paymentRendering) {
    return;
  }
  if (state.mode !== "live") {
    setPaymentMessage("데모 모드에서는 결제 요청을 보낼 수 없습니다.");
    return;
  }
  if (!state.paymentWidgets) {
    setPaymentMessage("결제위젯이 준비되지 않았습니다.");
    return;
  }

  elements.paymentButton.disabled = true;
  setPaymentMessage("결제 정보를 저장하고 있습니다.");
  let prepared = null;
  try {
    prepared = await postJson("/payments/prepare", { reservationId: draft.reservationId });
    await state.paymentWidgets.setAmount({ currency: "KRW", value: Number(prepared.amount) });

    await state.paymentWidgets.requestPayment({
      orderId: prepared.orderId,
      orderName: prepared.orderName,
      customerName: draft.payload.guestName,
      successUrl: `${window.location.origin}/?payment=success&reservationId=${encodeURIComponent(draft.reservationId)}`,
      failUrl: `${window.location.origin}/?payment=fail&reservationId=${encodeURIComponent(draft.reservationId)}`
    });
  } catch (error) {
    let message = error.code === "USER_CANCEL"
      ? "결제가 취소되었습니다. 다시 예약해주세요."
      : endpointMessageOr(error, "결제가 완료되지 않았습니다. 다시 예약해주세요.");
    if (prepared?.orderId) {
      try {
        await postJson("/payments/failures", { orderId: prepared.orderId });
      } catch (failureError) {
        message = endpointMessageOr(failureError, "결제 실패 후 예약 취소 처리에 실패했습니다.");
      }
    }
    redirectToPaymentFail(message, draft.reservationId);
  }
}

function cleanPaymentRedirectParams(params) {
  ["payment", "paymentType", "paymentKey", "orderId", "amount", "code", "message", "reservationId"].forEach((key) => {
    params.delete(key);
  });

  const query = params.toString();
  const nextUrl = `${window.location.pathname}${query ? `?${query}` : ""}${window.location.hash}`;
  window.history.replaceState({}, document.title, nextUrl);
}

async function handlePaymentRedirect() {
  if (!isUserPage() || !window.location.search) {
    return;
  }

  const params = new URLSearchParams(window.location.search);
  const paymentStatus = params.get("payment");
  const reservationId = Number(params.get("reservationId")) || null;
  if (paymentStatus === "fail") {
    const message = params.get("message") || "결제가 완료되지 않았습니다.";
    const orderId = params.get("orderId");
    try {
      if (orderId) {
        await postJson("/payments/failures", { orderId });
      }
      showErrorPopup(`${message}\n예약은 취소 처리되었습니다.`, "결제에 실패했습니다");
    } catch (error) {
      showErrorPopup(endpointMessageOr(error, "결제 실패 후 예약 취소 처리에 실패했습니다."), "예약 취소를 확인해주세요");
    } finally {
      clearStoredPaymentDraft(reservationId);
      cleanPaymentRedirectParams(params);
    }
    return;
  }
  if (paymentStatus !== "success") {
    return;
  }

  const paymentKey = params.get("paymentKey");
  const orderId = params.get("orderId");
  const amount = Number(params.get("amount"));
  if (!paymentKey || !orderId || !Number.isFinite(amount) || amount <= 0) {
    showErrorPopup("결제 승인에 필요한 정보가 부족합니다.", "결제를 확인할 수 없습니다");
    cleanPaymentRedirectParams(params);
    return;
  }

  try {
    await postJson("/payments/confirm", { paymentKey, orderId, amount });
    clearStoredPaymentDraft(reservationId);
    showToast("예약이 완료되었습니다.", "결제가 승인되어 예약이 확정되었습니다.");
    setFormMessage("예약이 완료되었습니다.", "ok");
  } catch (error) {
    showErrorPopup(endpointMessageOr(error, "결제 승인 처리에 실패했습니다."), "결제를 확인할 수 없습니다");
  } finally {
    clearStoredPaymentDraft(reservationId);
    cleanPaymentRedirectParams(params);
  }
}

function bindPaymentEvents() {
  elements.paymentButton.addEventListener("click", submitPayment);
  elements.checkoutBackButton.addEventListener("click", () => window.location.assign("/"));
}

export async function initPaymentPage() {
  bindPaymentEvents();
  state.mode = window.location.protocol === "file:" ? "demo" : "live";
  setSourceStatus();
  await initializePaymentPage();
}

export {
  moveToPaymentPage,
  handlePaymentRedirect
};
