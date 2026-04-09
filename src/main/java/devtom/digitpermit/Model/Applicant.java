package devtom.digitpermit.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applicants")
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Applicant {

    @Id
    private UUID id;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Email
    @Column(name = "email", unique = true)
    private String email;
    @Column(name = "phone_number",  unique = true)
    private String phoneNumber;
    @Column(name = "national_id",   unique = true)
    private String nationalId;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
