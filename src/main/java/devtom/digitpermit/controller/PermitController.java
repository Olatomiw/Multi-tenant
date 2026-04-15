package devtom.digitpermit.controller;

import devtom.digitpermit.dtO.req.CreatePermitRequest;
import devtom.digitpermit.dtO.res.PermitResponse;
import devtom.digitpermit.dtO.res.PermitSummaryResponse;
import devtom.digitpermit.service.PermitService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api")
@Slf4j
public class PermitController {

    private final PermitService permitService;

    public PermitController(PermitService permitService) {
        this.permitService = permitService;
    }


    @PostMapping("/permits")
    public ResponseEntity<PermitResponse> createPermit(@RequestHeader(name = "X-Tenant-Id") String header,
                                                       @Valid @RequestBody CreatePermitRequest request) {
        log.info("Received permit application for type: {}", request.getPermitType());
        PermitResponse response = permitService.createPermit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/permits/summary")
    public ResponseEntity<List<PermitSummaryResponse>> getAllPermitsSummary() {
        List<PermitSummaryResponse> allPermitsSummary = permitService.getAllPermitsSummary();
        return ResponseEntity.ok(allPermitsSummary);
    }
}
