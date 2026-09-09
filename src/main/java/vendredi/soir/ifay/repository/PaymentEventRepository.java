package vendredi.soir.ifay.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vendredi.soir.ifay.model.Provider;

public interface PaymentEventRepository extends JpaRepository<PaymentEventEntity, UUID> {
  List<PaymentEventEntity> findByTypeAndPspRefOrderBySequenceAsc(Provider type, String pspRef);
}
