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

package it.reply.orchestrator.config;

import it.reply.orchestrator.config.properties.OrchestratorProperties;

import java.util.Objects;

import javax.cache.configuration.Factory;
import javax.transaction.TransactionManager;

import lombok.AllArgsConstructor;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteSpring;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.TransactionConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class IgniteConfig {

  @AllArgsConstructor
  public static class TransactionManagerFactory implements Factory<TransactionManager> {

    @NonNull
    private transient TransactionManager transactionManager;

    @Override
    public TransactionManager create() {
      return Objects.requireNonNull(transactionManager);
    }
  }

  /**
   * Generates a new IgniteConfiguration.
   *
   * @return the generated IgniteConfiguration
   */
  @Bean
  public IgniteConfiguration igniteConfiguration(JtaTransactionManager transactionManager,
      OrchestratorProperties orchestratorProperties) {

    TransactionConfiguration txCfg = new TransactionConfiguration().setTxManagerFactory(
        new TransactionManagerFactory(transactionManager.getTransactionManager()));

    IgniteConfiguration igniteConfiguration = new IgniteConfiguration()
        .setGridLogger(new Slf4jLogger())
        .setClientMode(false)
        .setActiveOnStart(true)
        .setTransactionConfiguration(txCfg)
        .setMetricsLogFrequency(0);

    if (!orchestratorProperties.isClustered()) {
      TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi()
          .setIpFinder(new TcpDiscoveryVmIpFinder(true));
      igniteConfiguration = igniteConfiguration.setDiscoverySpi(discoverySpi);
    }
    return igniteConfiguration;
  }

  /**
   * Generates a new Ignite instance.
   *
   * @return the generated Ignite instance
   */
  @Bean(destroyMethod = "close")
  public Ignite ignite(ApplicationContext applicationContext,
      IgniteConfiguration igniteConfiguration) throws IgniteCheckedException {
    return IgniteSpring.start(igniteConfiguration, applicationContext);
  }
}
