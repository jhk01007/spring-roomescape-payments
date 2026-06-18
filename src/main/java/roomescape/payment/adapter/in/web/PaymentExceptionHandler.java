package roomescape.payment.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import roomescape.common.exception.ErrorResponse;
import roomescape.payment.adapter.out.client.TossPaymentException;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

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
