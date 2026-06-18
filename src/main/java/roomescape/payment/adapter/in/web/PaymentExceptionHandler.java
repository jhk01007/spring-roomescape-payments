package roomescape.payment.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import roomescape.common.exception.ErrorResponse;
import roomescape.payment.adapter.out.client.TossPaymentException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler {

    @ExceptionHandler(TossPaymentException.class)
    public ResponseEntity<ErrorResponse> handleTossPaymentException(
            TossPaymentException exception,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = ErrorResponse.of(
                request.getRequestURI(),
                exception.getCode(),
                exception.getMessage()
        );
        return ResponseEntity
                .status(exception.getStatus())
                .body(errorResponse);
    }
}
