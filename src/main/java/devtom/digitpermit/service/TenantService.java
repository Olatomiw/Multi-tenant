package devtom.digitpermit.service;

import devtom.digitpermit.Model.Tenant;
import devtom.digitpermit.enums.Status;
import devtom.digitpermit.repository.TenantRepository;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TenantService {

    private final DataSource dataSource;
    private final TenantRepository tenantRepository;

    public TenantService(DataSource dataSource, TenantRepository tenantRepository) {
        this.dataSource = dataSource;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public void createTenant(String tenantId, String schemaName) {
        if (!schemaName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid SchemaName");
        }
        try(Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()){
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tenants")
                .schemas(schemaName)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setTenantId(tenantId);
        tenant.setSchemaName(schemaName);
        tenant.setName(tenantId);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setStatus(Status.ACTIVE);

        tenantRepository.save(tenant);

    }
}
