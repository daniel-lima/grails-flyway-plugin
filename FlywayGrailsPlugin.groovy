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

/**
 * @author Daniel Henrique Alves Lima
 */
class FlywayGrailsPlugin {
    // the plugin version
    def version = "0.1.0-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2.2 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/**/*"
    ]

    // TODO Fill in these fields
    def author = "Daniel Henrique Alves Lima"
    def authorEmail = "email_daniel_h@yahoo.com.br"
    def title = "Flyway Grails Plugin"
    def description = '''\\.
Provides integration with Flyway (http://code.google.com/p/flyway/).
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/flyway"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
        addBeanFactoryPostProcessor(org.codehaus.groovy.grails.plugins.flyway.SessionFactoryPostProcessor)
        
        flyway(org.codehaus.groovy.grails.plugins.flyway.FlywayBean) {bean->
            bean.initMethod = 'migrate'
            bean.lazyInit = false
            dataSource = ref('dataSource')
            baseDir = 'database/migration'
        }
        
        /*println delegate.getBeanDefinitions()
        
        def sessionFactoryDef = delegate.getBeanDefinition('transactionManager')
        def sessionFactoryPropValues = sessionFactoryDef.propertyValues
        def sessionFactoryDependsOnValue = sessionFactoryPropValues.getPropertyValue('depends-on')
        println "sessionFactoryDependsOnValue ${sessionFactoryDependsOnValue}"*/
    }

    def doWithDynamicMethods = { ctx ->
        // TODO
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
