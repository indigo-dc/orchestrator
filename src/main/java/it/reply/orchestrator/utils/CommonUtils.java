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

package it.reply.orchestrator.utils;

import com.google.common.base.Equivalence;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Primitives;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.experimental.UtilityClass;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@UtilityClass
public class CommonUtils {

  private static final Set<Class<?>> PRIMITIVE_CLASSES = ImmutableSet.of(
      String.class,
      Boolean.class,
      Byte.class,
      Character.class,
      Double.class,
      Float.class,
      Integer.class,
      Long.class,
      Short.class
  );

  /**
   * Verify that a <code>@Nullable</code> reference is effectively non null and cast it to a
   * <code>@NonNull</code> reference. If the reference is instead null, a NPE is thrown
   *
   * @param <T> The type of object
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
   * @param <T> The type of object
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

  /**
   * Verify that a <code>@Nullable</code> reference is effectively non null and cast it to a
   * <code>@NonNull</code> reference. If the reference is instead null, the <code>@NonNull</code> a
   * default value will be generated through the provided supplier and returned.
   *
   * @param <T> The type of object
   * @param reference
   *          the <code>@Nullable</code> reference
   * @param defaultValueSupplier
   *          the <code>@NonNull</code> default value supplier
   * @return the <code>@NonNull</code> reference
   */
  @NonNull
  public static <T> T notNullOrDefaultValue(@Nullable T reference,
      Supplier<@NonNull T> defaultValueSupplier) {
    if (reference == null) {
      return checkNotNull(defaultValueSupplier.get());
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
   * @param <S> The type of object
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

  public static <E> Stream<E> spliteratorToStream(Spliterator<E> spliterator) {
    return StreamSupport.stream(spliterator, false);
  }

  /**
   * Generate a <b>sequential</b> and <b>ordered</b> {@link Stream} from the remaining element of a
   * {@link Iterator}.
   *
   * @param <E> the thrown exception type
   * @param iterator
   *          the iterator from which generate the stream
   * @return the stream
   */
  public static <E> Stream<E> iteratorToStream(Iterator<E> iterator) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
        false);
  }

  public static <E> Stream<E> nullableCollectionToStream(Collection<E> collection) {
    return collection == null ? Stream.empty() : collection.stream();
  }

  /**
   * Check if there is a HTTP request associated to the current thread or not.
   *
   * @return true if inside of a HTTP request, false otherwise
   */
  public static boolean isInHttpRequest() {
    return Optional
        .ofNullable(RequestContextHolder.getRequestAttributes())
        .filter(ServletRequestAttributes.class::isInstance)
        .map(ServletRequestAttributes.class::cast)
        .map(ServletRequestAttributes::getRequest)
        .isPresent();
  }

  /**
   * Filter a stream removing all the duplicate elements. The uniqueness is evaluated as
   * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt> on the result of the provided
   * mapping function.
   *
   * @param <T> The type of return object stream
   * @param <V> The type of the object to evaluate
   * @param stream
   *          the stream to filter
   * @param mapper
   *          the mapping function on which evaluate the uniqueness
   * @return the filtered stream
   */
  public static <T, V> Stream<T> distinct(Stream<T> stream, Function<T, V> mapper) {
    Equivalence<T> wrapper = Equivalence.equals().onResultOf(mapper::apply);
    return stream
        .map(wrapper::wrap)
        .distinct()
        .map(Equivalence.Wrapper::get);
  }

  /**
   * Returns a {@code Collector} that accumulates elements into a {@code Map} whose keys and values
   * are the result of applying the provided mapping functions to the input elements.
   *
   * <p>
   * If the mapped keys contains duplicates (according to {@link Object#equals(Object)}), an
   * {@code IllegalStateException} is thrown when the collection operation is performed.
   * </p>
   *
   * @param <T> the type of object
   * @param <K> the type of object
   * @param <U> the type of object
   * @param keyMapper
   *          a mapping function to produce keys
   * @param valueMapper
   *          a mapping function to produce values
   * @return a {@code Collector} which collects elements into a {@code Map} whose keys and values
   *         are the result of applying mapping functions to the input elements
   */
  public static <T, K, U> Collector<T, Map<K, U>, Map<K, U>> toMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper) {

    BiConsumer<Map<K, U>, T> accumulator =
        (map, element) -> map.put(keyMapper.apply(element), valueMapper.apply(element));
    BinaryOperator<Map<K, U>> combiner = (map1, map2) -> {
      map2.forEach((key, value) -> {
        U oldMapping = map1.put(key, value);
        Preconditions.checkState(oldMapping == null, "Duplicate key %s", key);
      });
      return map1;
    };
    return Collector.of(HashMap::new, accumulator, combiner);
  }

  public static boolean isPrimitive(Object o) {
    return o != null && PRIMITIVE_CLASSES.contains(Primitives.wrap(o.getClass()));
  }

}
