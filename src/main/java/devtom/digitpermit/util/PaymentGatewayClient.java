package devtom.digitpermit.util;

import devtom.digitpermit.dtO.req.PaymentVerificationRequest;
import devtom.digitpermit.dtO.res.PaymentVerificationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentGatewayClient {

    private final RestTemplate restTemplate;

    @Value("${payment.gateway.url}")
    private String paymentGatewayUrl;

    @CircuitBreaker(name = "permit-service", fallbackMethod = "paymentFallback")
    @Retry(name = "permit-service")
    @TimeLimiter(name = "permit-service")
    public CompletableFuture<PaymentVerificationResponse> verifyPayment(
            PaymentVerificationRequest request) {
        String uri = paymentGatewayUrl+"api/payments/verify";
        return CompletableFuture.supplyAsync(() -> {
            log.info("Calling payment gateway for nationalId: {}", request.getNationalId());

            ResponseEntity<PaymentVerificationResponse> response = restTemplate.postForEntity(
                    uri,
                    request,
                    PaymentVerificationResponse.class
            );
            log.info(paymentGatewayUrl + "api/payments/verify", response.getBody());
            log.info("Payment gateway responded with status: {}", response.getStatusCode());
            return response.getBody();
        });
    }

    public CompletableFuture<PaymentVerificationResponse> paymentFallback(
            PaymentVerificationRequest request, Throwable throwable) {

        log.warn("Payment gateway unavailable. Falling back. Reason: {}", throwable.getMessage());

        return CompletableFuture.completedFuture(
                PaymentVerificationResponse.builder()
                        .id("FALLBACK-" + UUID.randomUUID())
                        .status("PAYMENT_PENDING")
                        .message("Payment gateway temporarily unavailable. Will retry automatically.")
                        .build()
        );
    }
}
