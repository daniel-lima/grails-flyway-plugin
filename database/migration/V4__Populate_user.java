import com.googlecode.flyway.core.migration.java.JavaMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Example of a Java-based migration.
 */
public class V4__Populate_user implements JavaMigration {

    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate
                .execute("INSERT INTO test_user (name) VALUES ('Obelix_java')");
    }
}