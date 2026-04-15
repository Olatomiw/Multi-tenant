package devtom.digitpermit.model;

import devtom.digitpermit.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payment_records")
public class PaymentRecord {

    @Id
    private UUID id;
    @OneToOne(cascade = CascadeType.ALL)
    private Permit permit;
    @Column(name = "payment_reference")
    private String paymentReference;
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    @Column(name = "gateway_response")
    private String gatewayResponse;
    @Column(name = "attempt_count")
    private int attemptCount;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
