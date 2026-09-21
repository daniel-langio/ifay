package vendredi.soir.ifay.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import vendredi.soir.ifay.repository.SenderApiKeyEntity;
import vendredi.soir.ifay.repository.SenderApiKeyRepository;

/**
 * Mints static, per-sender API keys. Mirrors {@link ReceiverApiKeyService} - same bootstrapping
 * trust model (gated by the shared verifier key, not proof of phone ownership - see {@code
 * SenderController}), same known/accepted limitation.
 */
@Component
@AllArgsConstructor
public class SenderApiKeyService {
  private static final SecureRandom RANDOM = new SecureRandom();

  private final PartyService partyService;
  private final SenderApiKeyRepository senderApiKeyRepository;

  public String mint(String phoneNumber) {
    UUID senderId = partyService.findOrCreate(phoneNumber);
    String token = generateToken();
    senderApiKeyRepository.save(
        SenderApiKeyEntity.builder()
            .id(UUID.randomUUID())
            .senderId(senderId)
            .keyHash(hash(token))
            .createdAt(Instant.now())
            .build());
    return token;
  }

  private static String generateToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String hash(String token) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
