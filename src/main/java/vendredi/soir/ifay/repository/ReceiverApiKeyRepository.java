package vendredi.soir.ifay.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiverApiKeyRepository extends JpaRepository<ReceiverApiKeyEntity, UUID> {
  Optional<ReceiverApiKeyEntity> findByKeyHashAndRevokedAtIsNull(String keyHash);
}
