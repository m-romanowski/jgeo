package dev.marcinromanowski.postal;

import java.util.Arrays;
import java.util.Objects;

record SortedData(
    int[] sortedKeys,
    int[] sortedCityIds,
    int[] sortedStateIds,
    String[] sortedZips,
    int[] runStart,
    int[] runEnd
) {

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SortedData that = (SortedData) o;
    return Objects.deepEquals(runEnd, that.runEnd)
           && Objects.deepEquals(runStart, that.runStart)
           && Objects.deepEquals(sortedKeys, that.sortedKeys)
           && Objects.deepEquals(sortedCityIds, that.sortedCityIds)
           && Objects.deepEquals(sortedZips, that.sortedZips)
           && Objects.deepEquals(sortedStateIds, that.sortedStateIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        Arrays.hashCode(sortedKeys),
        Arrays.hashCode(sortedCityIds),
        Arrays.hashCode(sortedStateIds),
        Arrays.hashCode(sortedZips),
        Arrays.hashCode(runStart),
        Arrays.hashCode(runEnd)
    );
  }

  @Override
  public String toString() {
    return "SortedData{" +
           "sortedKeys=" + Arrays.toString(sortedKeys) +
           ", sortedCityIds=" + Arrays.toString(sortedCityIds) +
           ", sortedStateIds=" + Arrays.toString(sortedStateIds) +
           ", sortedZips=" + Arrays.toString(sortedZips) +
           ", runStart=" + Arrays.toString(runStart) +
           ", runEnd=" + Arrays.toString(runEnd) +
           '}';
  }

}
