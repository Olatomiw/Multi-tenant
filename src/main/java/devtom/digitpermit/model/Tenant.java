package devtom.digitpermit.model;

import devtom.digitpermit.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
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
    @Column(name = "tenant_id")
    private String tenantId;
    @NotBlank(message = "Schema Name cannot be blank")
    @Column(name = "schema_name")
    private String schemaName;
    @NotBlank(message = "Database Name cannot be blank")
    private String name;
    @Enumerated(EnumType.STRING)
    private Status status;
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
