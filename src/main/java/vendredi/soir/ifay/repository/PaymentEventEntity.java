package vendredi.soir.ifay.repository;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import vendredi.soir.ifay.model.PaymentEventType;
import vendredi.soir.ifay.model.Provider;

/**
 * One immutable link in the ledger's hash chain - a {@code ClaimRecorded} or
 * {@code ReportRecorded} event. This, not {@link PaymentEntity}, is the actual source of truth
 * for tamper-evidence: "this payment wasn't altered" means "no event was ever inserted after the
 * fact, edited, or deleted", which an append-only, hash-chained log can prove but a mutable row
 * can't. {@link PaymentEntity} is just a materialized projection of this log, kept in sync in the
 * same transaction as each append, so normal reads stay fast and simple.
 */
@Entity
@Table(name = "payment_event")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEventEntity {
  @Id private UUID id;

  /** Strictly increasing, assigned under the {@link ChainTipEntity} lock - see PaymentTransactions. */
  private long sequence;

  private String pspRef;

  @Enumerated(EnumType.STRING)
  private Provider type;

  @Enumerated(EnumType.STRING)
  private PaymentEventType eventType;

  private UUID senderId;
  private UUID receiverId;
  private Long claimedAmount;

  private UUID verifierId;
  private Long confirmedAmount;

  private Instant occurredAt;

  private String previousEventHash;
  private String eventHash;
}
