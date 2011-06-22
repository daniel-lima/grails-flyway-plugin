package org.codehaus.groovy.grails.plugins.flyway.migration;

import org.springframework.util.ClassUtils;

import com.googlecode.flyway.core.migration.MigrationInfoHelper;
import com.googlecode.flyway.core.migration.java.JavaMigration;
import com.googlecode.flyway.core.migration.java.JavaMigrationExecutor;
import com.googlecode.flyway.core.migration.java.JavaMigrationInfoProvider;

public class EnhancedJavaMigrationExecutor extends JavaMigrationExecutor {
    
    public EnhancedJavaMigrationExecutor(JavaMigration javaMigration) {
        this(javaMigration, null);
    }

    public EnhancedJavaMigrationExecutor(JavaMigration derivedJavaMigration,
            String originalClassName) {
        super(derivedJavaMigration);

        if (originalClassName != null) {
            if (ClassUtils.isAssignableValue(JavaMigrationInfoProvider.class,
                    derivedJavaMigration)) {
                JavaMigrationInfoProvider infoProvider = (JavaMigrationInfoProvider) derivedJavaMigration;
                schemaVersion = infoProvider.getVersion();
                description = infoProvider.getDescription();
            } else {
                String nameWithoutV = ClassUtils
                        .getShortName(originalClassName).substring(1);
                schemaVersion = MigrationInfoHelper
                        .extractSchemaVersion(nameWithoutV);
                description = MigrationInfoHelper
                        .extractDescription(nameWithoutV);
            }

            script = originalClassName;
        }
    }


}
