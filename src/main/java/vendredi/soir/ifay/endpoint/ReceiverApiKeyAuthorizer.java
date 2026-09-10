package vendredi.soir.ifay.endpoint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import vendredi.soir.ifay.endpoint.exception.UnauthorizedException;
import vendredi.soir.ifay.repository.ReceiverApiKeyRepository;

/**
 * Unlike {@link ApiKeyAuthorizer}'s two static, config-held secrets, a receiver key is minted
 * per-receiver and DB-stored (see {@code ReceiverApiKeyService}) - only its hash is ever
 * compared, never the plaintext.
 */
@Component
@AllArgsConstructor
public class ReceiverApiKeyAuthorizer {
  private final ReceiverApiKeyRepository receiverApiKeyRepository;

  public UUID acceptReceiver(String apiKey) {
    if (apiKey == null) {
      throw new UnauthorizedException("Invalid receiver API key");
    }
    return receiverApiKeyRepository
        .findByKeyHashAndRevokedAtIsNull(hash(apiKey))
        .map(k -> k.getReceiverId())
        .orElseThrow(() -> new UnauthorizedException("Invalid receiver API key"));
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
