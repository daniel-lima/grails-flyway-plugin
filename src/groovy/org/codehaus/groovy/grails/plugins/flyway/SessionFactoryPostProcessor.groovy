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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Daniel Henrique Alves Lima
 */
class SessionFactoryPostProcessor implements BeanFactoryPostProcessor/*, ApplicationContextAware */{

    private ApplicationContext appCtx

    /*@Override
    public void setApplicationContext(ApplicationContext context)
    throws BeansException {
        println context
    }*/



    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory)
    throws BeansException {
        //println "${factory}"
        BeanDefinition sessionFactoryDef = factory.getBeanDefinition('transactionManager')
        def dependsOn = sessionFactoryDef.dependsOn
        
        dependsOn = dependsOn? new ArrayList(dependsOn) : new ArrayList()
        dependsOn.add('flyway')
        
        //println ''
        //println "sessionFactoryDef ${sessionFactoryDef.properties}"
        sessionFactoryDef.dependsOn = dependsOn
    }



    /*@Override
     public Object postProcessAfterInitialization(Object bean, String beanName)
     throws BeansException {
     println "${beanName} ${bean}"
     return bean
     }
     @Override
     public Object postProcessBeforeInitialization(Object bean, String beanName)
     throws BeansException {
     println "${beanName} ${bean}"
     return bean
     }*/
}
