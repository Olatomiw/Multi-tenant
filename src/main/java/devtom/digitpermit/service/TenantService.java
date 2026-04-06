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
        if (!tenantId.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid tenant ID");
        }
        try(Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()){
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tenant")
                .schemas(schemaName)
                .load();
        flyway.migrate();

        Tenant tenant = new Tenant();
        tenant.setSchemaName(schemaName);
        tenant.setDatabaseName(tenantId);
        tenant.setStatus(Status.ACTIVE);

        tenantRepository.save(tenant);

    }
}
