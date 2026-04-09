package devtom.digitpermit.dtO.res;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationResponse {

    private String id;
    private String status;
    private String message;

    @PostConstruct
    void generateId(){
        id = UUID.randomUUID().toString();
    }
}
