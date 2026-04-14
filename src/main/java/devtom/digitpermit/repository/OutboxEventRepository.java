package devtom.digitpermit.repository;

import devtom.digitpermit.Model.OutboxEvent;
import devtom.digitpermit.enums.OutboxStatus;
import devtom.digitpermit.event.OutboxEventPayload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatus(OutboxStatus status);
    List<OutboxEvent> findByStatusAndRetryCountLessThan(OutboxStatus status, int maxRetries);
    
}
