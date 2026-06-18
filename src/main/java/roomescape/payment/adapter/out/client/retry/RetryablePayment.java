package roomescape.payment.adapter.out.client.retry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryablePayment {

    Class<? extends Throwable>[] retryOn();

    int maxAttempts() default 3;

    long delayMillis() default 200L;

}
