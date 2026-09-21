package vendredi.soir.ifay.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SenderApiKeyRepository extends JpaRepository<SenderApiKeyEntity, UUID> {
  Optional<SenderApiKeyEntity> findByKeyHashAndRevokedAtIsNull(String keyHash);
}
