package roomescape.payment.domain.exception;

import roomescape.common.exception.DomainException;

import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH_WITH_THEME;

/**
 * 주문 저장 금액과 요청 금액이 다를 때, confirm 호출 '전에' 차단하는 예외.
 */
public class PaymentAmountMismatchException extends DomainException {

  public PaymentAmountMismatchException(Long expected, Long actual) {
    super(PAYMENT_AMOUNT_MISMATCH_WITH_THEME);
  }

}
