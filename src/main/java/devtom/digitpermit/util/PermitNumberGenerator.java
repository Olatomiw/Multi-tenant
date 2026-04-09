package devtom.digitpermit.util;

import devtom.digitpermit.enums.PermitType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PermitNumberGenerator {

    public String generate(PermitType permitType) {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String sequence = String.format("%05d", (int) (Math.random() * 99999));
        String prefix = permitType.name().substring(0, 3).toUpperCase();
        return prefix + "-" + year + "-" + sequence;
        // Produces e.g. HEA-2024-00147
    }
}
