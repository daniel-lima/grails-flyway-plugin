package org.codehaus.groovy.grails.plugins.flyway.migration;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import com.googlecode.flyway.core.migration.Migration;
import com.googlecode.flyway.core.migration.java.JavaMigration;
import com.googlecode.flyway.core.migration.java.JavaMigrationChecksumProvider;
import com.googlecode.flyway.core.migration.java.JavaMigrationExecutor;
import com.googlecode.flyway.core.migration.java.JavaMigrationResolver;

public class EnhancedJavaMigrationResolver extends JavaMigrationResolver {

    private Logger log = Logger.getLogger(getClass());

    /**
     * The base directory on the classpath where to migrations are located.
     */
    private final String baseDir;

    /**
     * The base package on the classpath where to migrations are located.
     */
    private final String basePackage;

    public EnhancedJavaMigrationResolver(String baseDir, String basePackage) {
        super(basePackage);
        this.baseDir = baseDir;
        this.basePackage = basePackage;
    }

    public Collection<Migration> resolveMigrations() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            cl = this.getClass().getClassLoader();
        }
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader(cl);

        Collection<Migration> migrations = new ArrayList<Migration>();

        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(
                false);
        provider.addIncludeFilter(new AssignableTypeFilter(JavaMigration.class));
        Set<BeanDefinition> components = provider
                .findCandidateComponents(basePackage);
        final CRC32 crc32 = new CRC32();
        String sourcePrefix = "classpath:" + this.baseDir + "/";
        for (BeanDefinition beanDefinition : components) {
            crc32.reset();
            Class<?> clazz = ClassUtils.resolveClassName(
                    beanDefinition.getBeanClassName(), cl);
            JavaMigration javaMigration = (JavaMigration) BeanUtils
                    .instantiateClass(clazz);
            String originalClassName = null;
            
            if (!(javaMigration instanceof JavaMigrationChecksumProvider)) {
                String sourcePath = javaMigration.getClass().getName();
                sourcePath = sourcePath.replace('.', '/');
                sourcePath = sourcePrefix + sourcePath + ".java";
                if (log.isDebugEnabled()) {
                    log.debug("resolveMigrations(): Searching '" + sourcePath
                            + "' for '" + javaMigration.getClass().getName()
                            + "'");
                }

                Resource source = resourceLoader.getResource(sourcePath);
                if (source != null && source.exists()) {
                    calculateChecksum(crc32, source);
                    final int checksum = (int) crc32.getValue();
                    originalClassName = javaMigration.getClass().getName();
                    javaMigration = JavaMigrationProxy.newInstance(
                            javaMigration, checksum);

                    if (log.isDebugEnabled()) {
                        log.debug("resolveMigrations(): Proxy " + javaMigration
                                + " created");
                    }
                }
            }

            migrations.add(new EnhancedJavaMigrationExecutor(javaMigration, originalClassName));
        }

        return migrations;
    }

    private void calculateChecksum(final CRC32 crc32, Resource source) {
        InputStream input = null;
        try {
            try {
                input = new CheckedInputStream(new BufferedInputStream(
                        source.getInputStream()), crc32);

                while (input.read() >= 0);
            } finally {
                input.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class JavaMigrationProxy implements InvocationHandler {

        private JavaMigration obj;
        private int checksum;

        public static JavaMigration newInstance(JavaMigration obj, int checksum) {
            Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
            interfaces.addAll(Arrays.asList(obj.getClass().getInterfaces()));
            interfaces.add(ClassUtils.resolveClassName(
                    JavaMigrationChecksumProvider.class.getName(), obj
                            .getClass().getClassLoader()));

            return (JavaMigration) Proxy.newProxyInstance(obj.getClass()
                    .getClassLoader(), interfaces.toArray(new Class[interfaces
                    .size()]), new JavaMigrationProxy(obj, checksum));
        }

        private JavaMigrationProxy(JavaMigration obj, int checksum) {
            this.obj = obj;
            this.checksum = checksum;
        }

        public Object invoke(Object proxy, Method m, Object[] args)
                throws Throwable {
            Object result;

            if (!m.getName().equals("getChecksum")) {
                result = m.invoke(obj, args);
            } else {
                result = new Integer(checksum);
            }

            return result;
        }
    }

}
