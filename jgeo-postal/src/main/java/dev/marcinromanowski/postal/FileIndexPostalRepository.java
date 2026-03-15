package dev.marcinromanowski.postal;

import java.io.IOException;
import java.util.List;

class FileIndexPostalRepository implements PostalRepository {

  private final PostalLookup lookup;

  FileIndexPostalRepository(String filePath) throws IOException, ClassNotFoundException {
    this(PostalIndexLoader.load(filePath));
  }

  FileIndexPostalRepository(PostalIndexLoader loader) {
    this.lookup = new PostalLookup(loader);
  }

  @Override
  public List<PostalLocation> getByZip(String country, String zip) {
    return lookup.lookupByZip(country, zip);
  }

  @Override
  public PaginatedResponse<String> getZipsByCity(
      String country,
      String city,
      int page,
      int size
  ) throws IOException {
    String[] allZips = lookup.lookupByCityArray(country, city);
    return paginateArray(allZips, page, size);
  }

  @Override
  public PaginatedResponse<String> getZipsByCountry(
      String country,
      int page,
      int size
  ) throws IOException {
    String[] allZips = lookup.lookupByCountryArray(country);
    return paginateArray(allZips, page, size);
  }

  @Override
  public PaginatedResponse<String> searchCity(
      String country,
      String prefix,
      int page,
      int size
  ) throws IOException {
    List<String> matches = lookup.searchCity(country, prefix, page, size);
    return new PaginatedResponse<>(matches, page, size, matches.size());
  }

  private <T> PaginatedResponse<T> paginateArray(T[] array, int page, int size) {
    int total = array.length;
    int from = Math.min((page - 1) * size, total);
    int to = Math.min(from + size, total);
    List<T> pageData = new ArraySliceList<>(array, from, to);
    return new PaginatedResponse<>(pageData, page, size, total);
  }

}

