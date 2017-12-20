/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteSpring;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.TransactionConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class IgniteConfig {

  @Bean
  IgniteConfiguration igniteConfiguration(JtaTransactionManager transactionManager) {

    TransactionConfiguration txCfg = new TransactionConfiguration()
        .setTxManagerFactory(() -> transactionManager.getTransactionManager());

    return new IgniteConfiguration()
        .setGridLogger(new Slf4jLogger())
        .setClientMode(false)
        .setActiveOnStart(true)
        .setTransactionConfiguration(txCfg)
        .setMetricsLogFrequency(0);
  }

  @Bean(destroyMethod = "close")
  Ignite ignite(ApplicationContext applicationContext,
      IgniteConfiguration igniteConfiguration) throws IgniteCheckedException {
    Ignite ignite = null;
    try {
      ignite = IgniteSpring.start(igniteConfiguration, applicationContext);
    } catch (Exception ex) {
      if (ignite != null) {
        ignite.close();
      }
      throw ex;
    }
    return ignite;
  }
}
