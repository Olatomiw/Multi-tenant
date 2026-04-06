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
    public String hello(@RequestHeader("X-Tenant-Id") String header, @RequestParam String name){
        tenantService.createTenant(header, name);
        return "Hello World";
    }
}
