package org.codehaus.groovy.grails.plugins.flyway.migration;

import com.googlecode.flyway.core.migration.java.JavaMigration;
import com.googlecode.flyway.core.migration.java.JavaMigrationChecksumProvider;
import com.googlecode.flyway.core.migration.java.JavaMigrationInfoProvider;

public interface GroovyMigration extends JavaMigration,
        JavaMigrationChecksumProvider, JavaMigrationInfoProvider {

}
