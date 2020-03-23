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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

public class WorkflowListIteratorTest {

  private static final WorkflowListIterator<String> EMPTY_LIST = new WorkflowListIterator<String>(
      new ArrayList<>()) {
  };

  private static final WorkflowListIterator<String> ONE_ELEMENT = new WorkflowListIterator<String>(
      Lists.newArrayList("one")) {
  };
  private static final WorkflowListIterator<String> TWO_ELEMENTS = new WorkflowListIterator<String>(
      Lists.newArrayList("one", "two")) {
  };

  @Before
  public void init() {
    EMPTY_LIST.reset();
    ONE_ELEMENT.reset();
    TWO_ELEMENTS.reset();
  }

  @Test
  public void testGetSize() {
    assertThat(TWO_ELEMENTS.getSize()).isEqualTo(2);
  }

  @Test
  public void testReset() {
    WorkflowListIterator<String> it = TWO_ELEMENTS;
    assertThat(it.currentIndex()).isEqualTo(-1);
    it.next();
    assertThat(it.currentIndex()).isEqualTo(0);
    it.reset();
    assertThat(it.currentIndex()).isEqualTo(-1);
  }

  @Test
  public void testCurrentIndex() {
    WorkflowListIterator<String> it = ONE_ELEMENT;
    assertThat(it.currentIndex()).isEqualTo(-1);
    it.next();
    assertThat(it.currentIndex()).isEqualTo(0);
  }

  @Test
  public void testHasCurrent() {
    WorkflowListIterator<String> it = ONE_ELEMENT;
    assertThat(it.hasCurrent()).isFalse();
    it.next();
    assertThat(it.hasCurrent()).isTrue();
  }

  @Test
  public void testCurrentFail() {
    assertThatCode(ONE_ELEMENT::current).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  public void testCurrentSuccess() {
    WorkflowListIterator<String> it = ONE_ELEMENT;
    it.next();
    assertThat(it.current()).isEqualTo("one");
  }

  @Test
  public void testNextIndex() {
    WorkflowListIterator<String> it = TWO_ELEMENTS;
    assertThat(it.nextIndex()).isEqualTo(0);
    it.next();
    assertThat(it.nextIndex()).isEqualTo(1);
    it.next();
    assertThat(it.nextIndex()).isEqualTo(2);
  }

  @Test
  public void testHasNext() {
    WorkflowListIterator<String> it = TWO_ELEMENTS;
    assertThat(it.hasNext()).isTrue();
    it.next();
    assertThat(it.hasNext()).isTrue();
    it.next();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void testNextFail() {
    assertThatCode(EMPTY_LIST::next).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  public void testNextSuccess() {
    assertThat(ONE_ELEMENT.next()).isEqualTo("one");
  }

  @Test
  public void testPreviousIndex() {
    WorkflowListIterator<String> it = TWO_ELEMENTS;
    assertThat(it.previousIndex()).isEqualTo(-1);
    it.next();
    assertThat(it.previousIndex()).isEqualTo(-1);
    it.next();
    assertThat(it.previousIndex()).isEqualTo(0);
  }

  @Test
  public void testHasPrevious() {
    WorkflowListIterator<String> it = TWO_ELEMENTS;
    assertThat(it.hasPrevious()).isFalse();
    it.next();
    assertThat(it.hasPrevious()).isFalse();
    it.next();
    assertThat(it.hasPrevious()).isTrue();
  }

  @Test
  public void testPreviousFail() {
    WorkflowListIterator<String> it = ONE_ELEMENT;
    assertThatCode(it::previous).isInstanceOf(NoSuchElementException.class);
    it.next();
    assertThatCode(it::previous).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  public void testPreviousSuccess() {
    WorkflowListIterator<String> it = TWO_ELEMENTS;
    it.next();
    it.next();
    assertThat(it.previous()).isEqualTo("one");
  }

  @Test
  public void testRemove() {
    WorkflowListIterator<String> it = TWO_ELEMENTS;
    it.next();
    assertThatCode(it::remove).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("remove() method is not supported");
  }

  @Test
  public void testSet() {
    WorkflowListIterator<String> it = TWO_ELEMENTS;
    it.next();
    assertThatCode(() -> it.set("three")).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("set() method is not supported");
  }

  @Test
  public void testAdd() {
    WorkflowListIterator<String> it = TWO_ELEMENTS;
    it.next();
    assertThatCode(() -> it.add("three")).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("add() method is not supported");
  }
}
