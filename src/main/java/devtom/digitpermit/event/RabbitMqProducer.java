package devtom.digitpermit.event;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import devtom.digitpermit.Model.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitMqProducer {

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${digitpermit.exchange_key}")
    private String exchangeName;
    @Value("${digitpermit.routing_key}")
    private String routingKey;


    public void send(OutboxEvent event) {
        try {
            OutboxEventPayload payload = objectMapper
                    .readValue(event.getPayload(), OutboxEventPayload.class);

            rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);

            log.info("Published to RabbitMQ — exchange: {} routingKey: {} permitId: {}",
                    exchangeName, routingKey, payload.getPermitId());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize outbox event payload for id: "
                    + event.getId(), e);
        }
    }
}
