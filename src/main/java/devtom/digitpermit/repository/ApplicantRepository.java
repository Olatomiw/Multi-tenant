package devtom.digitpermit.repository;

import devtom.digitpermit.Model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
import java.util.UUID;

public interface ApplicantRepository extends JpaRepository<Applicant, UUID> {
    Optional<Applicant> findByNationalId(String nationalId);
}
