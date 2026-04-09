package devtom.digitpermit.repository;

import devtom.digitpermit.Model.Permit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PermitRepository extends JpaRepository<Permit, UUID> {

    boolean existsPermitByPermitNumber(String permitNumber);
    @Query("SELECT p FROM Permit p JOIN FETCH p.applicant")
    List<Permit> findAllWithApplicant();
}
