package devtom.digitpermit.dtO.res;

import devtom.digitpermit.enums.PermitStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermitSummaryResponse {
    private UUID permitId;
    private String permitNumber;
    private PermitStatus status;
    private String permitType;
    private String applicantFullName;
    private String applicantNationalId;
    private LocalDateTime createdAt;
}
