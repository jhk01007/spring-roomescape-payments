package roomescape.payment.domain.exception;

import org.springframework.http.HttpStatus;
import roomescape.common.exception.ErrorPolicy;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

public enum PaymentErrorCode implements ErrorPolicy {

    PAYMENT_AMOUNT_MISMATCH_WITH_THEME("요청된 결제 금액이 테마의 가격과 일치하지 않습니다.", UNPROCESSABLE_ENTITY),
    PAYMENT_RESERVATION_NOT_PENDING("결제 대기 상태의 예약만 결제를 준비할 수 있습니다.", CONFLICT),
    PAYMENT_EXPIRED("만료된 결제입니다. 다시 예약해주세요.", CONFLICT),
    PAYMENT_SESSION_NOT_FOUND("결제 세션을 찾을 수 없습니다.", NOT_FOUND),
    PAYMENT_CONFIRMATION_NOT_DONE("승인 완료 상태의 결제만 완료 처리할 수 있습니다.", CONFLICT),
    PAYMENT_FAILURE_NOT_ALLOWED("결제 대기 상태의 예약만 결제 실패 처리할 수 있습니다.", CONFLICT);


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
