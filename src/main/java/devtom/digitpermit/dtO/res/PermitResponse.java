package devtom.digitpermit.dtO.res;

import devtom.digitpermit.enums.PaymentStatus;
import devtom.digitpermit.enums.PermitStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class    PermitResponse {
    private UUID permitId;
    private String permitNumber;
    private PermitStatus status;
    private String permitType;
    private String description;
    private ApplicantResponse applicant;
    private PaymentStatus paymentStatus;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
}
