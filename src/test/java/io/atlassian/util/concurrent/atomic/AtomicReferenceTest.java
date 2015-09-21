package io.atlassian.util.concurrent.atomic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.function.Function;
import java.util.function.Supplier;

public class AtomicReferenceTest {
  @Test public void getAndSetIfNormal() {
    final String from = "from";
    final String to = "to";
    final AtomicReference<String> ref = new AtomicReference<String>(from);
    assertEquals(to, ref.getOrSetAndGetIf(from, () -> to));
  }

  @Test public void getAndSetIfNormalValue() {
    final String from = "from";
    final String to = "to";
    final AtomicReference<String> ref = new AtomicReference<String>(from);
    assertEquals(to, ref.getOrSetAndGetIf(from, to));
  }

  @Test public void getAndSetIfNull() {
    final String to = "to";
    final AtomicReference<String> ref = new AtomicReference<String>();
    assertEquals(to, Atomics.getAndSetIfNull(ref, (Supplier<String>) () -> to));
  }

  @Test public void getAndSetContended() {
    final String from = "from";
    final String to = "to";
    final String different = "different";
    final AtomicReference<String> ref = new AtomicReference<String>(from);
    assertEquals(different, ref.getOrSetAndGetIf(from, () -> {
      // being called, set the reference so the CAS fails
      ref.set(different);
      return to;
    }));
  }

  @Test public void getAndSetReturnsOldValueIfChanged() {
    final String old = "old";
    final String from = "from";
    final String to = "to";
    final AtomicReference<String> ref = new AtomicReference<String>(old);
    assertEquals(old, ref.getOrSetAndGetIf(from, () -> to));
  }

  @Test public void getAndSetSameValue() {
    final String from = "from";
    final String to = from;
    final AtomicReference<String> ref = new AtomicReference<String>(from);
    assertEquals(to, ref.getOrSetAndGetIf(from, () -> to));
  }

  @Test public void getAndSetSameValueDifferent() {
    final String from = "from";
    final String to = from;
    final String different = "blah";
    final AtomicReference<String> ref = new AtomicReference<String>(different);
    assertEquals(different, ref.getOrSetAndGetIf(from, () -> to));
  }

  @Test public void updateNormal() {
    final String from = "from";
    final String to = "to";
    final AtomicReference<String> ref = new AtomicReference<String>(from);
    final Function<String, String> newValueFactory = input -> to;
    assertEquals(to, ref.update(newValueFactory));
  }

  @Test public void updateContended() {
    final Integer from = 0;
    final Integer to = 10;
    final AtomicReference<Integer> ref = new AtomicReference<Integer>(from);
    assertEquals(to, ref.update(new Function<Integer, Integer>() {
      int x = from;

      public Integer apply(final Integer input) {
        if (x < to) {
          ref.set(++x);
        }
        return x;
      }
    }));
  }
}
