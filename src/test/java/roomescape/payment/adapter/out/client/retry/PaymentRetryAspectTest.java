package roomescape.payment.adapter.out.client.retry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRetryAspectTest {

    private final PaymentRetryAspect paymentRetryAspect = new PaymentRetryAspect();

    @Test
    @DisplayName("지정된 예외가 발생하면 최대 횟수 안에서 재시도한다.")
    void retry_whenRetryableExceptionThrown() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPointFor("retryable");
        AtomicInteger attempts = new AtomicInteger();
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RetryTargetException();
            }
            return "success";
        });

        Object result = paymentRetryAspect.retry(joinPoint);

        assertThat(result).isEqualTo("success");
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    @DisplayName("지정되지 않은 예외가 발생하면 재시도하지 않는다.")
    void notRetry_whenNotRetryableExceptionThrown() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPointFor("retryable");
        when(joinPoint.proceed()).thenThrow(new NonRetryTargetException());

        assertThatThrownBy(() -> paymentRetryAspect.retry(joinPoint))
                .isInstanceOf(NonRetryTargetException.class);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("지정된 예외가 최대 횟수까지 발생하면 마지막 예외를 던진다.")
    void throwLastException_whenRetryAttemptsExhausted() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPointFor("retryable");
        when(joinPoint.proceed()).thenThrow(new RetryTargetException());

        assertThatThrownBy(() -> paymentRetryAspect.retry(joinPoint))
                .isInstanceOf(RetryTargetException.class);
        verify(joinPoint, times(3)).proceed();
    }

    private ProceedingJoinPoint joinPointFor(String methodName) throws NoSuchMethodException {
        Method method = PaymentRetryFixture.class.getDeclaredMethod(methodName);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        return joinPoint;
    }

    private static class PaymentRetryFixture {

        @RetryablePayment(retryOn = RetryTargetException.class, delayMillis = 0)
        void retryable() {
        }

    }

    private static class RetryTargetException extends RuntimeException {
    }

    private static class NonRetryTargetException extends RuntimeException {
    }

}
