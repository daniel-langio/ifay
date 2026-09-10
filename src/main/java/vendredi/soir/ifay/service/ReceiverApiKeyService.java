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
import vendredi.soir.ifay.repository.ReceiverApiKeyEntity;
import vendredi.soir.ifay.repository.ReceiverApiKeyRepository;

/**
 * Mints static, per-receiver API keys. Bootstrapping trust for this is deliberately weak: the
 * caller (see {@code ReceiverController}) only has to hold the shared verifier API key, not prove
 * it owns the phone number being registered - there's no OTP/SMS-sending capability in ifay to
 * do better. This is a known, accepted limitation: any legitimate porofo install can mint a
 * receiver-scoped key for *any* phone number. What the resulting key still buys is real scoping
 * once minted - it can only ever list or verify payments for that one receiver.
 */
@Component
@AllArgsConstructor
public class ReceiverApiKeyService {
  private static final SecureRandom RANDOM = new SecureRandom();

  private final PartyService partyService;
  private final ReceiverApiKeyRepository receiverApiKeyRepository;

  public String mint(String phoneNumber) {
    UUID receiverId = partyService.findOrCreate(phoneNumber);
    String token = generateToken();
    receiverApiKeyRepository.save(
        ReceiverApiKeyEntity.builder()
            .id(UUID.randomUUID())
            .receiverId(receiverId)
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
