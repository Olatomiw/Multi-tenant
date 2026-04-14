package devtom.digitpermit.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitMqConsumer {

    public static final String QUEUE_NAME = "digitpermit";

    @RabbitListener(queues = QUEUE_NAME)
    public void receiveMessage(OutboxEventPayload event, Message message)
    {
        try {
            log.info("╔══════════════════════════════════════════════════");
            log.info("║ PermitCreated Event Received");
            log.info("║ Permit ID     : {}", event.getPermitId());
            log.info("║ Permit Number : {}", event.getPermitNumber());
            log.info("║ Permit Type   : {}", event.getPermitType());
            log.info("║ Applicant     : {}", event.getApplicantName());
            log.info("║ National ID   : {}", event.getApplicantNationalId());
            log.info("║ Ministry      : {}", event.getMinistry());
            log.info("║ Status        : {}", event.getStatus());
            log.info("║ Timestamp     : {}", event.getTimestamp());
            log.info("╚══════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("Failed to process PermitCreated event. MessageId: {} Reason: {}",
                    message.getMessageProperties().getMessageId(), e.getMessage());
            throw new RuntimeException("Event processing failed", e);
        }
    }
}
