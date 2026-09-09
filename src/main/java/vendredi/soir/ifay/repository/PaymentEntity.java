package vendredi.soir.ifay.repository;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import vendredi.soir.ifay.model.Provider;

@Entity
@Table(
    name = "payment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"type", "pspRef"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEntity {
  @Id private UUID id;

  private String pspRef;

  @Enumerated(EnumType.STRING)
  private Provider type;

  private String sender;
  private String receiver;
  private Long claimedAmount;
  private Long confirmedAmount;
  private String verifier;
  private String verifierRevision;
  private Instant sentAt;
  private Instant receivedAt;
  private Instant verifiedAt;
}
