package vendredi.soir.ifay.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerifierRepository extends JpaRepository<VerifierEntity, UUID> {

  @Query(
      "select v from VerifierEntity v where v.appId = :appId and v.version = :version "
          + "and (v.revision = :revision or (v.revision is null and :revision is null))")
  Optional<VerifierEntity> findByAppIdAndVersionAndRevision(
      @Param("appId") String appId,
      @Param("version") String version,
      @Param("revision") String revision);
}
