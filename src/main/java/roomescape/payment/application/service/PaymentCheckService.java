package roomescape.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.payment.application.port.in.PaymentCheckUseCase;
import roomescape.payment.application.port.in.dto.PaymentCheckResult;
import roomescape.payment.application.port.out.PaymentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCheckService implements PaymentCheckUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    public List<PaymentCheckResult> findAllByGuestName(String guestName) {
        return paymentRepository.findAllByGuestName(guestName).stream()
                .map(PaymentCheckResult::from)
                .toList();
    }
}
