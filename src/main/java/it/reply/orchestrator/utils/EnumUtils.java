package it.reply.orchestrator.utils;

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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.Objects;

public class EnumUtils {

  private static final Logger LOG = LoggerFactory.getLogger(EnumUtils.class);

  /**
   * <p>
   * Retrieve a enum value from its name and its class.
   * </p>
   * 
   * <p>
   * The enum must extends {@link Named}, and the name value should be the one that would be
   * returned from {@link Named#getName()}.
   * </p>
   * 
   * @param enumClass
   *          the class of the enum
   * @param name
   *          the enum name
   * @return the enum value, can be null
   */
  @Nullable
  public static <T extends Enum<T> & Named> T fromName(Class<T> enumClass, String name) {
    Objects.requireNonNull(enumClass);
    Objects.requireNonNull(name);
    for (T enumItem : org.apache.commons.lang3.EnumUtils.getEnumList(enumClass)) {
      if (name.equals(enumItem.getName())) {
        return enumItem;
      }
    }
    return null;
  }

  /**
   * <p>
   * Retrieve a enum value from its name and its class.
   * </p>
   * 
   * <p>
   * The enum must extends {@link Named}, and the name value should be the one that would be
   * returned from {@link Named#getName()}.
   * </p>
   * 
   * <p>
   * If no enum is found a {@link NoSuchElementException} will be thrown.
   * </p>
   * 
   * @param enumClass
   *          the class of the enum
   * @param name
   *          the enum name
   * @return the enum value, cannot be null
   */
  public static <T extends Enum<T> & Named> T fromNameOrThrow(Class<T> enumClass, String name) {
    @Nullable
    T foundEnum = fromName(enumClass, name);
    if (foundEnum != null) {
      return foundEnum;
    } else {
      throw new NoSuchElementException(
          "No enum found with name [" + name + "] in class [" + enumClass + "]");
    }
  }

  /**
   * <p>
   * Retrieve a enum value from its name and its class.
   * </p>
   * 
   * <p>
   * The enum must extends {@link Named}, and the name value should be the one that would be
   * returned from {@link Named#getName()}.
   * </p>
   * 
   * <p>
   * If no enum is found the default value will be returned.
   * </p>
   * 
   * @param enumClass
   *          the class of the enum
   * @param name
   *          the enum name
   * @param defaultValue
   *          the defaultValue
   * @return the enum value, cannot be null
   */
  public static <T extends Enum<T> & Named> T fromNameOrDefault(Class<T> enumClass, String name,
      T defaultValue) {
    Objects.requireNonNull(defaultValue);
    @Nullable
    T foundEnum = fromName(enumClass, name);
    if (foundEnum != null) {
      return foundEnum;
    } else {
      LOG.warn("No enum found with name [{}] in class [{}], using value [{}] as default", name,
          enumClass, defaultValue);
      return defaultValue;
    }
  }

}
