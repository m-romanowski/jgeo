package dev.marcinromanowski.postal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ArraySliceListTest {

  @Test
  void shouldReturnCorrectSize() {
    // given
    String[] array = {"a", "b", "c", "d", "e"};
    ArraySliceList<String> slice = new ArraySliceList<>(array, 1, 4);

    // expect
    assertEquals(3, slice.size());
  }

  @Test
  void shouldReturnCorrectElements() {
    // given
    String[] array = {"a", "b", "c", "d", "e"};
    ArraySliceList<String> slice = new ArraySliceList<>(array, 1, 4);

    // expect
    assertEquals("b", slice.get(0));
    assertEquals("c", slice.get(1));
    assertEquals("d", slice.get(2));
  }

  @Test
  void shouldThrowExceptionForInvalidIndex() {
    // given
    String[] array = {"a", "b", "c"};
    ArraySliceList<String> slice = new ArraySliceList<>(array, 0, 2);

    // expect
    assertThrows(IndexOutOfBoundsException.class, () -> slice.get(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> slice.get(2));
    assertThrows(IndexOutOfBoundsException.class, () -> slice.get(10));
  }

  @Test
  void shouldThrowExceptionForInvalidRange() {
    // given
    String[] array = {"a", "b", "c"};

    // expect
    assertThrows(IndexOutOfBoundsException.class, () -> new ArraySliceList<>(array, -1, 2));
    assertThrows(IndexOutOfBoundsException.class, () -> new ArraySliceList<>(array, 0, 4));
    assertThrows(IndexOutOfBoundsException.class, () -> new ArraySliceList<>(array, 2, 1));
  }

  @Test
  void shouldHandleEmptySlice() {
    // given
    String[] array = {"a", "b", "c"};
    ArraySliceList<String> slice = new ArraySliceList<>(array, 1, 1);

    // expect
    assertEquals(0, slice.size());
  }

  @Test
  void shouldHandleFullArray() {
    // given
    String[] array = {"a", "b", "c"};
    ArraySliceList<String> slice = new ArraySliceList<>(array, 0, 3);

    // expect
    assertEquals(3, slice.size());
    assertEquals("a", slice.get(0));
    assertEquals("b", slice.get(1));
    assertEquals("c", slice.get(2));
  }

}
