package devtom.digitpermit.service;

import devtom.digitpermit.dtO.req.CreatePermitRequest;
import devtom.digitpermit.dtO.res.PermitResponse;
import devtom.digitpermit.dtO.res.PermitSummaryResponse;

import java.util.List;

public interface PermitService {

    PermitResponse createPermit(CreatePermitRequest createPermitRequest);

    List<PermitSummaryResponse> getAllPermitsSummary();
}
