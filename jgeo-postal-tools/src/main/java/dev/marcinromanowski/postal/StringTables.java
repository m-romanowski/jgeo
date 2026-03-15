package dev.marcinromanowski.postal;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

record StringTables(
    String[] cities,
    String[] states,
    String[] stateCodes,
    String[] countries,
    Map<String, Integer> countryIdx,
    Map<String, Integer> cityIdx,
    Map<String, Integer> stateIdx
) {

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StringTables that = (StringTables) o;
    return Objects.deepEquals(cities, that.cities)
           && Objects.deepEquals(states, that.states)
           && Objects.deepEquals(stateCodes, that.stateCodes)
           && Objects.deepEquals(countries, that.countries)
           && Objects.equals(cityIdx, that.cityIdx)
           && Objects.equals(stateIdx, that.stateIdx)
           && Objects.equals(countryIdx, that.countryIdx);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        Arrays.hashCode(cities),
        Arrays.hashCode(states),
        Arrays.hashCode(stateCodes),
        Arrays.hashCode(countries),
        countryIdx,
        cityIdx,
        stateIdx
    );
  }

  @Override
  public String toString() {
    return "StringTables{" +
           "cities=" + Arrays.toString(cities) +
           ", states=" + Arrays.toString(states) +
           ", stateCodes=" + Arrays.toString(stateCodes) +
           ", countries=" + Arrays.toString(countries) +
           ", countryIdx=" + countryIdx +
           ", cityIdx=" + cityIdx +
           ", stateIdx=" + stateIdx +
           '}';
  }

}
