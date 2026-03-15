package dev.marcinromanowski.postal;

import java.util.Arrays;
import java.util.Objects;

record CityRanges(
    int[] slotIndex,
    int[] start,
    int[] end
) {

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CityRanges that = (CityRanges) o;
    return Objects.deepEquals(end, that.end)
           && Objects.deepEquals(start, that.start)
           && Objects.deepEquals(slotIndex, that.slotIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        Arrays.hashCode(slotIndex),
        Arrays.hashCode(start),
        Arrays.hashCode(end)
    );
  }

  @Override
  public String toString() {
    return "CityRanges{" +
           "slotIndex=" + Arrays.toString(slotIndex) +
           ", start=" + Arrays.toString(start) +
           ", end=" + Arrays.toString(end) +
           '}';
  }

}
