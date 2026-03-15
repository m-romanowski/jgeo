package dev.marcinromanowski.postal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IndexBuilder {

  private static final String DEFAULT_URL = "https://download.geonames.org/export/zip/allCountries.zip";
  private static final String DEFAULT_DATA_DIR = "data";
  private static final Set<String> EUROPE = Set.of(
      "AL", // Albania
      "AD", // Andorra
      "AT", // Austria
      "BY", // Belarus
      "BE", // Belgium
      "BA", // Bosnia and Herzegovina
      "BG", // Bulgaria
      "HR", // Croatia
      "CY", // Cyprus
      "CZ", // Czechia
      "DK", // Denmark
      "EE", // Estonia
      "FI", // Finland
      "FR", // France
      "DE", // Germany
      "GR", // Greece
      "VA", // Holy See (Vatican City)
      "HU", // Hungary
      "IS", // Iceland
      "IE", // Ireland
      "IT", // Italy
      "LV", // Latvia
      "LI", // Liechtenstein
      "LT", // Lithuania
      "LU", // Luxembourg
      "MT", // Malta
      "MD", // Moldova
      "MC", // Monaco
      "ME", // Montenegro
      "NL", // Netherlands
      "MK", // North Macedonia
      "NO", // Norway
      "PL", // Poland
      "PT", // Portugal
      "RO", // Romania
      "RU", // Russia
      "SM", // San Marino
      "RS", // Serbia
      "SK", // Slovakia
      "SI", // Slovenia
      "ES", // Spain
      "SE", // Sweden
      "CH", // Switzerland
      "TR", // Turkey
      "UA", // Ukraine
      "GB"  // United Kingdom
  );

  String downloadUrl = DEFAULT_URL;
  String dataDir = DEFAULT_DATA_DIR;
  String indexFile = null;
  Set<String> countryFilter = null;

  public IndexBuilder downloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
    return this;
  }

  public IndexBuilder dataDir(String dataDir) {
    this.dataDir = dataDir;
    return this;
  }

  public IndexBuilder indexFile(String indexFile) {
    this.indexFile = indexFile;
    return this;
  }

  public IndexBuilder countryFilter(Set<String> filter) {
    if (filter != null && filter.stream().anyMatch(code -> code.equalsIgnoreCase("EU"))) {
      Set<String> newFilters = new HashSet<>(filter);
      newFilters.removeIf("EU"::equalsIgnoreCase);
      newFilters.addAll(EUROPE);
      this.countryFilter = Collections.unmodifiableSet(newFilters);
    } else {
      this.countryFilter = filter;
    }
    return this;
  }

  public GeoNamesIndexBuilder build() {
    if (indexFile == null) {
      indexFile = buildIndexName();
    }
    return new GeoNamesIndexBuilder(this);
  }

  private String buildIndexName() {
    if (countryFilter == null) {
      return "postal-index-all.bin.gz";
    }

    if (countryFilter.size() == 1) {
      String suffix = countryFilter.iterator()
          .next()
          .toLowerCase();
      return "postal-index-" + suffix + ".bin.gz";
    }

    return "postal-index-custom.bin.gz";
  }

}
