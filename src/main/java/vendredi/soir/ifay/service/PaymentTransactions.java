package vendredi.soir.ifay.service;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.ifay.model.Payment;
import vendredi.soir.ifay.model.PaymentEventType;
import vendredi.soir.ifay.model.Provider;
import vendredi.soir.ifay.repository.ChainTipEntity;
import vendredi.soir.ifay.repository.ChainTipRepository;
import vendredi.soir.ifay.repository.PaymentEntity;
import vendredi.soir.ifay.repository.PaymentEventEntity;
import vendredi.soir.ifay.repository.PaymentEventRepository;
import vendredi.soir.ifay.repository.PaymentMapper;
import vendredi.soir.ifay.repository.PaymentRepository;

/**
 * Split out of {@link PaymentService} solely so its {@code @Transactional} methods go through
 * Spring's proxy: calling a {@code @Transactional} method on {@code this} from within the same
 * class would bypass Spring's proxy and silently no-op the transaction boundaries this design
 * depends on.
 *
 * Every write locks the global {@link ChainTipEntity} row first, which serializes it against
 * every other ledger write - Party/Verifier lookup-or-create, the Payment row upsert, and the
 * event append all happen inside that one critical section, so none of them need their own
 * locking or insert-then-catch-conflict dance.
 */
@Component
@AllArgsConstructor
class PaymentTransactions {
  private final ChainTipRepository chainTipRepository;
  private final PaymentEventRepository paymentEventRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;
  private final PartyService partyService;
  private final VerifierService verifierService;

  @Transactional
  Payment recordClaim(
      Provider type, String pspRef, String senderPhone, String receiverPhone, long amount) {
    ChainTipEntity tip = lockTip();
    UUID senderId = partyService.findOrCreate(senderPhone);
    UUID receiverId = partyService.findOrCreate(receiverPhone);
    Instant now = Instant.now();

    PaymentEventEntity event =
        PaymentEventEntity.builder()
            .id(UUID.randomUUID())
            .sequence(tip.getSequence() + 1)
            .pspRef(pspRef)
            .type(type)
            .eventType(PaymentEventType.CLAIM_RECORDED)
            .senderId(senderId)
            .receiverId(receiverId)
            .claimedAmount(amount)
            .occurredAt(now)
            .previousEventHash(tip.getTipHash())
            .build();
    appendEvent(tip, event);

    PaymentEntity payment = findOrCreatePaymentRow(type, pspRef);
    payment.setSenderId(senderId);
    payment.setReceiverId(receiverId);
    payment.setClaimedAmount(amount);
    payment.setSentAt(now);
    maybeVerify(payment);
    paymentRepository.save(payment);
    return paymentMapper.toDomain(payment);
  }

  @Transactional
  Payment recordReport(
      Provider type,
      String pspRef,
      long amount,
      String verifierAppId,
      String verifierVersion,
      String verifierRevision) {
    ChainTipEntity tip = lockTip();
    UUID verifierId = verifierService.findOrCreate(verifierAppId, verifierVersion, verifierRevision);
    Instant now = Instant.now();

    PaymentEventEntity event =
        PaymentEventEntity.builder()
            .id(UUID.randomUUID())
            .sequence(tip.getSequence() + 1)
            .pspRef(pspRef)
            .type(type)
            .eventType(PaymentEventType.REPORT_RECORDED)
            .verifierId(verifierId)
            .confirmedAmount(amount)
            .occurredAt(now)
            .previousEventHash(tip.getTipHash())
            .build();
    appendEvent(tip, event);

    PaymentEntity payment = findOrCreatePaymentRow(type, pspRef);
    payment.setVerifierId(verifierId);
    payment.setConfirmedAmount(amount);
    payment.setReceivedAt(now);
    maybeVerify(payment);
    paymentRepository.save(payment);
    return paymentMapper.toDomain(payment);
  }

  private ChainTipEntity lockTip() {
    return chainTipRepository
        .findTipForUpdate()
        .orElseThrow(() -> new IllegalStateException("Chain tip row missing - startup init failed"));
  }

  private void appendEvent(ChainTipEntity tip, PaymentEventEntity event) {
    event.setEventHash(HashChain.hash(event, tip.getTipHash()));
    paymentEventRepository.save(event);
    tip.setSequence(event.getSequence());
    tip.setTipHash(event.getEventHash());
    chainTipRepository.save(tip);
  }

  private PaymentEntity findOrCreatePaymentRow(Provider type, String pspRef) {
    return paymentRepository
        .findByTypeAndPspRef(type, pspRef)
        .orElseGet(() -> PaymentEntity.builder().id(UUID.randomUUID()).type(type).pspRef(pspRef).build());
  }

  private void maybeVerify(PaymentEntity e) {
    if (e.getSentAt() != null && e.getReceivedAt() != null && e.getVerifiedAt() == null) {
      e.setVerifiedAt(Instant.now());
    }
  }
}
