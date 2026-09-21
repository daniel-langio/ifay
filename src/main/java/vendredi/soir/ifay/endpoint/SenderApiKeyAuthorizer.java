package vendredi.soir.ifay.endpoint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import vendredi.soir.ifay.endpoint.exception.UnauthorizedException;
import vendredi.soir.ifay.repository.SenderApiKeyRepository;

/**
 * Mirrors {@link ReceiverApiKeyAuthorizer} for the other side of a payment - a sender key is
 * minted per-sender and DB-stored, only its hash is ever compared.
 */
@Component
@AllArgsConstructor
public class SenderApiKeyAuthorizer {
  private final SenderApiKeyRepository senderApiKeyRepository;

  public UUID acceptSender(String apiKey) {
    if (apiKey == null) {
      throw new UnauthorizedException("Invalid sender API key");
    }
    return senderApiKeyRepository
        .findByKeyHashAndRevokedAtIsNull(hash(apiKey))
        .map(k -> k.getSenderId())
        .orElseThrow(() -> new UnauthorizedException("Invalid sender API key"));
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
