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

  // columnDefinition is required on every enum-mapped column below: without it, Hibernate infers
  // the column type from the enum's *current* constant set and generates a CHECK constraint
  // listing them - `ddl-auto=update` then never widens that constraint when a new constant is
  // added later (it only adds missing columns/tables), so the column silently rejects any new
  // enum value at the database level even though the Java code compiles and expects it to work.
  // This bit real production traffic once (see git history) - explicit varchar keeps enum-value
  // validity a purely Java-level concern, matching how `verificationType` (a plain String) never
  // had this problem to begin with.
  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "varchar(32)")
  private Provider type;

  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "varchar(64)")
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
