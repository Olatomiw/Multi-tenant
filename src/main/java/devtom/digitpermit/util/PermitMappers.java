package devtom.digitpermit.util;

import devtom.digitpermit.model.Permit;
import devtom.digitpermit.dtO.res.PermitResponse;
import devtom.digitpermit.dtO.res.PermitSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermitMappers {

    private final Mappers applicantMapper;

    public PermitResponse toResponse(Permit permit) {
        return PermitResponse.builder()
                .permitId(permit.getId())
                .permitNumber(permit.getPermitNumber())
                .status(permit.getStatus())
                .permitType(permit.getPermitType().name())
                .description(permit.getDescription())
                .applicant(applicantMapper.toResponse(permit.getApplicant()))
                .createdAt(permit.getCreatedAt())
                .issuedAt(permit.getIssuedAt())
                .expiresAt(permit.getExpiresAt())
                .build();
    }

    public PermitSummaryResponse toSummaryResponse(Permit permit) {
        return PermitSummaryResponse.builder()
                .permitId(permit.getId())
                .permitNumber(permit.getPermitNumber())
                .status(permit.getStatus())
                .permitType(permit.getPermitType().name())
                .applicantFullName(permit.getApplicant().getFirstName()
                        + " " + permit.getApplicant().getLastName())
                .applicantNationalId(permit.getApplicant().getNationalId())
                .createdAt(permit.getCreatedAt())
                .build();
    }
}
