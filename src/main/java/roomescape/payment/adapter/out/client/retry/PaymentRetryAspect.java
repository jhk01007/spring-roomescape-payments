package roomescape.payment.adapter.out.client.retry;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PaymentRetryAspect {

    @Around("@annotation(roomescape.payment.adapter.out.client.retry.RetryablePayment)")
    public Object retry(ProceedingJoinPoint joinPoint) throws Throwable {
        RetryablePayment retryable = retryableAnnotation(joinPoint);
        int maxAttempts = Math.max(1, retryable.maxAttempts());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (Throwable exception) {
                if (!retryableException(exception, retryable.retryOn())) {
                    throw exception;
                }
                if (attempt == maxAttempts) {
                    throw exception;
                }
                log.warn("Retryable payment error. retry {}/{}", attempt + 1, maxAttempts);
                pause(retryable.delayMillis(), exception);
            }
        }

        throw new IllegalStateException("Retry attempts must be greater than zero.");
    }

    private RetryablePayment retryableAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod().getAnnotation(RetryablePayment.class);
    }

    private boolean retryableException(Throwable exception, Class<? extends Throwable>[] retryOn) {
        for (Class<? extends Throwable> retryableExceptionClass : retryOn) {
            if (retryableExceptionClass.isInstance(exception)) {
                return true;
            }
        }
        return false;
    }

    private void pause(long delayMillis, Throwable exception) throws Throwable {
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
