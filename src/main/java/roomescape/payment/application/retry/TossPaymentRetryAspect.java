package roomescape.payment.application.retry;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import roomescape.payment.adapter.out.client.TossPaymentException;

@Aspect
@Component
@Slf4j
public class TossPaymentRetryAspect {

    @Around("@annotation(roomescape.payment.application.retry.RetryableTossPayment)")
    public Object retry(ProceedingJoinPoint joinPoint) throws Throwable {
        RetryableTossPayment retryable = retryableAnnotation(joinPoint);
        int maxAttempts = Math.max(1, retryable.maxAttempts());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (TossPaymentException.Retryable exception) {
                if (attempt == maxAttempts) {
                    throw exception;
                }
                log.warn("Retryable Toss payment error. retry {}/{}", attempt + 1, maxAttempts);
                pause(retryable.delayMillis(), exception);
            }
        }

        throw new IllegalStateException("Retry attempts must be greater than zero.");
    }

    private RetryableTossPayment retryableAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod().getAnnotation(RetryableTossPayment.class);
    }

    private void pause(long delayMillis, TossPaymentException.Retryable exception) {
        if (delayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }
}
