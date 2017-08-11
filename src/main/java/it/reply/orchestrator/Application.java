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

package it.reply.orchestrator;

import bitronix.tm.jndi.BitronixInitialContextFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class Application {

  public static final Class<Application> applicationClass = Application.class;

  static {
    // JBPM needs a JNDI context from which retrieve the UT and TSR
    System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
        BitronixInitialContextFactory.class.getName());
  }

  public static void main(String[] args) {
    SpringApplication.run(applicationClass, args);
  }

}
