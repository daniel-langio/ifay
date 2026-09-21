package vendredi.soir.ifay.repository;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * A static, per-sender API key minted via {@code POST /senders/api-keys}. Mirrors {@link
 * ReceiverApiKeyEntity} exactly, scoped to the other side of a payment - only the SHA-256 hash is
 * ever persisted, the plaintext token is returned exactly once, at mint time.
 */
@Entity
@Table(name = "sender_api_key")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SenderApiKeyEntity {
  @Id private UUID id;
  private UUID senderId;
  private String keyHash;
  private Instant createdAt;
  private Instant revokedAt;
}
