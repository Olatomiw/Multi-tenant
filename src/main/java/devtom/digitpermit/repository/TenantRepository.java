package devtom.digitpermit.repository;

import devtom.digitpermit.Model.Tenant;
import devtom.digitpermit.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID>{

    Optional<Tenant> findByTenantId(String tenantId);

    List<Tenant> findByStatus(Status status);

}
