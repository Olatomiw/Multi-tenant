package devtom.digitpermit.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class OutboxEventPayload {

    private UUID permitId;
    private String permitNumber;
    private String permitType;
    private String applicantName;
    private String applicantNationalId;
    private String ministry;
    private String status;
    private LocalDateTime timestamp;
}
