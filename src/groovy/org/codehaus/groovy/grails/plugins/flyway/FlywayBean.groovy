/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.flyway

import grails.util.GrailsUtil

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.flyway.migration.GroovyAwareMigrationProvider
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate

import com.googlecode.flyway.core.Flyway
import com.googlecode.flyway.core.dbsupport.DbSupport
import com.googlecode.flyway.core.dbsupport.DbSupportFactory
import com.googlecode.flyway.core.exception.FlywayException
import com.googlecode.flyway.core.metadatatable.MetaDataTable
import com.googlecode.flyway.core.migration.DbMigrator
import com.googlecode.flyway.core.migration.Migration
import com.googlecode.flyway.core.migration.MigrationProvider

/**
 * @author Daniel Henrique Alves Lima
 */
class FlywayBean extends Flyway implements GrailsApplicationAware {

    /**
     * The file name prefix for groovy migrations. (default: V)
     */
    String groovyMigrationPrefix = "V"

    /**
     * The file name suffix for groovy migrations. (default: .groovy)
     */
    String groovyMigrationSuffix = ".groovy";


    private TransactionTemplate transactionTemplate
    private JdbcTemplate jdbcTemplate
    private DbSupport dbSupport

    private Logger log = Logger.getLogger(getClass())
    private GrailsApplication grailsApplication

    @Override
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication

        def config = grailsApplication.getConfig()
        boolean hasDbCreate = config.dataSource?.containsKey('dbCreate')
        def dbCreate = hasDbCreate? config.dataSource.dbCreate : null

        if (dbCreate) {
            log.error "It's not a good idea using flyway with dataSource.dbCreate = ${dbCreate}"
        }
    }


    // TODO: Remove this copy & paste workaround
    @Override
    public int migrate() throws FlywayException {
        try {
            performSetup()
            MigrationProvider migrationProvider =
                    new GroovyAwareMigrationProvider(basePackage, baseDir, encoding, sqlMigrationPrefix, sqlMigrationSuffix, groovyMigrationPrefix, groovyMigrationSuffix, placeholders, placeholderPrefix, placeholderSuffix);
            List<Migration> availableMigrations = migrationProvider.findAvailableMigrations();
            if (availableMigrations.isEmpty()) {
                return 0;
            }


            MetaDataTable metaDataTable = createMetaDataTable();

            validate();

            metaDataTable.createIfNotExists();

            DbMigrator dbMigrator =
                    new DbMigrator(transactionTemplate, jdbcTemplate, dbSupport, metaDataTable, target, ignoreFailedFutureMigration);
            return dbMigrator.migrate(availableMigrations);
        } catch (Exception e) {
            GrailsUtil.deepSanitize(e)
            e.printStackTrace()
        }
    }


    // TODO: Remove this copy & paste workaround
    void performSetup() throws FlywayException {
        super.performSetup()

        transactionTemplate = new TransactionTemplate(transactionManager);
        jdbcTemplate = new JdbcTemplate(dataSource);

        dbSupport = DbSupportFactory.createDbSupport(jdbcTemplate);
    }



    /**
     * @return A new, fully configured, MetaDataTable instance.
     */
    private MetaDataTable createMetaDataTable() {
        return new MetaDataTable(this.@transactionTemplate, jdbcTemplate, dbSupport, schemas[0], table);
    }
}
