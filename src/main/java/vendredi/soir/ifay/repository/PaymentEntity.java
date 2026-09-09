package vendredi.soir.ifay.repository;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "payment")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEntity {
  @Id private UUID id;
  private String payerReference;
  private String payerMsisdn;
  private long amountRequested;
  private Long confirmedAmount;

  @Builder.Default private boolean pspReportedFailure = false;

  private String serverCorrelationId;
  private String scope;
  private Instant creationInstant;
  private Instant lastVerificationInstant;

  @Builder.Default private int verificationAttemptNb = 0;
}
