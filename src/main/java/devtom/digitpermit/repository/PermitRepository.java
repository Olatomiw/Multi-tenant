package devtom.digitpermit.repository;

import devtom.digitpermit.Model.Permit;
import devtom.digitpermit.enums.PermitStatus;
import devtom.digitpermit.enums.PermitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.rmi.server.RemoteRef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermitRepository extends JpaRepository<Permit, UUID> {

    boolean existsPermitByPermitNumber(String permitNumber);
    @Query("SELECT p FROM Permit p JOIN FETCH p.applicant")
    List<Permit> findAllWithApplicant();

    Optional<Permit> findByApplicant_NationalIdAndPermitTypeAndStatus(String applicantNationalId,
                                                                     PermitType permitType,
                                                                     PermitStatus status);
}
