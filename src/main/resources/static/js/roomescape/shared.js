const API_BASE = "";
const GUEST_NAME_HEADER = "X-Guest-Name";
const DEMO_DATE = "2026-05-06";
const DEFAULT_DATE = todayDate();
const DEFAULT_THEME_PRICE = 50000;
const PAGE = document.body.dataset.page || "user";
const SLOT_AVAILABLE = "AVAILABLE";
const SLOT_WAITING = "WAITING";
const SLOT_UNAVAILABLE = "UNAVAILABLE";
const SLOT_LOADING = "LOADING";
const PAYMENT_DRAFT_STORAGE_PREFIX = "roomescape:payment-draft:";

const state = {
  currentView: "user",
  mode: "demo",
  themes: [],
  popularThemes: [],
  times: [],
  reservations: [],
  lookupReservations: [],
  demoReservations: [
    { id: 1, guestName: "guest-8", date: "2026-05-05", themeId: 1, timeId: 1, status: "CONFIRMED" },
    { id: 2, guestName: "guest-9", date: "2026-05-05", themeId: 1, timeId: 2, status: "CONFIRMED" },
    { id: 3, guestName: "guest-10", date: "2026-05-05", themeId: 1, timeId: 3, status: "CANCELED" },
    { id: 4, guestName: "guest-18", date: "2027-05-05", themeId: 2, timeId: 1, status: "CONFIRMED" },
    { id: 5, guestName: "guest-19", date: "2027-05-05", themeId: 2, timeId: 2, status: "CONFIRMED" },
    { id: 6, guestName: "guest-56", date: "2027-05-06", themeId: 11, timeId: 1, status: "CONFIRMED" },
    { id: 7, guestName: "guest-57", date: "2027-05-06", themeId: 11, timeId: 2, status: "WAITING", waitNumber: 1 }
  ],
  selectedThemeId: null,
  selectedTimeId: null,
  selectedSlotAvailability: null,
  slotAvailabilityRequestId: 0,
  paymentDraft: null,
  paymentWidgets: null,
  paymentRendering: false,
  editingReservationId: null,
  editingReservationThemeId: null,
  cancelingReservationId: null,
  adminSelectedThemeId: null,
  adminSelectedTimeId: null,
  adminReservationPage: 1,
  adminReservationSize: 20,
  adminReservationNumberOfElements: 0,
  adminReservationTotalElements: 0,
  adminReservationTotalPages: 0,
  adminReservationHasPrevious: false,
  adminReservationHasNext: false
};

const demoThemes = Array.from({ length: 12 }, (_, index) => {
  const id = index + 1;
  return {
    id,
    name: `Theme ${id}`,
    description: id === 11 ? "Out of range reservations only" : id === 12 ? "No reservations" : `Popular theme rank ${id}`,
    thumbnail: "",
    price: DEFAULT_THEME_PRICE
  };
});

const demoTimes = [
  { id: 1, startAt: "10:00" },
  { id: 2, startAt: "12:00" },
  { id: 3, startAt: "14:00" },
  { id: 4, startAt: "16:00" },
  { id: 5, startAt: "18:00" },
  { id: 6, startAt: "20:00" }
];

const colors = [
  ["#0e6f70", "#192f35"],
  ["#b84f2f", "#33241f"],
  ["#2f5c9a", "#1b2738"],
  ["#746a35", "#252318"],
  ["#7b3f68", "#2e1e2a"],
  ["#2f7a50", "#182b22"]
];

const $ = (selector) => document.querySelector(selector);

function todayDate() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const date = String(today.getDate()).padStart(2, "0");
  return `${year}-${month}-${date}`;
}

function isAdminPage() {
  return PAGE === "admin";
}

function isUserPage() {
  return PAGE === "user";
}

function isPaymentPage() {
  return PAGE === "payment";
}

const elements = {
  sourceStatus: $("#sourceStatus"),
  popularList: $("#popularList"),
  dateInput: $("#dateInput"),
  dateNote: $("#dateNote"),
  themeGrid: $("#themeGrid"),
  themeCount: $("#themeCount"),
  timeGrid: $("#timeGrid"),
  timeCount: $("#timeCount"),
  nameInput: $("#nameInput"),
  summaryDate: $("#summaryDate"),
  summaryTheme: $("#summaryTheme"),
  summaryTime: $("#summaryTime"),
  reserveButton: $("#reserveButton"),
  formMessage: $("#formMessage"),
  lookupForm: $("#lookupForm"),
  lookupGuestName: $("#lookupGuestName"),
  lookupButton: $("#lookupButton"),
  lookupMessage: $("#lookupMessage"),
  lookupList: $("#lookupList"),
  lookupCount: $("#lookupCount"),
  summaryStatus: $("#summaryStatus"),
  paymentPanel: $("#paymentPanel"),
  paymentSummary: $("#paymentSummary"),
  paymentAmount: $("#paymentAmount"),
  paymentMethod: $("#paymentMethod"),
  paymentAgreement: $("#paymentAgreement"),
  paymentButton: $("#paymentButton"),
  paymentMessage: $("#paymentMessage"),
  checkoutReservationId: $("#checkoutReservationId"),
  checkoutOrderName: $("#checkoutOrderName"),
  checkoutGuestName: $("#checkoutGuestName"),
  checkoutDateTime: $("#checkoutDateTime"),
  checkoutBackButton: $("#checkoutBackButton"),
  editReservationForm: $("#editReservationForm"),
  editReservationTitle: $("#editReservationTitle"),
  editReservationMeta: $("#editReservationMeta"),
  editAuthorizationName: $("#editAuthorizationName"),
  editReservationDate: $("#editReservationDate"),
  editReservationTime: $("#editReservationTime"),
  editReservationButton: $("#editReservationButton"),
  editReservationMessage: $("#editReservationMessage"),
  editCancelButton: $("#editCancelButton"),
  cancelReservationForm: $("#cancelReservationForm"),
  cancelReservationTitle: $("#cancelReservationTitle"),
  cancelReservationMeta: $("#cancelReservationMeta"),
  cancelAuthorizationName: $("#cancelAuthorizationName"),
  cancelReservationButton: $("#cancelReservationButton"),
  cancelReservationMessage: $("#cancelReservationMessage"),
  cancelReservationCloseButton: $("#cancelReservationCloseButton"),
  themeForm: $("#themeForm"),
  timeForm: $("#timeForm"),
  adminThemeName: $("#adminThemeName"),
  adminThemeDescription: $("#adminThemeDescription"),
  adminThemeThumbnail: $("#adminThemeThumbnail"),
  adminThemePrice: $("#adminThemePrice"),
  adminReservationForm: $("#adminReservationForm"),
  adminReserveName: $("#adminReserveName"),
  adminReserveDate: $("#adminReserveDate"),
  adminReserveTheme: $("#adminReserveTheme"),
  adminReserveTimeGrid: $("#adminReserveTimeGrid"),
  adminReserveSummary: $("#adminReserveSummary"),
  adminReserveButton: $("#adminReserveButton"),
  adminReserveMessage: $("#adminReserveMessage"),
  adminTimeStartAt: $("#adminTimeStartAt"),
  adminMessage: $("#adminMessage"),
  adminReservationList: $("#adminReservationList"),
  adminThemeList: $("#adminThemeList"),
  adminTimeList: $("#adminTimeList"),
  reservationCount: $("#reservationCount"),
  adminReservationPageSize: $("#adminReservationPageSize"),
  adminReservationPrevPage: $("#adminReservationPrevPage"),
  adminReservationNextPage: $("#adminReservationNextPage"),
  adminReservationPageLabel: $("#adminReservationPageLabel"),
  adminThemeCount: $("#adminThemeCount"),
  adminTimeCount: $("#adminTimeCount"),
  toast: $("#toast"),
  errorPopup: $("#errorPopup"),
  errorPopupTitle: $("#errorPopupTitle"),
  errorPopupMessage: $("#errorPopupMessage"),
  errorPopupClose: $("#errorPopupClose")
};

let popupPreviousFocus = null;

function posterFor(theme) {
  const [start, end] = colors[(theme.id - 1) % colors.length];
  const canvas = document.createElement("canvas");
  canvas.width = 520;
  canvas.height = 320;
  const context = canvas.getContext("2d");
  const gradient = context.createLinearGradient(0, 0, 520, 320);
  gradient.addColorStop(0, start);
  gradient.addColorStop(1, end);
  context.fillStyle = gradient;
  context.fillRect(0, 0, 520, 320);
  context.fillStyle = "rgba(255,255,255,0.14)";
  for (let i = 0; i < 8; i += 1) {
    context.fillRect(42 + i * 58, 52, 26, 216);
  }
  context.fillStyle = "rgba(0,0,0,0.22)";
  context.fillRect(0, 232, 520, 88);
  context.fillStyle = "#fffdf8";
  context.font = "800 42px system-ui, sans-serif";
  context.fillText(theme.name, 34, 286);
  return canvas.toDataURL("image/png");
}

function themeImageSource(theme) {
  const thumbnail = String(theme.thumbnail || "").trim();
  if (!thumbnail || thumbnail.includes("example.com/")) {
    return posterFor(theme);
  }
  return thumbnail;
}

async function getJson(path, headers = {}) {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), 5000);
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      Accept: "application/json",
      ...headers
    },
    signal: controller.signal
  }).finally(() => window.clearTimeout(timer));
  if (!response.ok) {
    throw new Error(await errorMessageFrom(response));
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

async function postJson(path, body) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    throw new Error(await errorMessageFrom(response));
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

async function patchJson(path, body, headers = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "PATCH",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...headers
    },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    throw new Error(await errorMessageFrom(response));
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

async function deleteJson(path, headers = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "DELETE",
    headers: {
      Accept: "application/json",
      ...headers
    }
  });
  if (!response.ok) {
    throw new Error(await errorMessageFrom(response));
  }
}

function guestNameHeaders(guestName) {
  return {
    [GUEST_NAME_HEADER]: encodeURIComponent(guestName)
  };
}

async function errorMessageFrom(response) {
  const fallback = `HTTP ${response.status}`;
  const contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) {
    return fallback;
  }

  try {
    const body = await response.json();
    if (Array.isArray(body.messages) && body.messages.length > 0) {
      return body.messages.filter(Boolean).join("\n");
    }
    if (typeof body.message === "string" && body.message.trim()) {
      return body.message;
    }
    return fallback;
  } catch (error) {
    return fallback;
  }
}

function endpointMessageOr(error, fallback) {
  if (error instanceof Error && error.message && !/^HTTP \d+$/.test(error.message)) {
    return error.message;
  }
  return fallback;
}

function paymentDraftStorageKey(reservationId) {
  return `${PAYMENT_DRAFT_STORAGE_PREFIX}${reservationId}`;
}

function savePaymentDraft(draft) {
  window.sessionStorage.setItem(
    paymentDraftStorageKey(draft.reservationId),
    JSON.stringify(draft)
  );
}

function loadPaymentDraft(reservationId) {
  if (!reservationId) {
    return null;
  }
  const saved = window.sessionStorage.getItem(paymentDraftStorageKey(reservationId));
  if (!saved) {
    return null;
  }
  try {
    const draft = JSON.parse(saved);
    return Number(draft?.reservationId) === Number(reservationId) ? draft : null;
  } catch (error) {
    return null;
  }
}

function clearStoredPaymentDraft(reservationId) {
  if (reservationId) {
    window.sessionStorage.removeItem(paymentDraftStorageKey(reservationId));
  }
}

function redirectToPaymentFail(message, reservationId = null) {
  const params = new URLSearchParams();
  params.set("payment", "fail");
  if (reservationId) {
    params.set("reservationId", reservationId);
  }
  if (message) {
    params.set("message", message);
  }
  window.location.assign(`/?${params.toString()}`);
}

function showErrorPopup(message, title = "확인해주세요") {
  const text = String(message || "요청을 처리하지 못했습니다.");
  if (!elements.errorPopup) {
    window.alert(text);
    return;
  }

  popupPreviousFocus = document.activeElement;
  elements.errorPopupTitle.textContent = title;
  elements.errorPopupMessage.textContent = text;
  elements.errorPopup.hidden = false;
  elements.errorPopup.classList.add("show");
  document.body.classList.add("popup-open");
  elements.errorPopupClose.focus();
}

function hideErrorPopup() {
  if (!elements.errorPopup || elements.errorPopup.hidden) {
    return;
  }

  elements.errorPopup.classList.remove("show");
  elements.errorPopup.hidden = true;
  document.body.classList.remove("popup-open");

  if (popupPreviousFocus && typeof popupPreviousFocus.focus === "function") {
    popupPreviousFocus.focus();
  }
  popupPreviousFocus = null;
}

function setInlineMessage(element, baseClass, text, type = "") {
  if (!element) {
    return;
  }

  if (type === "error") {
    element.textContent = "";
    element.className = baseClass;
    showErrorPopup(text);
    return;
  }

  element.textContent = text;
  element.className = `${baseClass}${type ? ` ${type}` : ""}`;
}

function setFormMessage(text, type = "") {
  setInlineMessage(elements.formMessage, "message", text, type);
}

function setPaymentMessage(text, type = "") {
  setInlineMessage(elements.paymentMessage, "message", text, type);
}

function setLookupMessage(text, type = "") {
  setInlineMessage(elements.lookupMessage, "message", text, type);
}

async function getReservationListData(page = state.adminReservationPage, size = state.adminReservationSize) {
  return getJson(`/admin/reservations?page=${page}&size=${size}`);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function setSourceStatus() {
  elements.sourceStatus.classList.toggle("live", state.mode === "live");
  const label = state.mode === "live"
    ? "Spring 서버 연결됨"
    : state.mode === "loading"
      ? "데이터 불러오는 중"
      : "데모 데이터";
  elements.sourceStatus.querySelector("span:last-child").textContent = label;
}

function selectedTheme() {
  return state.themes.find((theme) => theme.id === state.selectedThemeId) || null;
}

function selectedTime() {
  return state.times.find((time) => time.id === state.selectedTimeId) || null;
}

function currentSlotAvailability() {
  return state.selectedSlotAvailability?.availability || null;
}

function availabilityLabel(availability) {
  if (availability === SLOT_AVAILABLE) {
    return "예약 가능";
  }
  if (availability === SLOT_WAITING) {
    return "대기 가능";
  }
  if (availability === SLOT_UNAVAILABLE) {
    return "신청 불가";
  }
  if (availability === SLOT_LOADING) {
    return "상태 확인 중";
  }
  return "상태 미확인";
}

function availabilityClass(availability) {
  if (availability === SLOT_AVAILABLE) {
    return "available";
  }
  if (availability === SLOT_WAITING) {
    return "waiting";
  }
  if (availability === SLOT_UNAVAILABLE) {
    return "unavailable";
  }
  return "";
}

function resetSlotAvailability() {
  state.slotAvailabilityRequestId += 1;
  state.selectedSlotAvailability = null;
}

function selectedAdminTheme() {
  return state.themes.find((theme) => theme.id === state.adminSelectedThemeId) || null;
}

function selectedAdminTime() {
  return state.times.find((time) => time.id === state.adminSelectedTimeId) || null;
}

function normalizeTime(startAt) {
  return String(startAt).slice(0, 5);
}

function formatDate(dateText) {
  if (!dateText) {
    return "-";
  }
  const [year, month, date] = dateText.split("-");
  return `${year}.${month}.${date}`;
}

function formatPrice(price) {
  return `${Number(price ?? DEFAULT_THEME_PRICE).toLocaleString("ko-KR")}원`;
}

function reservationStatus(reservation) {
  return reservation.status || "CONFIRMED";
}

function statusLabel(status) {
  if (status === "WAITING") {
    return "대기";
  }
  if (status === "PENDING") {
    return "결제 대기";
  }
  if (status === "CANCELED") {
    return "취소";
  }
  return "확정";
}

function statusBadgeHtml(status, waitNumber) {
  const badgeClass = status === "WAITING"
    ? "waiting"
    : status === "PENDING"
      ? "pending"
      : status === "CANCELED"
        ? "canceled"
        : "confirmed";
  const label = status === "WAITING" && waitNumber
    ? `${statusLabel(status)} ${waitNumber}번`
    : statusLabel(status);
  return `<span class="status-badge ${badgeClass}">${escapeHtml(label)}</span>`;
}

function isActiveReservation(reservation) {
  return reservationStatus(reservation) !== "CANCELED";
}

function isConfirmedReservation(reservation) {
  return reservationStatus(reservation) === "CONFIRMED";
}

function isOccupyingReservation(reservation) {
  const status = reservationStatus(reservation);
  return status === "CONFIRMED" || status === "PENDING";
}

function clearPaymentDraft() {
  state.paymentDraft = null;
  state.paymentWidgets = null;
  state.paymentRendering = false;
  if (!elements.paymentPanel) {
    return;
  }

  elements.paymentPanel.hidden = true;
  elements.paymentSummary.textContent = "";
  elements.paymentAmount.textContent = "";
  elements.paymentMethod.innerHTML = "";
  elements.paymentAgreement.innerHTML = "";
  elements.paymentButton.disabled = true;
  setPaymentMessage("");
}

function isSameSlot(reservation, date, timeId, themeId) {
  return reservation.date === date &&
    getReservationTimeId(reservation) === Number(timeId) &&
    getReservationThemeId(reservation) === Number(themeId);
}

function demoSlotAvailability(date, timeId, themeId) {
  const time = state.times.find((item) => item.id === Number(timeId));
  if (!time) {
    return SLOT_UNAVAILABLE;
  }

  const slotDateTime = new Date(`${date}T${normalizeTime(time.startAt)}:00`);
  if (!Number.isNaN(slotDateTime.getTime()) && slotDateTime < new Date()) {
    return SLOT_UNAVAILABLE;
  }

  const activeReservations = state.demoReservations.filter((reservation) =>
    isActiveReservation(reservation) && isSameSlot(reservation, date, timeId, themeId)
  );
  if (activeReservations.some(isOccupyingReservation)) {
    return SLOT_WAITING;
  }
  if (activeReservations.length > 0) {
    return SLOT_UNAVAILABLE;
  }
  return SLOT_AVAILABLE;
}

async function fetchSlotAvailability(date, timeId, themeId) {
  if (state.mode !== "live") {
    return { availability: demoSlotAvailability(date, timeId, themeId) };
  }

  const params = new URLSearchParams({
    date,
    timeId: String(timeId),
    themeId: String(themeId)
  });
  return getJson(`/reservations/availability?${params.toString()}`);
}

function nextDemoWaitNumber(date, timeId, themeId, excludedId = null) {
  return state.demoReservations.filter((reservation) =>
    reservation.id !== excludedId &&
    isActiveReservation(reservation) &&
    reservationStatus(reservation) === "WAITING" &&
    isSameSlot(reservation, date, timeId, themeId)
  ).length + 1;
}

function hasDemoConfirmedReservation(date, timeId, themeId, excludedId = null) {
  return state.demoReservations.some((reservation) =>
    reservation.id !== excludedId &&
    isActiveReservation(reservation) &&
    isOccupyingReservation(reservation) &&
    isSameSlot(reservation, date, timeId, themeId)
  );
}

function hasDuplicateDemoReservation(guestName, date, timeId, themeId, excludedId = null) {
  return state.demoReservations.some((reservation) =>
    reservation.id !== excludedId &&
    isActiveReservation(reservation) &&
    reservation.guestName === guestName &&
    isSameSlot(reservation, date, timeId, themeId)
  );
}

function createDemoReservation(payload) {
  const status = hasDemoConfirmedReservation(payload.date, payload.timeId, payload.themeId)
    ? "WAITING"
    : "CONFIRMED";
  if (hasDuplicateDemoReservation(payload.guestName, payload.date, payload.timeId, payload.themeId)) {
    throw new Error("이미 같은 시간대에 대기 중입니다.");
  }

  const waitNumber = status === "WAITING"
    ? nextDemoWaitNumber(payload.date, payload.timeId, payload.themeId)
    : null;

  return {
    id: getNextId(state.demoReservations),
    guestName: payload.guestName,
    date: payload.date,
    themeId: payload.themeId,
    timeId: payload.timeId,
    status,
    ...(waitNumber ? { waitNumber } : {})
  };
}

function createDemoPaymentReservation(payload) {
  if (demoSlotAvailability(payload.date, payload.timeId, payload.themeId) !== SLOT_AVAILABLE) {
    throw new Error("예약 가능한 슬롯만 결제를 시작할 수 있습니다.");
  }
  return {
    id: getNextId(state.demoReservations),
    guestName: payload.guestName,
    date: payload.date,
    themeId: payload.themeId,
    timeId: payload.timeId,
    status: "PENDING"
  };
}

function createDemoWaitingReservation(payload) {
  if (demoSlotAvailability(payload.date, payload.timeId, payload.themeId) !== SLOT_WAITING) {
    throw new Error("대기 가능한 슬롯만 대기 예약을 신청할 수 있습니다.");
  }
  if (hasDuplicateDemoReservation(payload.guestName, payload.date, payload.timeId, payload.themeId)) {
    throw new Error("이미 같은 시간대에 대기 중입니다.");
  }

  const waitNumber = nextDemoWaitNumber(payload.date, payload.timeId, payload.themeId);
  return {
    id: getNextId(state.demoReservations),
    guestName: payload.guestName,
    date: payload.date,
    themeId: payload.themeId,
    timeId: payload.timeId,
    status: "WAITING",
    waitNumber
  };
}

function createPendingReservation(payload, paymentReservation) {
  return {
    id: paymentReservation.reservationId,
    guestName: payload.guestName,
    date: payload.date,
    themeId: payload.themeId,
    timeId: payload.timeId,
    status: "PENDING"
  };
}

function normalizeWaitingReservation(response, payload) {
  return {
    id: response.id,
    guestName: response.guestName || payload.guestName,
    date: response.date || payload.date,
    theme: response.theme,
    time: response.time,
    themeId: response.theme?.id || payload.themeId,
    timeId: response.time?.id || payload.timeId,
    status: response.status || "WAITING",
    waitNumber: response.waitNumber
  };
}

function getNextId(items) {
  return Math.max(0, ...items.map((item) => Number(item.id) || 0)) + 1;
}

function getReservationTheme(reservation) {
  return reservation.theme || state.themes.find((theme) => theme.id === reservation.themeId) || null;
}

function getReservationTime(reservation) {
  return reservation.time || state.times.find((time) => time.id === reservation.timeId) || null;
}

function getReservationThemeId(reservation) {
  return Number(reservation.themeId || reservation.theme?.id);
}

function getReservationTimeId(reservation) {
  return Number(reservation.timeId || reservation.time?.id);
}

function replaceReservation(reservations, editedReservation) {
  return reservations.map((reservation) =>
    reservation.id === editedReservation.id ? editedReservation : reservation
  );
}

function demoWaitingReservationsForSlot(date, timeId, themeId) {
  return state.demoReservations
    .filter((reservation) =>
      isActiveReservation(reservation) &&
      reservationStatus(reservation) === "WAITING" &&
      isSameSlot(reservation, date, timeId, themeId)
    )
    .sort((a, b) => Number(a.id) - Number(b.id));
}

function refreshDemoWaitNumbers(date, timeId, themeId) {
  demoWaitingReservationsForSlot(date, timeId, themeId)
    .forEach((reservation, index) => {
      reservation.waitNumber = index + 1;
    });
}

function promoteTopDemoWaiting(date, timeId, themeId) {
  const [topWaiting] = demoWaitingReservationsForSlot(date, timeId, themeId);
  if (!topWaiting) {
    return;
  }

  topWaiting.status = "CONFIRMED";
  delete topWaiting.waitNumber;
  refreshDemoWaitNumbers(date, timeId, themeId);
}

function editDemoReservation(id, payload, authorizationName) {
  const reservation = state.demoReservations.find((item) => item.id === id);
  if (!reservation) {
    throw new Error("존재하지 않는 예약입니다.");
  }
  if (reservation.guestName !== authorizationName) {
    throw new Error("본인의 예약만 수정할 수 있습니다.");
  }
  if (reservationStatus(reservation) === "CANCELED") {
    throw new Error("이미 취소된 예약입니다.");
  }

  const beforeDate = reservation.date;
  const beforeTimeId = getReservationTimeId(reservation);
  const themeId = getReservationThemeId(reservation);
  const wasConfirmed = isConfirmedReservation(reservation);
  const slotChanged = beforeDate !== payload.date || beforeTimeId !== payload.timeId;

  if (hasDuplicateDemoReservation(reservation.guestName, payload.date, payload.timeId, themeId, id)) {
    throw new Error("이미 존재하는 예약입니다.");
  }

  const status = hasDemoConfirmedReservation(payload.date, payload.timeId, themeId, id)
    ? "WAITING"
    : "CONFIRMED";
  const waitNumber = status === "WAITING"
    ? nextDemoWaitNumber(payload.date, payload.timeId, themeId, id)
    : null;

  reservation.date = payload.date;
  reservation.timeId = payload.timeId;
  reservation.status = status;
  if (waitNumber) {
    reservation.waitNumber = waitNumber;
  } else {
    delete reservation.waitNumber;
  }

  if (wasConfirmed && (slotChanged || status !== "CONFIRMED")) {
    promoteTopDemoWaiting(beforeDate, beforeTimeId, themeId);
  } else if (slotChanged) {
    refreshDemoWaitNumbers(beforeDate, beforeTimeId, themeId);
  }
  if (status === "WAITING") {
    refreshDemoWaitNumbers(payload.date, payload.timeId, themeId);
  }

  return reservation;
}

function cancelDemoReservation(id, authorizationName) {
  const reservation = state.demoReservations.find((item) => item.id === id);
  if (!reservation) {
    throw new Error("존재하지 않는 예약입니다.");
  }
  if (reservation.guestName !== authorizationName) {
    throw new Error("본인의 예약만 취소할 수 있습니다.");
  }
  if (reservationStatus(reservation) === "CANCELED") {
    throw new Error("이미 취소된 예약입니다.");
  }

  const wasConfirmed = reservationStatus(reservation) === "CONFIRMED";
  const date = reservation.date;
  const timeId = getReservationTimeId(reservation);
  const themeId = getReservationThemeId(reservation);

  reservation.status = "CANCELED";
  delete reservation.waitNumber;

  if (wasConfirmed) {
    promoteTopDemoWaiting(date, timeId, themeId);
    return;
  }
  refreshDemoWaitNumbers(date, timeId, themeId);
}

function showToast(title, detail) {
  elements.toast.innerHTML = `<strong>${title}</strong><span>${detail}</span>`;
  elements.toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    elements.toast.classList.remove("show");
  }, 3600);
}


export const app = {
  API_BASE,
  GUEST_NAME_HEADER,
  DEMO_DATE,
  DEFAULT_DATE,
  DEFAULT_THEME_PRICE,
  PAGE,
  SLOT_AVAILABLE,
  SLOT_WAITING,
  SLOT_UNAVAILABLE,
  SLOT_LOADING,
  PAYMENT_DRAFT_STORAGE_PREFIX,
  state,
  demoThemes,
  demoTimes,
  elements,
  todayDate,
  isAdminPage,
  isUserPage,
  isPaymentPage,
  posterFor,
  themeImageSource,
  getJson,
  postJson,
  patchJson,
  deleteJson,
  guestNameHeaders,
  errorMessageFrom,
  endpointMessageOr,
  paymentDraftStorageKey,
  savePaymentDraft,
  loadPaymentDraft,
  clearStoredPaymentDraft,
  redirectToPaymentFail,
  showErrorPopup,
  hideErrorPopup,
  showToast,
  setInlineMessage,
  setFormMessage,
  setPaymentMessage,
  setLookupMessage,
  getReservationListData,
  escapeHtml,
  setSourceStatus,
  selectedTheme,
  selectedTime,
  currentSlotAvailability,
  availabilityLabel,
  availabilityClass,
  resetSlotAvailability,
  selectedAdminTheme,
  selectedAdminTime,
  normalizeTime,
  formatDate,
  formatPrice,
  reservationStatus,
  statusLabel,
  statusBadgeHtml,
  isActiveReservation,
  isConfirmedReservation,
  isOccupyingReservation,
  clearPaymentDraft,
  isSameSlot,
  demoSlotAvailability,
  fetchSlotAvailability,
  nextDemoWaitNumber,
  hasDemoConfirmedReservation,
  hasDuplicateDemoReservation,
  createDemoReservation,
  createDemoPaymentReservation,
  createDemoWaitingReservation,
  createPendingReservation,
  normalizeWaitingReservation,
  getNextId,
  getReservationTheme,
  getReservationTime,
  getReservationThemeId,
  getReservationTimeId,
  replaceReservation,
  demoWaitingReservationsForSlot,
  refreshDemoWaitNumbers,
  promoteTopDemoWaiting,
  editDemoReservation,
  cancelDemoReservation
};
