package devtom.digitpermit.service;

import devtom.digitpermit.dtO.req.CreatePermitRequest;
import devtom.digitpermit.dtO.res.PermitResponse;

public interface PermitService {

    PermitResponse createPermit(CreatePermitRequest createPermitRequest);
}
