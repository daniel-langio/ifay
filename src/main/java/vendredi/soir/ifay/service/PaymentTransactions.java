package vendredi.soir.ifay.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.repository.PaymentEntity;
import vendredi.soir.ifay.repository.PaymentMapper;
import vendredi.soir.ifay.repository.PaymentRepository;
import vendredi.soir.ifay.model.Payment;

/**
 * Split out of {@link PaymentService} solely so its {@code @Transactional} methods go through
 * Spring's proxy: calling a {@code @Transactional} method on {@code this} from within the same
 * class bypasses the proxy entirely (Spring AOP is proxy-based, not bytecode-weaved here), which
 * would silently turn {@code REQUIRES_NEW} into a no-op. Cross-bean calls don't have that problem.
 */
@Component
@AllArgsConstructor
class PaymentTransactions {
  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;

  @Transactional
  Optional<Payment> tryApplyClaim(
      Provider type, String pspRef, String sender, String receiver, long amount) {
    return paymentRepository
        .findByTypeAndPspRefForUpdate(type, pspRef)
        .map(
            existing -> {
              existing.setSender(sender);
              existing.setReceiver(receiver);
              existing.setClaimedAmount(amount);
              existing.setSentAt(Instant.now());
              maybeVerify(existing);
              paymentRepository.save(existing);
              return paymentMapper.toDomain(existing);
            });
  }

  @Transactional
  Optional<Payment> tryApplyReport(
      Provider type, String pspRef, long amount, String verifier, String verifierRevision) {
    return paymentRepository
        .findByTypeAndPspRefForUpdate(type, pspRef)
        .map(
            existing -> {
              existing.setConfirmedAmount(amount);
              existing.setVerifier(verifier);
              existing.setVerifierRevision(verifierRevision);
              existing.setReceivedAt(Instant.now());
              maybeVerify(existing);
              paymentRepository.save(existing);
              return paymentMapper.toDomain(existing);
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  Payment insertClaim(String sender, String receiver, long amount, Provider type, String pspRef) {
    PaymentEntity entity =
        PaymentEntity.builder()
            .id(UUID.randomUUID())
            .pspRef(pspRef)
            .type(type)
            .sender(sender)
            .receiver(receiver)
            .claimedAmount(amount)
            .sentAt(Instant.now())
            .build();
    paymentRepository.saveAndFlush(entity); // flush now so a unique violation is catchable here
    return paymentMapper.toDomain(entity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  Payment insertReport(
      Provider type, String pspRef, long amount, String verifier, String verifierRevision) {
    PaymentEntity entity =
        PaymentEntity.builder()
            .id(UUID.randomUUID())
            .pspRef(pspRef)
            .type(type)
            .confirmedAmount(amount)
            .verifier(verifier)
            .verifierRevision(verifierRevision)
            .receivedAt(Instant.now())
            .build();
    paymentRepository.saveAndFlush(entity);
    return paymentMapper.toDomain(entity);
  }

  private void maybeVerify(PaymentEntity e) {
    if (e.getSentAt() != null && e.getReceivedAt() != null && e.getVerifiedAt() == null) {
      e.setVerifiedAt(Instant.now());
    }
  }
}
