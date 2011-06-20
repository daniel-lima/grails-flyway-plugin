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
package org.codehaus.groovy.grails.plugins.flyway.migration

import java.util.List

import org.apache.log4j.Logger

import com.googlecode.flyway.core.exception.FlywayException
import com.googlecode.flyway.core.migration.Migration
import com.googlecode.flyway.core.migration.MigrationProvider
import com.googlecode.flyway.core.migration.MigrationResolver
import com.googlecode.flyway.core.migration.java.JavaMigrationResolver
import com.googlecode.flyway.core.migration.sql.PlaceholderReplacer
import com.googlecode.flyway.core.migration.sql.SqlMigrationResolver

class GroovyAwareMigrationProvider extends MigrationProvider {
    
    private Logger log = Logger.getLogger(getClass());

    /**
     * The base package where the Java migrations are located.
     */
    private final String basePackage;

    /**
     * The base directory on the classpath where the Sql migrations are located.
     */
    private final String baseDir;

    /**
     * The encoding of Sql migrations.
     */
    private final String encoding;

    /**
     * The file name prefix for sql migrations.
     */
    private final String sqlMigrationPrefix;

    /**
     * The file name suffix for sql migrations.
     */
    private final String sqlMigrationSuffix;

    /**
     * A map of &lt;placeholder, replacementValue&gt; to apply to sql migration scripts.
     */
    private final Map<String, String> placeholders;

    /**
     * The prefix of every placeholder.
     */
    private final String placeholderPrefix;

    /**
     * The suffix of every placeholder.
     */
    private final String placeholderSuffix;

    /**
     * The available migrations, sorted by version, newest first. An empty list is returned when no migrations can be
     * found.
     */
    private List<Migration> availableMigrations;




    /**
     * The file name prefix for groovy migrations.
     */
    private final String groovyMigrationPrefix;

    /**
     * The file name suffix for groovy migrations.
     */
    private final String groovyMigrationSuffix;


    /**
     * Creates a new MigrationProvider.
     *
     * @param basePackage        The base package where the Java migrations are located.
     * @param baseDir            The base directory on the classpath where the Sql migrations are located.
     * @param encoding           The encoding of Sql migrations.
     * @param sqlMigrationPrefix The file name prefix for sql migrations.
     * @param sqlMigrationSuffix The file name suffix for sql migrations.
     * @param placeholders       A map of <placeholder, replacementValue> to apply to sql migration scripts.
     * @param placeholderPrefix  The prefix of every placeholder.
     * @param placeholderSuffix  The suffix of every placeholder.
     */
    public GroovyAwareMigrationProvider(String basePackage, String baseDir, String encoding, String sqlMigrationPrefix, String sqlMigrationSuffix, String groovyMigrationPrefix, String groovyMigrationSuffix, Map<String, String> placeholders, String placeholderPrefix, String placeholderSuffix) {
        super(basePackage, baseDir, encoding, sqlMigrationPrefix, sqlMigrationSuffix, placeholders, placeholderPrefix, placeholderSuffix);

        this.basePackage = basePackage;
        this.baseDir = baseDir;
        this.encoding = encoding;
        this.sqlMigrationPrefix = sqlMigrationPrefix;
        this.sqlMigrationSuffix = sqlMigrationSuffix;
        this.placeholders = placeholders;
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;


        this.groovyMigrationPrefix = groovyMigrationPrefix;
        this.groovyMigrationSuffix = groovyMigrationSuffix;
    }



    @Override
    public List<Migration> findAvailableMigrations() throws FlywayException {
        if (this.availableMigrations == null) {
            this.availableMigrations = doFindAvailableMigrations();
        }

        return this.availableMigrations;
    }


    /**
     * Finds all available migrations using all migration resolvers (sql, groovy, java, ...).
     *
     * @return The available migrations, sorted by version, newest first. An empty list is returned when no migrations
     *         can be found.
     * @throws com.googlecode.flyway.core.exception.FlywayException
     *          when the available migrations have overlapping versions.
     */
    private List<Migration> doFindAvailableMigrations() throws FlywayException {
        PlaceholderReplacer placeholderReplacer = new PlaceholderReplacer(placeholders, placeholderPrefix, placeholderSuffix);

        Collection<MigrationResolver> migrationResolvers = new ArrayList<MigrationResolver>();
        migrationResolvers.add(new SqlMigrationResolver(this.baseDir, placeholderReplacer, this.encoding, this.sqlMigrationPrefix, this.sqlMigrationSuffix));
        migrationResolvers.add(new GroovyMigrationResolver(baseDir, encoding, groovyMigrationPrefix, groovyMigrationSuffix));
        migrationResolvers.add(new JavaMigrationResolver(basePackage));

        List<Migration> migrations = new ArrayList<Migration>(collectMigrations(migrationResolvers));
        Collections.sort(migrations);
        Collections.reverse(migrations);
        
        if (log.isDebugEnabled()) {
            log.debug("migrations = {migrations}")
        }

        checkForIncompatibilities(migrations);

        return migrations;
    }
}
