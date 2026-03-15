package dev.marcinromanowski.postal;

import java.util.Arrays;
import java.util.Objects;

record CountryRanges(
    int[] start,
    int[] end
) {

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CountryRanges that = (CountryRanges) o;
    return Objects.deepEquals(end, that.end)
           && Objects.deepEquals(start, that.start);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        Arrays.hashCode(start),
        Arrays.hashCode(end)
    );
  }

  @Override
  public String toString() {
    return "CountryRanges{" +
           "start=" + Arrays.toString(start) +
           ", end=" + Arrays.toString(end) +
           '}';
  }

}
