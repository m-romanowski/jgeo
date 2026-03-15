package dev.marcinromanowski.postal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

class PostalRepositoryWrapper implements PostalRepository {

  private final PostalRepository delegate;

  PostalRepositoryWrapper(PostalRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<PostalLocation> getByZip(String country, String zip) {
    return delegate.getByZip(country, zip);
  }

  @Override
  public PaginatedResponse<String> getZipsByCity(
      String country,
      String city,
      int page,
      int size
  ) {
    try {
      return delegate.getZipsByCity(country, city, page, size);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public PaginatedResponse<String> getZipsByCountry(
      String country,
      int page,
      int size
  ) {
    try {
      return delegate.getZipsByCountry(country, page, size);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public PaginatedResponse<String> searchCity(
      String country,
      String prefix,
      int page,
      int size
  ) {
    try {
      return delegate.searchCity(country, prefix, page, size);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
