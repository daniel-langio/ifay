package vendredi.soir.ifay.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ChainTipRepository extends JpaRepository<ChainTipEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from ChainTipEntity c where c.id = 1")
  Optional<ChainTipEntity> findTipForUpdate();
}
