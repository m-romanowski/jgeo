package dev.marcinromanowski.postal;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Objects;

class ArraySliceList<T> extends AbstractList<T> {

  private final T[] array;
  private final int start;
  private final int end;

  ArraySliceList(T[] array, int start, int end) {
    if (start < 0 || end > array.length || start > end) {
      throw new IndexOutOfBoundsException();
    }
    this.array = array;
    this.start = start;
    this.end = end;
  }

  @Override
  public T get(int index) {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException();
    }
    return array[start + index];
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ArraySliceList<?> that = (ArraySliceList<?>) o;
    return start == that.start
           && end == that.end
           && Objects.deepEquals(array, that.array);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), Arrays.hashCode(array), start, end);
  }

  @Override
  public int size() {
    return end - start;
  }

}
