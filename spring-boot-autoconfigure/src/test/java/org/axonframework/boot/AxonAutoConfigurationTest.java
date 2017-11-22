/*
 * Copyright (c) 2010-2017. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.boot;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.Configurer;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.saga.SagaEventHandler;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.axonframework.messaging.annotation.MultiParameterResolverFactory;
import org.axonframework.messaging.annotation.ParameterResolverFactory;
import org.axonframework.messaging.annotation.SimpleResourceParameterResolverFactory;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.messaging.correlation.SimpleCorrelationDataProvider;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.config.AxonConfiguration;
import org.axonframework.spring.stereotype.Aggregate;
import org.axonframework.spring.stereotype.Saga;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@ContextConfiguration(classes = AxonAutoConfigurationTest.Context.class)
@EnableAutoConfiguration(exclude = {JmxAutoConfiguration.class, WebClientAutoConfiguration.class,
                                    HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class})
@RunWith(SpringRunner.class)
public class AxonAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Configurer configurer;

    @Autowired
    private AxonConfiguration configuration;

    @Test
    public void testContextInitialization() throws Exception {
        assertNotNull(applicationContext);
        assertNotNull(configurer);

        assertNotNull(applicationContext.getBean(CommandBus.class));
        assertNotNull(applicationContext.getBean(EventBus.class));
        assertNotNull(applicationContext.getBean(EventStore.class));
        assertNotNull(applicationContext.getBean(CommandGateway.class));
        assertNotNull(applicationContext.getBean(Serializer.class));
        assertEquals(MultiParameterResolverFactory.class, applicationContext.getBean(ParameterResolverFactory.class).getClass());
        assertEquals(1, applicationContext.getBeansOfType(EventStorageEngine.class).size());
        assertEquals(0, applicationContext.getBeansOfType(TokenStore.class).size());
        assertNotNull(applicationContext.getBean(Context.MySaga.class));
        assertNotNull(applicationContext.getBean(Context.MyAggregate.class));

        assertEquals(2, configuration.correlationDataProviders().size());
    }

    @Configuration
    public static class Context {

        @Bean
        public SnapshotTriggerDefinition snapshotTriggerDefinition() {
            return new EventCountSnapshotTriggerDefinition(mock(Snapshotter.class), 2);
        }

        @Bean
        public ParameterResolverFactory customerParameterResolverFactory() {
            return new SimpleResourceParameterResolverFactory(singleton(new CustomResource()));
        }

        @Bean
        public EventStorageEngine storageEngine() {
            return new InMemoryEventStorageEngine();
        }

        @Bean
        public CorrelationDataProvider correlationData1() {
            return new SimpleCorrelationDataProvider("key1");
        }

        @Bean
        public CorrelationDataProvider correlationData2() {
            return new SimpleCorrelationDataProvider("key2");
        }

        @Aggregate(snapshotTriggerDefinition = "snapshotTriggerDefinition")
        public static class MyAggregate {

            @CommandHandler
            public void handle(String type, SomeComponent test, CustomResource resource) {

            }

            @EventHandler
            public void on(String type, SomeComponent test) {

            }
        }

        @Saga
        public static class MySaga {
            @SagaEventHandler(associationProperty = "toString")
            public void handle(String type, SomeComponent test) {

            }

        }

        @Component
        public static class SomeComponent {

            @EventHandler
            public void handle(String event, SomeOtherComponent test) {

            }

        }

        @Component
        public static class SomeOtherComponent {
        }


    }
    public static class CustomResource {

    }
}