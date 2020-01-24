/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

package it.reply.orchestrator.config.amqp;

import it.reply.orchestrator.controller.AmqpListener;
import it.reply.orchestrator.service.DeploymentSchedulerService;
import it.reply.orchestrator.service.DeploymentService;

import lombok.Getter;
import lombok.Setter;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class AmqpConfig {

  @Autowired
  private DeploymentSchedulerService deploymentSchedulerService;

  @Autowired
  private DeploymentService deploymentService;

  @Autowired
  private AmqpListener amqpListener;

  @Value("${orchestrator.rabbitmq.exchange}")
  private String exchange;

  @Value("${orchestrator.rabbitmq.queue}")
  private String queueName;

  @Value("${orchestrator.rabbitmq.routingkey}")
  private String routingKey;

  @Value("${spring.rabbitmq.username}")
  String username;

  @Value("${spring.rabbitmq.password}")
  private String password;

  @Bean
  Queue queue() {
    return new Queue(queueName, true);
  }

  @Bean
  DirectExchange exchange() {
    return new DirectExchange(exchange);
  }

  // create MessageListenerContainer using default connection factory
  @Bean
  MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory) {
    SimpleMessageListenerContainer simpleMessageListenerContainer =
        new SimpleMessageListenerContainer();
    // use of custom connection factory
    simpleMessageListenerContainer.setConnectionFactory(connectionFactory());
    simpleMessageListenerContainer.setQueues(queue());
    simpleMessageListenerContainer.setMessageListener(amqpListener);
    return simpleMessageListenerContainer;

  }

  // create custom connection factory

  @Bean
  ConnectionFactory connectionFactory() {
    CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory("localhost");
    cachingConnectionFactory.setUsername(username);
    cachingConnectionFactory.setUsername(password);
    cachingConnectionFactory.setPort(8008);
    return cachingConnectionFactory;
  }

}
