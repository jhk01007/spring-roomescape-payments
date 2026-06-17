package roomescape.payment.domain.exception;

import org.springframework.http.HttpStatus;
import roomescape.common.exception.ErrorPolicy;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

public enum PaymentErrorCode implements ErrorPolicy {

    PAYMENT_AMOUNT_MISMATCH_WITH_THEME("요청된 결제 금액이 테마의 가격과 일치하지 않습니다.", UNPROCESSABLE_ENTITY),
    PAYMENT_RESERVATION_NOT_PENDING("결제 대기 상태의 예약만 결제를 준비할 수 있습니다.", CONFLICT);


    private final String code;
    private final String message;
    private final HttpStatus status;

    PaymentErrorCode(String message, HttpStatus status) {
        this.code = name();
        this.message = message;
        this.status = status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
