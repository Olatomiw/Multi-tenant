package devtom.digitpermit.config;

import com.rabbitmq.client.AMQP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMqConfig {

    public static final String QUEUE_NAME = "digitpermit";

    private final String EXCHANGE_NAME;

    public final String ROUTING_KEY;

    public RabbitMqConfig(@Value("${digitpermit.exchange_key}") String exchangeName,
                          @Value("${digitpermit.routing_key}") String routingKey){
        this.EXCHANGE_NAME = exchangeName;
        this.ROUTING_KEY = routingKey;
    }


    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Exchange exchange(){
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Binding binding(Queue queue, Exchange exchange){
        log.info("Binding queue {} to exchange {}", queue.getName(), exchange.getName());
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ROUTING_KEY)
                .noargs();

    }


}
