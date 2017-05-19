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

package it.reply.orchestrator.utils;

import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;
import es.upv.i3m.grycap.im.exceptions.FileException;

import lombok.experimental.UtilityClass;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class CommonUtils {

  public static String getFileContentAsString(String fileUri) throws FileException {
    return new NoNullOrEmptyFile(new Utf8File(Paths.get(fileUri))).read();
  }

  /**
   * Verify that a <code>@Nullable</code> reference is effectively non null and cast it to a
   * <code>@NonNull</code> reference. If the reference is instead null, a NPE is thrown
   * 
   * @param reference
   *          the nullable reference
   * @return the non null reference
   */
  @NonNull
  public static <T> T checkNotNull(@Nullable T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  /**
   * Verify that a <code>@Nullable</code> reference is effectively non null and cast it to a
   * <code>@NonNull</code> reference. If the reference is instead null, the <code>@NonNull</code>
   * default value is returned
   * 
   * @param reference
   *          the <code>@Nullable</code> reference
   * @param defaultValue
   *          the <code>@NonNull</code> default value
   * @return the <code>@NonNull</code> reference
   */
  @NonNull
  public static <T> T notNullOrDefaultValue(@Nullable T reference, @NonNull T defaultValue) {
    if (reference == null) {
      return checkNotNull(defaultValue);
    } else {
      return reference;
    }
  }

  public static <K, V> Optional<V> getFromOptionalMap(@Nullable Map<K, V> optionalMap, K key) {
    return Optional.ofNullable(optionalMap).map(map -> checkNotNull(map).get(key));
  }

  public static <K, V> Optional<V> removeFromOptionalMap(@Nullable Map<K, V> optionalMap, K key) {
    return Optional.ofNullable(optionalMap).map(map -> checkNotNull(map).remove(key));
  }

  /**
   * Cast, if present, the object wrapped inside an {@link Optional}.
   * 
   * @param optionalObject
   *          the wrapped object
   * @return the casted object wrapped in a Optional
   */
  @SuppressWarnings("unchecked") // no escape from Java erasure
  public static <S> Optional<S> optionalCast(@Nullable Object optionalObject) {
    Optional<?> optional = optionalObject instanceof Optional ? (Optional<?>) optionalObject
        : Optional.ofNullable(optionalObject);
    return optional.map(object -> (S) object);
  }

  public static <E> Stream<E> spliteratorToStream(Spliterator<E> spliterator, boolean parallel) {
    return StreamSupport.stream(spliterator, parallel);
  }

  public static <E> Stream<E> spliteratorToStream(Spliterator<E> spliterator) {
    return spliteratorToStream(spliterator, false);
  }

  public static <E> Stream<E> iteratorToStream(Iterator<E> iterator) {
    Iterable<E> iterable = () -> iterator;
    return spliteratorToStream(iterable.spliterator(), false);
  }

  /**
   * Check if there is a HTTP request associated to the current thread or not.
   * 
   * @return true if inside of a HTTP request, false otherwise
   */
  public static boolean isInHttpRequest() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .filter(ServletRequestAttributes.class::isInstance)
        .map(ServletRequestAttributes.class::cast)
        .map(ServletRequestAttributes::getRequest)
        .isPresent();
  }
}
