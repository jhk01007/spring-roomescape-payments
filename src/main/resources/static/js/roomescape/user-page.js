import { app } from "./shared.js";
import { handlePaymentRedirect, moveToPaymentPage } from "./payment-page.js";

const {
  DEFAULT_DATE,
  DEFAULT_THEME_PRICE,
  SLOT_AVAILABLE,
  SLOT_WAITING,
  SLOT_UNAVAILABLE,
  SLOT_LOADING,
  state,
  demoThemes,
  demoTimes,
  elements,
  posterFor,
  themeImageSource,
  getJson,
  postJson,
  patchJson,
  deleteJson,
  guestNameHeaders,
  endpointMessageOr,
  setSourceStatus,
  selectedTheme,
  selectedTime,
  currentSlotAvailability,
  availabilityLabel,
  availabilityClass,
  resetSlotAvailability,
  normalizeTime,
  formatDate,
  formatPrice,
  reservationStatus,
  statusBadgeHtml,
  isActiveReservation,
  isConfirmedReservation,
  isOccupyingReservation,
  clearPaymentDraft,
  isSameSlot,
  fetchSlotAvailability,
  createDemoPaymentReservation,
  createDemoWaitingReservation,
  createPendingReservation,
  normalizeWaitingReservation,
  getReservationTheme,
  getReservationTime,
  getReservationThemeId,
  getReservationTimeId,
  replaceReservation,
  editDemoReservation,
  cancelDemoReservation,
  escapeHtml,
  showToast,
  setFormMessage,
  setLookupMessage,
  setInlineMessage
} = app;

function renderPopularThemes() {
  const themes = state.popularThemes.slice(0, 10);
  elements.popularList.innerHTML = "";

  if (themes.length === 0) {
    elements.popularList.innerHTML = `<div class="empty">인기 테마가 없습니다.</div>`;
    return;
  }

  themes.forEach((theme, index) => {
    const item = document.createElement("button");
    item.type = "button";
    item.className = "popular-item";
    item.innerHTML = `
      <span class="rank">${index + 1}</span>
      <span>
        <span class="popular-name">${escapeHtml(theme.name)}</span>
        <span class="popular-desc">${escapeHtml(theme.description || "")}</span>
      </span>
    `;
    item.addEventListener("click", () => {
      state.selectedThemeId = theme.id;
      state.selectedTimeId = null;
      resetSlotAvailability();
      clearPaymentDraft();
      renderThemes();
      renderTimes();
    });
    elements.popularList.appendChild(item);
  });
}

function renderThemes() {
  elements.themeGrid.innerHTML = "";
  elements.themeCount.textContent = `${state.themes.length}개 테마`;

  if (state.themes.length === 0) {
    elements.themeGrid.innerHTML = `<div class="empty">등록된 테마가 없습니다.</div>`;
    return;
  }

  state.themes.forEach((theme) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `theme-button${theme.id === state.selectedThemeId ? " selected" : ""}`;
    button.setAttribute("aria-pressed", theme.id === state.selectedThemeId ? "true" : "false");
    const imageSource = themeImageSource(theme);
    button.innerHTML = `
      <img class="theme-thumb" src="${escapeHtml(imageSource)}" alt="${escapeHtml(theme.name)} 썸네일">
      <span class="theme-body">
        <span class="theme-name">${escapeHtml(theme.name)}</span>
        <span class="theme-description">${escapeHtml(theme.description || "")}</span>
        <span class="theme-meta">60분 진행 · ${escapeHtml(formatPrice(theme.price))}</span>
      </span>
    `;
    const img = button.querySelector("img");
    img.addEventListener("error", () => {
      img.src = posterFor(theme);
    }, { once: true });
    button.addEventListener("click", () => {
      state.selectedThemeId = theme.id;
      state.selectedTimeId = null;
      resetSlotAvailability();
      clearPaymentDraft();
      renderThemes();
      renderTimes();
    });
    elements.themeGrid.appendChild(button);
  });
}

async function loadSelectedSlotAvailability() {
  const theme = selectedTheme();
  const time = selectedTime();
  const date = elements.dateInput.value;
  if (!theme || !time || !date) {
    resetSlotAvailability();
    syncSummary();
    return;
  }

  const requestId = state.slotAvailabilityRequestId + 1;
  state.slotAvailabilityRequestId = requestId;
  state.selectedSlotAvailability = { availability: SLOT_LOADING };
  renderTimes();
  syncSummary();

  try {
    const result = await fetchSlotAvailability(date, time.id, theme.id);
    if (state.slotAvailabilityRequestId !== requestId) {
      return;
    }
    state.selectedSlotAvailability = { availability: result.availability || SLOT_UNAVAILABLE };
    renderTimes();
    syncSummary();
  } catch (error) {
    if (state.slotAvailabilityRequestId !== requestId) {
      return;
    }
    state.selectedSlotAvailability = { availability: SLOT_UNAVAILABLE };
    renderTimes();
    syncSummary();
    setFormMessage(endpointMessageOr(error, "슬롯 상태를 확인하지 못했습니다."));
  }
}

function renderTimes() {
  elements.timeGrid.innerHTML = "";
  elements.timeCount.textContent = state.selectedThemeId ? `${state.times.length}개 시간` : "테마를 먼저 선택";

  if (!state.selectedThemeId) {
    elements.timeGrid.innerHTML = `<div class="empty">테마를 선택하면 시간 목록이 표시됩니다.</div>`;
    syncSummary();
    return;
  }

  if (state.times.length === 0) {
    elements.timeGrid.innerHTML = `<div class="empty">등록된 시간이 없습니다.</div>`;
    syncSummary();
    return;
  }

  [...state.times]
    .sort((a, b) => normalizeTime(a.startAt).localeCompare(normalizeTime(b.startAt)))
    .forEach((time) => {
      const button = document.createElement("button");
      button.type = "button";
      const isSelected = time.id === state.selectedTimeId;
      const availability = isSelected ? currentSlotAvailability() : null;
      const slotClass = availabilityClass(availability);
      button.className = `time-button${isSelected ? " selected" : ""}${slotClass ? ` ${slotClass}` : ""}`;
      button.innerHTML = `
        <span>${escapeHtml(normalizeTime(time.startAt))}</span>
        ${isSelected && availability ? `<span class="time-state">${escapeHtml(availabilityLabel(availability))}</span>` : ""}
      `;
      button.addEventListener("click", () => {
        state.selectedTimeId = time.id;
        resetSlotAvailability();
        clearPaymentDraft();
        renderTimes();
        loadSelectedSlotAvailability();
      });
      elements.timeGrid.appendChild(button);
    });

  syncSummary();
}

function syncSummary() {
  const theme = selectedTheme();
  const time = selectedTime();
  const availability = currentSlotAvailability();
  elements.dateNote.textContent = `${formatDate(elements.dateInput.value)} 기준으로 등록된 모든 예약 시간을 선택할 수 있습니다.`;
  elements.summaryDate.textContent = formatDate(elements.dateInput.value);
  elements.summaryTheme.textContent = theme ? theme.name : "-";
  elements.summaryTime.textContent = time ? normalizeTime(time.startAt) : "-";
  elements.summaryStatus.textContent = time ? availabilityLabel(availability) : "-";

  const hasRequiredInput = Boolean(elements.nameInput.value.trim() && theme && time);
  const canReserve = hasRequiredInput &&
    !state.paymentDraft &&
    (availability === SLOT_AVAILABLE || availability === SLOT_WAITING);
  elements.reserveButton.disabled = !canReserve;
  elements.reserveButton.textContent = availability === SLOT_AVAILABLE
    ? "결제하기"
    : availability === SLOT_WAITING
      ? "대기 신청하기"
      : availability === SLOT_LOADING
        ? "상태 확인 중"
        : "신청 불가";

  if (state.paymentDraft) {
    setFormMessage("결제수단을 선택한 뒤 결제를 진행해주세요.", "ok");
    return;
  }
  if (!theme || !time) {
    setFormMessage("테마와 시간을 선택하면 예약 가능 여부를 확인합니다.");
    return;
  }
  if (availability === SLOT_LOADING) {
    setFormMessage("선택한 슬롯의 예약 가능 여부를 확인하고 있습니다.");
    return;
  }
  if (availability === SLOT_AVAILABLE) {
    setFormMessage(hasRequiredInput ? "예약 가능한 슬롯입니다. 결제를 진행할 수 있습니다." : "이름을 입력하면 결제를 시작할 수 있습니다.");
    return;
  }
  if (availability === SLOT_WAITING) {
    setFormMessage(hasRequiredInput ? "이미 점유된 슬롯입니다. 대기 예약을 신청할 수 있습니다." : "이름을 입력하면 대기 예약을 신청할 수 있습니다.");
    return;
  }
  setFormMessage(time ? "현재 이 슬롯은 신청할 수 없습니다." : "이름, 테마, 시간을 모두 선택하면 예약하거나 대기할 수 있습니다.");
}

async function reserve() {
  const theme = selectedTheme();
  const time = selectedTime();
  const name = elements.nameInput.value.trim();
  if (!theme || !time || !name) {
    syncSummary();
    return;
  }

  const payload = {
    guestName: name,
    date: elements.dateInput.value,
    timeId: time.id,
    themeId: theme.id
  };

  try {
    const availabilityResult = await fetchSlotAvailability(payload.date, payload.timeId, payload.themeId);
    const availability = availabilityResult.availability || SLOT_UNAVAILABLE;
    state.selectedSlotAvailability = { availability };
    renderTimes();
    syncSummary();

    let createdReservation = null;
    if (availability === SLOT_AVAILABLE) {
      let paymentReservation = null;
      if (state.mode === "live") {
        paymentReservation = await postJson("/payments/reservations", payload);
        createdReservation = createPendingReservation(payload, paymentReservation);
      } else {
        createdReservation = createDemoPaymentReservation(payload);
        paymentReservation = {
          reservationId: createdReservation.id,
          amount: theme.price ?? DEFAULT_THEME_PRICE,
          orderName: theme.name
        };
        state.demoReservations = [...state.demoReservations, createdReservation];
      }
      state.reservations = [...state.reservations, createdReservation];
      state.selectedSlotAvailability = { availability: SLOT_WAITING };
      moveToPaymentPage(payload, paymentReservation, theme, time);
      return;
    }

    if (availability === SLOT_WAITING) {
      if (state.mode === "live") {
        const waitingReservation = await postJson("/reservations/waiting", payload);
        createdReservation = normalizeWaitingReservation(waitingReservation, payload);
      } else {
        createdReservation = createDemoWaitingReservation(payload);
        state.demoReservations = [...state.demoReservations, createdReservation];
      }
    } else {
      throw new Error("현재 이 슬롯은 신청할 수 없습니다.");
    }
    state.reservations = [...state.reservations, createdReservation];

    const createdStatus = reservationStatus(createdReservation);
    const isWaiting = createdStatus === "WAITING";
    const waitNumber = createdReservation.waitNumber;
    showToast(
      isWaiting ? `${name}님의 대기 신청이 완료되었습니다.` : `${name}님의 예약이 완료되었습니다.`,
      `${formatDate(payload.date)} · ${theme.name} · ${normalizeTime(time.startAt)}${isWaiting && waitNumber ? ` · 대기 ${waitNumber}번` : ""}`
    );
    elements.nameInput.value = "";
    state.selectedTimeId = null;
    resetSlotAvailability();
    clearPaymentDraft();
    renderTimes();
    setFormMessage(isWaiting
      ? `대기 신청이 완료되었습니다.${waitNumber ? ` 현재 대기 ${waitNumber}번입니다.` : ""}`
      : "예약이 완료되었습니다.", "ok");
  } catch (error) {
    setFormMessage(endpointMessageOr(error, "예약 요청에 실패했습니다."), "error");
  }
}

function findReservation(id) {
  return [...state.lookupReservations, ...state.reservations, ...state.demoReservations]
    .find((reservation) => reservation.id === id) || null;
}

function clearEditReservation() {
  state.editingReservationId = null;
  state.editingReservationThemeId = null;
  elements.editReservationForm.hidden = true;
  elements.editReservationForm.reset();
  elements.editReservationMeta.textContent = "";
  elements.editReservationTime.disabled = false;
  setEditReservationMessage("");
}

function clearCancelReservation() {
  state.cancelingReservationId = null;
  elements.cancelReservationForm.hidden = true;
  elements.cancelReservationForm.reset();
  elements.cancelReservationMeta.textContent = "";
  setCancelReservationMessage("");
  syncCancelReservationForm();
}

async function startEditReservation(id) {
  const reservation = findReservation(id);
  if (!reservation) {
    return;
  }

  clearCancelReservation();
  const theme = getReservationTheme(reservation);
  const time = getReservationTime(reservation);
  state.editingReservationId = id;
  state.editingReservationThemeId = getReservationThemeId(reservation);
  elements.editReservationForm.hidden = false;
  elements.editReservationTitle.textContent = `예약 수정 #${id}`;
  elements.editReservationMeta.textContent = `${theme?.name || "-"} · ${normalizeTime(time?.startAt || "-")}`;
  elements.editAuthorizationName.value = "";
  elements.editReservationDate.value = reservation.date;
  renderEditTimeOptions(state.times, getReservationTimeId(reservation));
  setEditReservationMessage("");
  syncEditReservationForm();
  elements.editAuthorizationName.focus();
}

function startCancelReservation(id) {
  const reservation = findReservation(id);
  if (!reservation) {
    return;
  }

  clearEditReservation();
  const theme = getReservationTheme(reservation);
  const time = getReservationTime(reservation);
  state.cancelingReservationId = id;
  elements.cancelReservationForm.hidden = false;
  elements.cancelReservationTitle.textContent = `예약 취소 #${id}`;
  elements.cancelReservationMeta.textContent = `${theme?.name || "-"} · ${formatDate(reservation.date)} · ${normalizeTime(time?.startAt || "-")}`;
  elements.cancelAuthorizationName.value = "";
  setCancelReservationMessage("");
  syncCancelReservationForm();
  elements.cancelAuthorizationName.focus();
}

async function startPaymentReservation(id) {
  const reservation = findReservation(id);
  if (!reservation) {
    return;
  }

  if (reservationStatus(reservation) !== "PENDING") {
    setLookupMessage("결제 대기 상태의 예약만 결제할 수 있습니다.", "error");
    return;
  }

  const theme = getReservationTheme(reservation);
  const time = getReservationTime(reservation);
  const themeId = getReservationThemeId(reservation);
  const timeId = getReservationTimeId(reservation);
  if (!theme || !time || !themeId || !timeId) {
    setLookupMessage("결제에 필요한 예약 정보를 찾을 수 없습니다.", "error");
    return;
  }

  clearEditReservation();
  clearCancelReservation();

  const payload = {
    guestName: reservation.guestName,
    date: reservation.date,
    themeId,
    timeId
  };
  const paymentReservation = {
    reservationId: reservation.id,
    amount: theme.price ?? DEFAULT_THEME_PRICE,
    orderName: theme.name
  };

  state.selectedThemeId = themeId;
  state.selectedTimeId = timeId;
  state.selectedSlotAvailability = { availability: SLOT_WAITING };
  elements.nameInput.value = reservation.guestName || "";
  elements.dateInput.value = reservation.date;

  moveToPaymentPage(payload, paymentReservation, theme, time);
}

function renderLookupReservations(reservations) {
  state.lookupReservations = reservations;
  elements.lookupList.innerHTML = "";
  elements.lookupCount.textContent = `${reservations.length}건`;

  if (reservations.length === 0) {
    elements.lookupList.innerHTML = `<div class="empty">조회된 예약이 없습니다.</div>`;
    return;
  }

  [...reservations]
    .sort((a, b) => String(b.date).localeCompare(String(a.date)) || Number(b.id) - Number(a.id))
    .forEach((reservation) => {
      const theme = getReservationTheme(reservation);
      const time = getReservationTime(reservation);
      const status = reservationStatus(reservation);
      const waitNumber = reservation.waitNumber;
      const disabledActions = status === "CANCELED";
      const canPay = status === "PENDING";
      const row = document.createElement("div");
      row.className = `list-row reservation-row ${status.toLowerCase()}`;
      row.innerHTML = `
        <div class="list-main">
          <span class="list-title">
            ${escapeHtml(reservation.guestName || "예약자")}
            ${statusBadgeHtml(status, waitNumber)}
          </span>
          <span class="list-meta">${escapeHtml(formatDate(reservation.date))} · ${escapeHtml(theme?.name || "-")} · ${escapeHtml(normalizeTime(time?.startAt || "-"))}</span>
        </div>
        <div class="row-actions">
          ${canPay ? `<button class="primary-button compact-button" type="button" data-pay-reservation-id="${reservation.id}">결제하기</button>` : ""}
          ${disabledActions ? "" : `<button class="secondary-button compact-button" type="button" data-edit-reservation-id="${reservation.id}">수정</button>`}
          ${disabledActions ? "" : `<button class="danger-button compact-button" type="button" data-cancel-reservation-id="${reservation.id}">취소</button>`}
        </div>
      `;
      elements.lookupList.appendChild(row);
    });
}

function renderLookupPayments(payments) {
  state.lookupPayments = payments;
  if (!elements.paymentCheckList || !elements.paymentCheckCount) {
    return;
  }

  elements.paymentCheckList.innerHTML = "";
  elements.paymentCheckCount.textContent = `${payments.length}건`;

  if (payments.length === 0) {
    elements.paymentCheckList.innerHTML = `<div class="empty">조회된 결제 정보가 없습니다.</div>`;
    return;
  }

  [...payments]
    .sort((a, b) => String(b.date).localeCompare(String(a.date)) || Number(b.reservationId) - Number(a.reservationId))
    .forEach((payment) => {
      const theme = getReservationTheme(payment);
      const time = getReservationTime(payment);
      const canRetryPayment = payment.status === "REQUIRES_CHECK";
      const row = document.createElement("div");
      row.className = `list-row reservation-row ${String(payment.status || "").toLowerCase()}`;
      row.innerHTML = `
        <div class="list-main">
          <span class="list-title">
            ${escapeHtml(payment.guestName || "예약자")}
            ${statusBadgeHtml(payment.status, 0)}
          </span>
          <span class="list-meta">${escapeHtml(formatDate(payment.date))} · ${escapeHtml(theme?.name || "-")} · ${escapeHtml(normalizeTime(time?.startAt || "-"))}</span>
          <span class="list-meta">주문 ${escapeHtml(payment.orderId || "-")} · 결제키 ${escapeHtml(payment.paymentKey || "-")} · ${escapeHtml(formatPrice(payment.amount))}</span>
        </div>
        <div class="row-actions">
          ${canRetryPayment ? `<button class="primary-button compact-button" type="button" data-retry-payment-id="${payment.reservationId}">결제 승인 재요청</button>` : ""}
        </div>
      `;
      elements.paymentCheckList.appendChild(row);
    });
}

async function fetchLookupData(guestName) {
  if (state.mode !== "live") {
    return {
      reservations: state.demoReservations.filter((reservation) => reservation.guestName === guestName),
      payments: state.demoPayments.filter((payment) => payment.guestName === guestName)
    };
  }

  const [reservationData, paymentData] = await Promise.all([
    getJson("/reservations/me", guestNameHeaders(guestName)),
    getJson("/payments/me", guestNameHeaders(guestName))
  ]);

  return {
    reservations: reservationData.reservations || [],
    payments: paymentData.payments || []
  };
}

async function lookupReservations(event) {
  event.preventDefault();
  const guestName = elements.lookupGuestName.value.trim();
  if (!guestName) {
    setLookupMessage("예약자 이름을 입력해주세요.", "error");
    renderLookupReservations([]);
    renderLookupPayments([]);
    clearEditReservation();
    clearCancelReservation();
    return;
  }

  elements.lookupButton.disabled = true;
  setLookupMessage("예약을 조회하는 중입니다.");
  clearEditReservation();
  clearCancelReservation();

  try {
    const { reservations, payments } = await fetchLookupData(guestName);

    renderLookupReservations(reservations);
    renderLookupPayments(payments);
    const totalCount = reservations.length + payments.length;
    setLookupMessage(totalCount === 0 ? "조회된 예약과 결제가 없습니다." : "예약과 결제 조회가 완료되었습니다.",
      totalCount === 0 ? "" : "ok");
  } catch (error) {
    renderLookupReservations([]);
    renderLookupPayments([]);
    setLookupMessage(endpointMessageOr(error, "예약과 결제 조회에 실패했습니다."), "error");
  } finally {
    elements.lookupButton.disabled = false;
  }
}

async function retryPaymentConfirmation(reservationId) {
  const payment = state.lookupPayments.find((item) => Number(item.reservationId) === reservationId);
  if (!payment?.paymentKey || !payment?.orderId || !payment?.amount) {
    setLookupMessage("결제 승인 재요청에 필요한 정보가 없습니다.", "error");
    return;
  }

  setLookupMessage("결제 승인 결과를 다시 확인하는 중입니다.");
  try {
    const result = await postJson("/payments/confirm", {
      paymentKey: payment.paymentKey,
      orderId: payment.orderId,
      amount: payment.amount
    });
    const guestName = elements.lookupGuestName.value.trim();
    if (guestName) {
      const { reservations, payments } = await fetchLookupData(guestName);
      renderLookupReservations(reservations);
      renderLookupPayments(payments);
    }
    if (result.status === "REQUIRES_CHECK") {
      setLookupMessage("아직 결제 승인 결과 확인이 필요합니다. 잠시 후 다시 시도해주세요.", "error");
      return;
    }
    showToast("예약이 완료되었습니다.", "결제가 승인되어 예약이 확정되었습니다.");
    setLookupMessage("결제 승인 재요청이 완료되었습니다.", "ok");
  } catch (error) {
    setLookupMessage(endpointMessageOr(error, "결제 승인 재요청에 실패했습니다."), "error");
  }
}

function renderEditTimeOptions(times, selectedTimeId = null) {
  elements.editReservationTime.innerHTML = "";
  if (times.length === 0) {
    elements.editReservationTime.innerHTML = `<option value="">등록된 시간 없음</option>`;
    elements.editReservationTime.disabled = true;
    syncEditReservationForm();
    return;
  }

  elements.editReservationTime.disabled = false;
  elements.editReservationTime.innerHTML = `<option value="">시간 선택</option>`;

  [...times]
    .sort((a, b) => normalizeTime(a.startAt).localeCompare(normalizeTime(b.startAt)))
    .forEach((time) => {
      const option = document.createElement("option");
      option.value = time.id;
      option.textContent = normalizeTime(time.startAt);
      elements.editReservationTime.appendChild(option);
    });

  if (times.some((time) => time.id === selectedTimeId)) {
    elements.editReservationTime.value = String(selectedTimeId);
  }
  syncEditReservationForm();
}

function setEditReservationMessage(text, type = "") {
  setInlineMessage(elements.editReservationMessage, "message", text, type);
}

function setCancelReservationMessage(text, type = "") {
  setInlineMessage(elements.cancelReservationMessage, "message", text, type);
}

function syncEditReservationForm() {
  if (!elements.editReservationForm || elements.editReservationForm.hidden) {
    return;
  }

  const canEdit = Boolean(
    state.editingReservationId &&
    elements.editAuthorizationName.value.trim() &&
    elements.editReservationDate.value &&
    elements.editReservationTime.value &&
    !elements.editReservationTime.disabled
  );
  elements.editReservationButton.disabled = !canEdit;
}

function syncCancelReservationForm() {
  if (!elements.cancelReservationForm || elements.cancelReservationForm.hidden) {
    return;
  }

  const canCancel = Boolean(
    state.cancelingReservationId &&
    elements.cancelAuthorizationName.value.trim()
  );
  elements.cancelReservationButton.disabled = !canCancel;
}

async function submitCancelReservation(event) {
  event.preventDefault();
  const reservationId = state.cancelingReservationId;
  const authorizationName = elements.cancelAuthorizationName.value.trim();
  if (!reservationId || !authorizationName) {
    setCancelReservationMessage("예약자 이름을 입력해주세요.", "error");
    syncCancelReservationForm();
    return;
  }

  elements.cancelReservationButton.disabled = true;
  setCancelReservationMessage("예약을 취소하는 중입니다.");

  try {
    let reservations = [];
    let payments = [];
    if (state.mode === "live") {
      await deleteJson(`/reservations/${reservationId}`, guestNameHeaders(authorizationName));
      const lookupData = await fetchLookupData(authorizationName);
      reservations = lookupData.reservations;
      payments = lookupData.payments;
    } else {
      cancelDemoReservation(reservationId, authorizationName);
      reservations = state.demoReservations.filter((reservation) => reservation.guestName === authorizationName);
    }

    state.lookupReservations = reservations;
    renderLookupReservations(state.lookupReservations);
    renderLookupPayments(payments);
    clearCancelReservation();
    showToast("예약이 취소되었습니다.", authorizationName);
    renderTimes();
    setLookupMessage("예약 취소가 완료되었습니다.", "ok");
  } catch (error) {
    setCancelReservationMessage(endpointMessageOr(error, "예약 취소에 실패했습니다."), "error");
    syncCancelReservationForm();
  }
}

async function editReservation(event) {
  event.preventDefault();
  const reservationId = state.editingReservationId;
  const authorizationName = elements.editAuthorizationName.value.trim();
  const payload = {
    date: elements.editReservationDate.value,
    timeId: Number(elements.editReservationTime.value)
  };

  if (!reservationId || !authorizationName || !payload.date || !payload.timeId) {
    setEditReservationMessage("이름, 날짜, 시간을 모두 입력해주세요.", "error");
    syncEditReservationForm();
    return;
  }

  elements.editReservationButton.disabled = true;
  setEditReservationMessage("예약을 수정하는 중입니다.");

  try {
    let reservations = [];
    let payments = [];
    let editedReservation = null;
    if (state.mode === "live") {
      await patchJson(`/reservations/${reservationId}`, payload, guestNameHeaders(authorizationName));
      const lookupData = await fetchLookupData(authorizationName);
      reservations = lookupData.reservations;
      payments = lookupData.payments;
      editedReservation = reservations.find((reservation) => reservation.id === reservationId) || null;
    } else {
      editedReservation = editDemoReservation(reservationId, payload, authorizationName);
      state.demoReservations = replaceReservation(state.demoReservations, editedReservation);
      reservations = state.demoReservations.filter((reservation) => reservation.guestName === authorizationName);
    }
    if (editedReservation) {
      state.reservations = replaceReservation(state.reservations, editedReservation);
    }
    state.lookupReservations = reservations;

    renderLookupReservations(state.lookupReservations);
    renderLookupPayments(payments);
    clearEditReservation();
    showToast("예약이 수정되었습니다.", `${formatDate(payload.date)} · ${normalizeTime(getReservationTime(editedReservation || { timeId: payload.timeId })?.startAt || "")}`);
    renderTimes();
    setLookupMessage("예약 수정이 완료되었습니다.", "ok");
  } catch (error) {
    setEditReservationMessage(endpointMessageOr(error, "예약 수정에 실패했습니다."), "error");
    syncEditReservationForm();
  }
}

function renderUserDemoFirst() {
  state.mode = "demo";
  state.themes = demoThemes;
  state.popularThemes = demoThemes.slice(0, 10);
  state.times = demoTimes.map(({ id, startAt }) => ({ id, startAt }));
  setSourceStatus();

  state.selectedThemeId = state.themes[0]?.id || null;
  state.selectedTimeId = null;
  elements.dateInput.value = DEFAULT_DATE;
  renderPopularThemes();
  renderThemes();
  renderTimes();
  syncSummary();
  renderLookupReservations([]);
  renderLookupPayments([]);
}

async function loadUserInitialData() {
  const isFilePreview = window.location.protocol === "file:";
  if (isFilePreview) {
    renderUserDemoFirst();
    return;
  }

  elements.dateInput.value = DEFAULT_DATE;
  state.mode = "loading";
  setSourceStatus();

  try {
    const [themeData, popularityData, timeData] = await Promise.all([
      getJson("/themes"),
      getJson("/themes/popularity?days=7&size=10"),
      getJson("/times")
    ]);
    state.mode = "live";
    state.themes = themeData.themes || [];
    state.popularThemes = popularityData.themes || popularityData.popularThemes || [];
    state.times = timeData.times || [];
    state.reservations = [];
    setSourceStatus();

    state.selectedThemeId = state.themes[0]?.id || null;
    state.selectedTimeId = null;
    renderPopularThemes();
    renderThemes();
    renderTimes();
    syncSummary();
    renderLookupReservations([]);
    renderLookupPayments([]);
  } catch (error) {
    renderUserDemoFirst();
    setSourceStatus();
  }
}

function bindUserEvents() {
  elements.dateInput.addEventListener("change", () => {
    state.selectedTimeId = null;
    resetSlotAvailability();
    clearPaymentDraft();
    renderTimes();
  });
  elements.nameInput.addEventListener("input", syncSummary);
  elements.reserveButton.addEventListener("click", reserve);
  elements.lookupForm.addEventListener("submit", lookupReservations);
  elements.lookupList.addEventListener("click", (event) => {
    const payButton = event.target.closest("[data-pay-reservation-id]");
    if (payButton) {
      startPaymentReservation(Number(payButton.dataset.payReservationId));
      return;
    }

    const editButton = event.target.closest("[data-edit-reservation-id]");
    if (editButton) {
      startEditReservation(Number(editButton.dataset.editReservationId));
      return;
    }

    const cancelButton = event.target.closest("[data-cancel-reservation-id]");
    if (cancelButton) {
      startCancelReservation(Number(cancelButton.dataset.cancelReservationId));
    }
  });
  elements.paymentCheckList.addEventListener("click", (event) => {
    const retryPaymentButton = event.target.closest("[data-retry-payment-id]");
    if (retryPaymentButton) {
      retryPaymentConfirmation(Number(retryPaymentButton.dataset.retryPaymentId));
    }
  });
  elements.editReservationForm.addEventListener("submit", editReservation);
  elements.editCancelButton.addEventListener("click", clearEditReservation);
  elements.editAuthorizationName.addEventListener("input", syncEditReservationForm);
  elements.editReservationDate.addEventListener("change", syncEditReservationForm);
  elements.editReservationTime.addEventListener("change", syncEditReservationForm);
  elements.cancelReservationForm.addEventListener("submit", submitCancelReservation);
  elements.cancelReservationCloseButton.addEventListener("click", clearCancelReservation);
  elements.cancelAuthorizationName.addEventListener("input", syncCancelReservationForm);
}

export async function initUserPage() {
  bindUserEvents();
  await loadUserInitialData();
  await handlePaymentRedirect();
}
