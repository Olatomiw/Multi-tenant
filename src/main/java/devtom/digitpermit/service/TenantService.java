package devtom.digitpermit.service;

import devtom.digitpermit.Model.Tenant;
import devtom.digitpermit.config.RabbitMqProducer;
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
    private final RabbitMqProducer rabbitMqProducer;

    public TenantService(DataSource dataSource, TenantRepository tenantRepository, RabbitMqProducer rabbitMqProducer) {
        this.dataSource = dataSource;
        this.tenantRepository = tenantRepository;
        this.rabbitMqProducer = rabbitMqProducer;
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
            throw new RuntimeException("Failed to create schema: " + schemaName, e);
        }

        try{
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration/tenants")
                    .schemas(schemaName)
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();

            rabbitMqProducer.send("Tenant Created in DB: " + tenantId + "schema: " + schemaName);

        }catch (Exception e){
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
            } catch (SQLException dropException) {
                throw new RuntimeException("Migration failed and schema cleanup also failed for: " + schemaName, dropException);
            }
            throw new RuntimeException("Flyway migration failed for schema: " + schemaName, e);
        }

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
