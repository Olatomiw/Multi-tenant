package devtom.digitpermit.repository;

import devtom.digitpermit.Model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID>{

}
