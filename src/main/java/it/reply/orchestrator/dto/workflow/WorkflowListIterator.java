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

package it.reply.orchestrator.dto.workflow;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.apache.commons.collections4.ResettableListIterator;
import org.checkerframework.checker.nullness.qual.NonNull;

@JsonAutoDetect(
    fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode
public abstract class WorkflowListIterator<T> implements ResettableListIterator<T> {

  private static final int INITIAL_INDEX = -1;

  @NonNull
  @NotNull
  private List<T> items = new ArrayList<>();

  private int currentIndex = INITIAL_INDEX;

  public WorkflowListIterator(@NonNull List<T> items) {
    this.items = items;
  }

  /**
   * Returns the size of the list backing the iterator.
   *
   * @return the size of the list backing the iterator
   */
  public int getSize() {
    return items.size();
  }

  @Override
  public void reset() {
    currentIndex = INITIAL_INDEX;
  }

  /**
   * Returns the index of the element that would be returned by a subsequent call to {@link
   * #current}.(Returns -1 if the list iterator have never been traversed in the forward direction
   * at least one time.)
   *
   * @return the index of the element that would be returned by a subsequent call to {@code current}
   */
  public int currentIndex() {
    return currentIndex;
  }

  /**
   * Returns {@code true} if this list iterator has been traversed one time in the forward
   * direction. (In other words, returns {@code true} if {@link #next} have been called at least one
   * time.)
   *
   * @return {@code true} if the list iterator have been traversed at least one time in the forward
   *      direction
   */
  public boolean hasCurrent() {
    return indexInValidRange(currentIndex());
  }

  /**
   * Returns the current element in the list. Repeated calls of this method will return the same
   * element repeatedly.)
   *
   * @return the current element in the list
   * @throws NoSuchElementException
   *     if the iteration has no current element
   */
  public T current() {
    if (!hasCurrent()) {
      throw new NoSuchElementException();
    } else {
      return items.get(currentIndex());
    }
  }

  @Override
  public int nextIndex() {
    return Math.min(currentIndex + 1, items.size());
  }

  @Override
  public boolean hasNext() {
    return indexInValidRange(nextIndex());
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    } else {
      currentIndex = nextIndex();
      return items.get(currentIndex());
    }
  }

  @Override
  public int previousIndex() {
    return Math.max(currentIndex - 1, INITIAL_INDEX);
  }

  @Override
  public boolean hasPrevious() {
    return indexInValidRange(previousIndex());
  }

  @Override
  public T previous() {
    if (!hasPrevious()) {
      throw new NoSuchElementException();
    } else {
      currentIndex = previousIndex();
      return items.get(currentIndex());
    }
  }

  private boolean indexInValidRange(int index) {
    return index > INITIAL_INDEX && index < items.size();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove() method is not supported");
  }

  @Override
  public void set(T s) {
    throw new UnsupportedOperationException("set() method is not supported");
  }

  @Override
  public void add(T s) {
    throw new UnsupportedOperationException("add() method is not supported");
  }

}
