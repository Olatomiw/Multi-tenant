package devtom.digitpermit.util;

import devtom.digitpermit.Model.Applicant;
import devtom.digitpermit.Model.Permit;
import devtom.digitpermit.dtO.req.ApplicantRequestDto;
import devtom.digitpermit.dtO.res.ApplicantResponse;
import devtom.digitpermit.dtO.res.PermitResponse;
import devtom.digitpermit.dtO.res.PermitSummaryResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class Mappers {
    public Applicant toEntity(ApplicantRequestDto request) {
        return Applicant.builder()
                .id(UUID.randomUUID())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .nationalId(request.getNationalId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public ApplicantResponse toResponse(Applicant applicant) {
        return ApplicantResponse.builder()
                .id(applicant.getId())
                .firstName(applicant.getFirstName())
                .lastName(applicant.getLastName())
                .email(applicant.getEmail())
                .phoneNumber(applicant.getPhoneNumber())
                .nationalId(applicant.getNationalId())
                .build();
    }

}
