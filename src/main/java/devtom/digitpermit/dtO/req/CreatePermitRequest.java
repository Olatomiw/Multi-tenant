package devtom.digitpermit.dtO.req;

import devtom.digitpermit.enums.PermitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link devtom.digitpermit.model.Permit}
 */
@Value
public class CreatePermitRequest implements Serializable {
    @NotNull(message = "Applicant details are required")
    @Valid
    private ApplicantRequestDto applicant;

    @NotNull(message = "Permit type is required")
    private PermitType permitType;

    @NotBlank(message = "Description is required")
    private String description;
}