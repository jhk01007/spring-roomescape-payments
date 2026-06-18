import { app } from "./roomescape/shared.js";
import { initAdminPage } from "./roomescape/admin-page.js";
import { initPaymentPage } from "./roomescape/payment-page.js";
import { initUserPage } from "./roomescape/user-page.js";

const {
  elements,
  hideErrorPopup,
  isAdminPage,
  isPaymentPage,
  isUserPage
} = app;

function bindCommonEvents() {
  if (!elements.errorPopup || !elements.errorPopupClose) {
    return;
  }

  elements.errorPopupClose.addEventListener("click", hideErrorPopup);
  elements.errorPopup.addEventListener("click", (event) => {
    if (event.target === elements.errorPopup) {
      hideErrorPopup();
    }
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      hideErrorPopup();
    }
  });
}

async function bootstrap() {
  bindCommonEvents();

  if (isUserPage()) {
    await initUserPage();
    return;
  }
  if (isPaymentPage()) {
    await initPaymentPage();
    return;
  }
  if (isAdminPage()) {
    await initAdminPage();
  }
}

bootstrap();
