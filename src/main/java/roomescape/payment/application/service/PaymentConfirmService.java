package roomescape.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import roomescape.common.exception.DomainException;
import roomescape.payment.application.port.in.PaymentConfirmUseCase;
import roomescape.payment.application.port.out.PaymentRepository;
import roomescape.payment.application.port.out.PaymentSessionInfo;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.payment.domain.PaymentConfirmation;
import roomescape.payment.domain.PaymentResult;
import roomescape.payment.domain.exception.PaymentAmountMismatchException;

import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_ALREADY_PROCESSED;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_SESSION_NOT_FOUND;

/**
 * 결제 승인 유스케이스. 게이트웨이 호출 '전에' 금액을 검증하는 것이 핵심이다.
 */
@Service
@RequiredArgsConstructor
public class PaymentConfirmService implements PaymentConfirmUseCase {

    private final PaymentSessionRepository paymentSessionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayRetryExecutor paymentGatewayRetryExecutor;
    private final PaymentCompleteService paymentCompleteService;

    public PaymentResult confirm(String paymentKey, String orderId, Long amount) {
        validateAlreadyPaymentConfirmed(paymentKey);

        PaymentSessionInfo paymentSession = getPaymentSession(orderId);
        validateAmount(amount, paymentSession);
        paymentCompleteService.validateCompletable(paymentSession.reservationId());

        PaymentConfirmation confirmation = new PaymentConfirmation(paymentKey, orderId, amount);
        PaymentResult paymentResult = paymentGatewayRetryExecutor.confirm(confirmation);
        paymentCompleteService.complete(paymentResult);

        return paymentResult;
    }

    private void validateAlreadyPaymentConfirmed(String paymentKey) {
        if (paymentRepository.existsByPaymentKey(paymentKey)) {
            throw new DomainException(PAYMENT_ALREADY_PROCESSED);
        }
    }

    private static void validateAmount(Long amount, PaymentSessionInfo paymentSession) {
        if (!paymentSession.isSameAmount(amount)) {
            throw new PaymentAmountMismatchException(paymentSession.amount(), amount);
        }
    }

    private PaymentSessionInfo getPaymentSession(String orderId) {
        return paymentSessionRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(PAYMENT_SESSION_NOT_FOUND));
    }

}
