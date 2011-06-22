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

import java.util.Collection
import java.util.zip.CRC32

import org.apache.log4j.Logger
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.jdbc.core.JdbcTemplate

import com.googlecode.flyway.core.exception.FlywayException
import com.googlecode.flyway.core.migration.Migration
import com.googlecode.flyway.core.migration.MigrationInfoHelper
import com.googlecode.flyway.core.migration.MigrationResolver
import com.googlecode.flyway.core.migration.SchemaVersion
import com.googlecode.flyway.core.migration.java.JavaMigrationExecutor
import com.googlecode.flyway.core.migration.sql.SqlMigrationResolver
import com.googlecode.flyway.core.util.ResourceUtils


class GroovyMigrationResolver implements MigrationResolver {

    private Logger log = Logger.getLogger(getClass())

    /**
     * The base directory on the classpath where to migrations are located.
     */
    private final String baseDir;


    /**
     * The encoding of Groovy migrations.
     */
    private final String encoding;

    /**
     * The prefix for groovy migrations
     */
    private final String groovyMigrationPrefix;

    /**
     * The suffix for groovy migrations
     */
    private final String groovyMigrationSuffix;

    /**
     * Creates a new instance.
     *
     * @param baseDir             The base directory on the classpath where to migrations are located.
     * @param placeholderReplacer The placeholder replacer to apply to groovy migration scripts.
     * @param encoding            The encoding of Groovy migrations.
     * @param groovyMigrationPrefix  The prefix for groovy migrations
     * @param groovyMigrationSuffix  The suffix for groovy migrations
     */
    public GroovyMigrationResolver(String baseDir, String encoding, String groovyMigrationPrefix, String groovyMigrationSuffix) {
        this.baseDir = baseDir
        this.encoding = encoding
        this.groovyMigrationPrefix = groovyMigrationPrefix
        this.groovyMigrationSuffix = groovyMigrationSuffix
    }



    @Override
    public Collection<Migration> resolveMigrations() {
        ClassLoader cl = Thread.currentThread().contextClassLoader
        if (!cl) {
            cl = this.class.classLoader
        }
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver =
                new PathMatchingResourcePatternResolver(cl);

        GroovyClassLoader groovyClassLoader = (cl instanceof GroovyClassLoader)? cl: new GroovyClassLoader(cl)


        Collection<Migration> migrations = new ArrayList<Migration>()

        Resource classPathBaseDir = new ClassPathResource("${baseDir}/")
        if (!classPathBaseDir.exists()) {
            log.warn("Unable to find path for groovy migrations: ${baseDir}")
            return migrations
        }

        Resource[] resources;
        try {
            String searchRoot = "${baseDir}/"
            final String searchPattern = "**/${groovyMigrationPrefix}?*${groovyMigrationSuffix}"
            resources = pathMatchingResourcePatternResolver.getResources("classpath*:${searchRoot}${searchPattern}")

            final CRC32 crc32 = new CRC32()

            for (resource in resources) {
                crc32.reset()
                final String versionString =
                        extractVersionStringFromFileName(resource.filename, groovyMigrationPrefix, groovyMigrationSuffix)
                String uri = resource.getURI().toString()
                String scriptName = uri.substring(uri.indexOf(searchRoot) + searchRoot.length())

                // TODO: Use Reader and GroovyCodeSource instead of String
                String scriptSource = ResourceUtils.loadResourceAsString(resource, encoding)
                // TODO: Use CheckedInputStream to calculate the checksum
                crc32.update(scriptSource.bytes)

                def script = (Class) groovyClassLoader.parseClass(scriptSource)
                script = script.newInstance()

                // TODO: Check for description property existence and migrate closure existence
                Map migration = null
                migration = [
                            version: (SchemaVersion) MigrationInfoHelper.extractSchemaVersion(versionString),
                            description: (String) MigrationInfoHelper.extractDescription(versionString),
                            checksum: (Integer) crc32.value,
                            getVersion: {return migration.version},
                            getDescription: {return migration.description},
                            getChecksum: {return migration.checksum},
                            migrate: {JdbcTemplate jdbcTemplate ->
                                script.setProperty('jdbcTemplate', jdbcTemplate)
                                script.run()
                            }
                        ]

                GroovyMigration groovyMigration = migration as GroovyMigration
                JavaMigrationExecutor migrationAdapter = new JavaMigrationExecutor(groovyMigration)
                migrationAdapter.@script = scriptName
                migrations.add(migrationAdapter)
            }
        } catch (IOException e) {
            throw new FlywayException("Error loading sql migration files", e)
        }

        return migrations
    }


    /**
     * Extracts the groovy file version string from this file name.
     *
     * @param fileName The file name to parse.
     * @param prefix   The prefix to extract
     * @param suffix   The suffix to extract
     *
     * @return The version string.
     */
    static String extractVersionStringFromFileName(String fileName, String prefix, String suffix) {
        return SqlMigrationResolver.extractVersionStringFromFileName(fileName, prefix, suffix)
    }
}
