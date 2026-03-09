package dev.marcinromanowski.postal;

import dev.marcinromanowski.postal.dto.PaginatedResponse;
import dev.marcinromanowski.postal.dto.PostalLocation;
import java.io.IOException;
import java.util.List;

public interface PostalRepository {

  List<PostalLocation> getByZip(String country, String zip);

  PaginatedResponse<String> getZipsByCity(
      String country,
      String city,
      int page,
      int size
  ) throws IOException;

  PaginatedResponse<String> getZipsByCountry(
      String country,
      int page,
      int size
  ) throws IOException;

  PaginatedResponse<String> searchCity(
      String country,
      String prefix,
      int page,
      int size
  ) throws IOException;

}
