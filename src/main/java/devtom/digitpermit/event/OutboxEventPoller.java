package devtom.digitpermit.event;

import devtom.digitpermit.Model.OutboxEvent;
import devtom.digitpermit.Model.Tenant;
import devtom.digitpermit.Model.TenantContext;
import devtom.digitpermit.enums.OutboxStatus;
import devtom.digitpermit.enums.Status;
import devtom.digitpermit.repository.OutboxEventRepository;
import devtom.digitpermit.repository.TenantRepository;
import devtom.digitpermit.service.PollAndPublishService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPoller {

    private final PollAndPublishService pollAndPublishService;
    private final TenantRepository tenantRepository;



    @Scheduled(fixedDelayString = "${outbox.poller.interval:5000}")
    public void poller() {
        List<Tenant> activeTenants = tenantRepository.findByStatus(Status.ACTIVE);

        for (Tenant tenant : activeTenants) {
            try {
                TenantContext.setCurrentTenant(tenant.getSchemaName());
                pollAndPublishService.processAllPendingEvents();

            } catch (Exception e) {
                log.error("Poller failed for tenant: {} Reason: {}",
                        tenant.getTenantId(), e.getMessage());
            } finally {
                TenantContext.clearCurrentTenant();
            }
        }
    }
}
