package devtom.digitpermit.config;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqConsumer {

    public static final String QUEUE_NAME = "digitpermit";

    @RabbitListener(queues = QUEUE_NAME)
    public void receiveMessage(String message)
    {
        // Handle the received message here
        System.out.println("Received message: " + message);
    }
}
