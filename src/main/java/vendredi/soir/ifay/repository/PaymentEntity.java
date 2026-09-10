package vendredi.soir.ifay.repository;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import vendredi.soir.ifay.model.Provider;

@Entity
@Table(
    name = "payment",
    // Physical column names, not entity field names: Spring's default naming strategy
    // snake_cases every camelCase field (pspRef -> psp_ref) when generating DDL, but a
    // uniqueConstraints columnNames string is used as a literal SQL identifier, bypassing that
    // translation - using the entity field name here silently breaks the ALTER TABLE at startup.
    uniqueConstraints = @UniqueConstraint(columnNames = {"type", "psp_ref"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEntity {
  @Id private UUID id;

  private String pspRef;

  @Enumerated(EnumType.STRING)
  private Provider type;

  private UUID senderId;
  private UUID receiverId;
  private Long claimedAmount;
  private Long confirmedAmount;
  private UUID verifierId;
  private Instant sentAt;
  private Instant receivedAt;
  private Instant verifiedAt;
  private String verificationType;
}
