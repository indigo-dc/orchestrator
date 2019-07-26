/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.dal.util;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.annotations.VisibleForTesting;

import java.io.Serializable;

import org.flowable.engine.common.impl.cfg.IdGenerator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

public class StrongSequentialUuidGenerator implements IdGenerator, IdentifierGenerator {

  private static TimeBasedGenerator timeBasedGenerator = Generators
      .timeBasedGenerator(EthernetAddress.fromInterface());

  public static String generate() {
    return toSequentialUuid(timeBasedGenerator.generate().toString());
  }

  @Override
  public Serializable generate(SessionImplementor session, Object object) {
    return generate();
  }

  @Override
  public String getNextId() {
    return generate();
  }

  @VisibleForTesting
  protected static String toSequentialUuid(String nonSequentialUuid) {
    char[] oldChars = nonSequentialUuid.toCharArray();
    char[] newChars = new char[36];
    System.arraycopy(oldChars, 0, newChars, 9, 4);
    System.arraycopy(oldChars, 4, newChars, 14, 4);
    System.arraycopy(oldChars, 8, newChars, 8, 1);
    System.arraycopy(oldChars, 9, newChars, 4, 4);
    System.arraycopy(oldChars, 13, newChars, 13, 1);
    System.arraycopy(oldChars, 14, newChars, 0, 4);
    System.arraycopy(oldChars, 18, newChars, 18, 18);
    return new String(newChars);
  }

  @VisibleForTesting
  protected static String fromSequentialUuid(String sequentialUuid) {
    char[] oldChars = sequentialUuid.toCharArray();
    char[] newChars = new char[36];
    System.arraycopy(oldChars, 0, newChars, 14, 4);
    System.arraycopy(oldChars, 4, newChars, 9, 4);
    System.arraycopy(oldChars, 8, newChars, 8, 1);
    System.arraycopy(oldChars, 9, newChars, 0, 4);
    System.arraycopy(oldChars, 13, newChars, 13, 1);
    System.arraycopy(oldChars, 14, newChars, 4, 4);
    System.arraycopy(oldChars, 18, newChars, 18, 18);
    return new String(newChars);
  }

}
