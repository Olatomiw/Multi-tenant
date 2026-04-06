package devtom.digitpermit.Model;

import devtom.digitpermit.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "tenants", schema = "public")
public class Tenant {

    @Id
    private UUID id;
    @NotBlank(message = "Tenant ID cannot be blank")
    private String tenantId;
    @NotBlank(message = "Schema Name cannot be blank")
    private String schemaName;
    @NotBlank(message = "Database Name cannot be blank")
    private String databaseName;
    @Enumerated(EnumType.STRING)
    private Status status;
    @CreatedDate
    private LocalDateTime createdAt;

}
