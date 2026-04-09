package devtom.digitpermit.dtO.req;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentVerificationRequest {

    private String nationalId;
    private BigDecimal amount;
    private String permitType;
}
