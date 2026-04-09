package devtom.digitpermit.Model;

import devtom.digitpermit.enums.PermitStatus;
import devtom.digitpermit.enums.PermitType;
import devtom.digitpermit.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "permits")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Permit {

    @Id
    private UUID id;
    @Column(name = "permit_number",  nullable = false)
    private String permitNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "permit_type",  nullable = false)
    private PermitType permitType;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PermitStatus status;
    @Column(name = "description")
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    private Applicant applicant;
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
