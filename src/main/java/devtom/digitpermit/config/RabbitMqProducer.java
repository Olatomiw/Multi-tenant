package devtom.digitpermit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitMqProducer {



    private final RabbitTemplate rabbitTemplate;

    private final String exchangeName;

    private final String routingKey;

    public RabbitMqProducer(RabbitTemplate rabbitTemplate,
                            @Value("${digitpermit.exchange_key}") String exchangeName,
                            @Value("${digitpermit.routing_key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public void send(String message) {
        rabbitTemplate.convertAndSend(exchangeName,
                routingKey,
                message);
        log.info("Message sent to RabbitMQ: {}", message);
    }
}
