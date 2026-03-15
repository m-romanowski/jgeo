package dev.marcinromanowski.postal;

import java.util.Arrays;
import java.util.Objects;

record PostalArrays(
    int[] postalKeys,
    int[] cityIds,
    int[] stateIds,
    String[] zips
) {

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostalArrays that = (PostalArrays) o;
    return Objects.deepEquals(cityIds, that.cityIds)
           && Objects.deepEquals(zips, that.zips)
           && Objects.deepEquals(stateIds, that.stateIds)
           && Objects.deepEquals(postalKeys, that.postalKeys);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        Arrays.hashCode(postalKeys),
        Arrays.hashCode(cityIds),
        Arrays.hashCode(stateIds),
        Arrays.hashCode(zips)
    );
  }

  @Override
  public String toString() {
    return "PostalArrays{" +
           "postalKeys=" + Arrays.toString(postalKeys) +
           ", cityIds=" + Arrays.toString(cityIds) +
           ", stateIds=" + Arrays.toString(stateIds) +
           ", zips=" + Arrays.toString(zips) +
           '}';
  }

}
