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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EnumUtils {

  private static final Logger LOG = LoggerFactory.getLogger(EnumUtils.class);

  /**
   * Retrieve a enum value from its class and a user defined predicate.
   * 
   * @param enumClass
   *          the class of the enum
   * @param predicate
   *          the the predicate
   * @return the optional enum value
   */
  public static <T extends Enum<T>> Optional<T> fromPredicate(Class<T> enumClass,
      Predicate<? super T> predicate) {
    Objects.requireNonNull(enumClass);
    Objects.requireNonNull(predicate);
    return Arrays.stream(enumClass.getEnumConstants()).filter(predicate).findFirst();
  }

  /**
   * Collect all the enums represented by a class in a List of objects returned by a mapper
   * function.
   * 
   * @param enumClass
   *          enums the class
   * @param mapper
   *          the mapper fuction
   * @return the list
   */
  public static <T extends Enum<T>, R> List<R> toList(Class<T> enumClass,
      Function<? super T, R> mapper) {
    Objects.requireNonNull(enumClass);
    Objects.requireNonNull(mapper);
    return Arrays.stream(enumClass.getEnumConstants()).map(mapper).collect(Collectors.toList());
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
   * @param enumClass
   *          the class of the enum
   * @param name
   *          the enum name
   * @return the enum value, can be null
   */
  public static <T extends Enum<T> & Named> Optional<T> fromName(Class<T> enumClass, String name) {
    Objects.requireNonNull(enumClass);
    Objects.requireNonNull(name);
    return fromPredicate(enumClass, input -> name.equals(input.getName()));
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
    return fromName(enumClass, name).orElseThrow(() -> new NoSuchElementException(
        "No enum found with name [" + name + "] in class [" + enumClass + "]"));
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

    return fromName(enumClass, name).orElseGet(() -> {
      LOG.warn("No enum found with name [{}] in class [{}], using value [{}] as default", name,
          enumClass, defaultValue);
      return defaultValue;
    });

  }

}
