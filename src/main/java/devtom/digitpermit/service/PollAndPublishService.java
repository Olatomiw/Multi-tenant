package devtom.digitpermit.service;

import devtom.digitpermit.Model.OutboxEvent;
import devtom.digitpermit.enums.OutboxStatus;
import devtom.digitpermit.event.RabbitMqProducer;
import devtom.digitpermit.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollAndPublishService {

    private final RabbitMqProducer rabbitMQPublisher;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void processAllPendingEvents() {

        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByStatusAndRetryCountLessThan(OutboxStatus.PENDING, 5);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox poller found {} pending event(s)", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            rabbitMQPublisher.send(event);

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);

            log.info("Successfully published event: {} for aggregateId: {}",
                    event.getEventType(), event.getAggregateId());

        } catch (Exception e) {
            int newRetryCount = event.getRetryCount() + 1;
            event.setRetryCount(newRetryCount);

            if (newRetryCount >= 5) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Event permanently failed after {} attempts. aggregateId: {} eventType: {}",
                        newRetryCount, event.getAggregateId(), event.getEventType());
            } else {
                log.warn("Event publish failed. Attempt {}/5. Will retry. aggregateId: {}",
                        newRetryCount, event.getAggregateId());
            }

            outboxEventRepository.save(event);
        }
    }
}
