package devtom.digitpermit.service.impl;

import devtom.digitpermit.Model.Applicant;
import devtom.digitpermit.Model.PaymentRecord;
import devtom.digitpermit.Model.Permit;
import devtom.digitpermit.dtO.req.ApplicantRequestDto;
import devtom.digitpermit.dtO.req.CreatePermitRequest;
import devtom.digitpermit.dtO.req.PaymentVerificationRequest;
import devtom.digitpermit.dtO.res.PaymentVerificationResponse;
import devtom.digitpermit.dtO.res.PermitResponse;
import devtom.digitpermit.dtO.res.PermitSummaryResponse;
import devtom.digitpermit.enums.PaymentStatus;
import devtom.digitpermit.enums.PermitStatus;
import devtom.digitpermit.repository.ApplicantRepository;
import devtom.digitpermit.repository.PaymentRecordRepository;
import devtom.digitpermit.repository.PermitRepository;
import devtom.digitpermit.service.PermitService;
import devtom.digitpermit.util.Mappers;
import devtom.digitpermit.util.PaymentGatewayClient;
import devtom.digitpermit.util.PermitMappers;
import devtom.digitpermit.util.PermitNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PermitServiceImpl implements PermitService {

    private final PermitRepository permitRepository;
    private final ApplicantRepository applicantRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final Mappers applicantMapper;
    private final PermitMappers permitMapper;
    private final PermitNumberGenerator permitNumberGenerator;
    private final PaymentGatewayClient paymentGatewayClient;

    @Transactional
    @Override
    public PermitResponse createPermit(CreatePermitRequest request) {

        // ── STEP 1: Find or create the applicant ──────────────────────────
        Applicant applicant = findOrCreateApplicant(request.getApplicant());
        log.info("Applicant resolved: {} ({})", applicant.getNationalId(), applicant.getId());


        PaymentVerificationResponse paymentResponse = verifyPayment(request, applicant);
        log.info("Payment verification result: {}", paymentResponse.getStatus());

        PermitStatus initialStatus = resolvePermitStatus(paymentResponse.getStatus());

        Permit permit = buildPermit(request, applicant, initialStatus);
        permit = permitRepository.save(permit);
        log.info("Permit created: {} with status: {}", permit.getPermitNumber(), permit.getStatus());

        PaymentRecord paymentRecord = buildPaymentRecord(permit, paymentResponse);
        paymentRecordRepository.save(paymentRecord);


        PermitResponse response = permitMapper.toResponse(permit);
        response.setMessage(resolveResponseMessage(initialStatus));
        response.setPaymentStatus(PaymentStatus.valueOf(paymentResponse.getStatus()));
        return response;

    }



    @Transactional(readOnly = true)
    public List<PermitSummaryResponse> getAllPermitsSummary() {
        return permitRepository.findAllWithApplicant()
                .stream()
                .map(permitMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }







    private PaymentVerificationResponse verifyPayment(CreatePermitRequest request,
                                                      Applicant applicant) {
        try {
            PaymentVerificationRequest paymentRequest = PaymentVerificationRequest.builder()
                    .nationalId(applicant.getNationalId())
                    .amount(new BigDecimal("15000.00"))
                    .permitType(request.getPermitType().name())
                    .build();

            return paymentGatewayClient
                    .verifyPayment(paymentRequest)
                    .get(3, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.warn("Payment verification failed. Defaulting to PENDING. Cause: {}", e.getMessage());
            return PaymentVerificationResponse.builder()
                    .id("TIMEOUT-" + UUID.randomUUID())
                    .status("PAYMENT_PENDING")
                    .message("Payment verification could not be completed at this time.")
                    .build();
        }
    }

    private Applicant findOrCreateApplicant(ApplicantRequestDto request) {
        return applicantRepository
                .findByNationalId(request.getNationalId())
                .orElseGet(() -> {
                    log.info("New applicant detected. Creating record for: {}", request.getNationalId());
                    Applicant newApplicant = applicantMapper.toEntity(request);
                    return applicantRepository.save(newApplicant);
                });
    }

    private PermitStatus resolvePermitStatus(String paymentStatus) {
        return switch (paymentStatus) {
            case "SUCCESS" -> PermitStatus.PAYMENT_VERIFIED;
            case "FAILED"  -> PermitStatus.REJECTED;
            default        -> PermitStatus.PENDING_PAYMENT;
        };
    }

    private Permit buildPermit(CreatePermitRequest request,
                               Applicant applicant,
                               PermitStatus status) {
        return Permit.builder()
                .id(UUID.randomUUID())
                .permitNumber(permitNumberGenerator.generate(request.getPermitType()))
                .permitType(request.getPermitType())
                .status(status)
                .description(request.getDescription())
                .applicant(applicant)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private PaymentRecord buildPaymentRecord(Permit permit,
                                             PaymentVerificationResponse paymentResponse) {
        return PaymentRecord.builder()
                .id(UUID.randomUUID())
                .permit(permit)
                .paymentReference(paymentResponse.getId())
                .status(PaymentStatus.valueOf(paymentResponse.getStatus()))
                .gatewayResponse(paymentResponse.getMessage())
                .attemptCount(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String resolveResponseMessage(PermitStatus status) {
        return switch (status) {
            case PAYMENT_VERIFIED -> "Payment confirmed. Your permit application is under review.";
            case PENDING_PAYMENT  -> "Payment verification pending. Your application has been saved.";
            case REJECTED         -> "Payment failed. Please retry your application.";
            default               -> "Your permit application has been received.";
        };
    }


}
