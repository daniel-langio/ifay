package vendredi.soir.ifay.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.ifay.endpoint.exception.NotFoundException;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.repository.PaymentEntity;
import vendredi.soir.ifay.repository.PaymentMapper;
import vendredi.soir.ifay.repository.PaymentRepository;

/**
 * Records claims (from a client app, on behalf of a payer) and reports (from a verifier, e.g.
 * porofo's SMS parser) against the same {@code (type, pspRef)} key - whichever side arrives
 * first creates the row, the other side updates it and triggers verification. A plain unique
 * constraint on (type, pspRef) is the actual race-safety net (a pessimistic lock only serializes
 * access to a row that already exists, it can't stop two concurrent inserts for a brand new key)
 * - both record* methods try the update-if-exists path first, and if that finds nothing, insert
 * and fall back to update-if-exists again if that insert loses a race and hits the constraint.
 */
@Service
@AllArgsConstructor
public class PaymentService {
  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;

  public Payment recordClaim(String sender, String receiver, long amount, Provider type, String rawPspRef) {
    String pspRef = normalizeRef(rawPspRef);
    Optional<Payment> updated = tryApplyClaim(type, pspRef, sender, receiver, amount);
    if (updated.isPresent()) {
      return updated.get();
    }
    try {
      return insertClaim(sender, receiver, amount, type, pspRef);
    } catch (DataIntegrityViolationException e) {
      // Lost the race - a report (or, in theory, a racing duplicate claim) landed first between
      // our lookup and our insert.
      return tryApplyClaim(type, pspRef, sender, receiver, amount)
          .orElseThrow(
              () -> new IllegalStateException("Unique violation but no row for " + type + "/" + pspRef));
    }
  }

  public Payment recordReport(
      Provider type, String rawPspRef, long amount, String verifier, String verifierRevision) {
    String pspRef = normalizeRef(rawPspRef);
    Optional<Payment> updated = tryApplyReport(type, pspRef, amount, verifier, verifierRevision);
    if (updated.isPresent()) {
      return updated.get();
    }
    try {
      return insertReport(type, pspRef, amount, verifier, verifierRevision);
    } catch (DataIntegrityViolationException e) {
      return tryApplyReport(type, pspRef, amount, verifier, verifierRevision)
          .orElseThrow(
              () -> new IllegalStateException("Unique violation but no row for " + type + "/" + pspRef));
    }
  }

  /**
   * Case matters for matching (a player types a Ref/Trans Id in whatever case their own
   * confirmation shows it, while porofo's SMS parser normalizes text to lowercase before
   * extracting one) - normalize both sides identically so a case difference alone never causes
   * an otherwise-correct match to miss.
   */
  private static String normalizeRef(String rawPspRef) {
    return rawPspRef.trim().toUpperCase();
  }

  @Transactional(readOnly = true)
  public Payment getPayment(String id) {
    return paymentMapper.toDomain(
        paymentRepository
            .findById(UUID.fromString(id))
            .orElseThrow(() -> new NotFoundException("No payment with id " + id)));
  }

  @Transactional
  public Optional<Payment> tryApplyClaim(
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
  public Optional<Payment> tryApplyReport(
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
  public Payment insertClaim(String sender, String receiver, long amount, Provider type, String pspRef) {
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
  public Payment insertReport(
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
