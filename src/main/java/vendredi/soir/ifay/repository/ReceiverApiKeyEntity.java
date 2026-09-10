package vendredi.soir.ifay.repository;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * A static, per-receiver API key minted via {@code POST /receivers/api-keys}. Only the SHA-256
 * hash is ever persisted - the plaintext token is returned exactly once, at mint time, and
 * cannot be recovered from this row afterward.
 */
@Entity
@Table(name = "receiver_api_key")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiverApiKeyEntity {
  @Id private UUID id;
  private UUID receiverId;
  private String keyHash;
  private Instant createdAt;
  private Instant revokedAt;
}
