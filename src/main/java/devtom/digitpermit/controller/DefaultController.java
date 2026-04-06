package devtom.digitpermit.controller;

import devtom.digitpermit.service.TenantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DefaultController {

    private final TenantService tenantService;

    public DefaultController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/hello")
    public String hello(@RequestHeader String X_Tenant_Id, @RequestParam String name){
        tenantService.createTenant(X_Tenant_Id,name + X_Tenant_Id);
        return "Hello World";
    }
}
