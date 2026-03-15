package dev.marcinromanowski.postal;

import static dev.marcinromanowski.postal.TestFixture.indexPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileIndexPostalTest {

  private static final PostalRepository repository;

  static {
    try {
      repository = PostalRepositoryFactory.fileIndex(indexPath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void shouldAllowToGetCityWithStatesByZipCode() {
    // when
    List<PostalLocation> locations = repository.getByZip("PL", "87-100");

    // then
    assertEquals(1, locations.size());

    PostalLocation location = locations.getFirst();
    assertEquals("Toruń", location.city());
    assertEquals("Kujawsko-Pomorskie", location.state());
    assertEquals("73", location.stateCode()); // FIPS code
  }

  @Test
  void shouldAllowToGetZipsByCity() throws IOException {
    // when
    PaginatedResponse<String> response = repository.getZipsByCity("PL", "TORUŃ", 1, 30);

    // then
    assertEquals(1, response.page());
    assertEquals(30, response.size());
    assertEquals(27, response.total());
    assertEquals(27, response.data().size());
  }

  @Test
  void shouldAllowToGetZipsByCityWithPagination() throws IOException {
    // when
    PaginatedResponse<String> response = repository.getZipsByCity("PL", "TORUŃ", 2, 10);

    // then
    assertEquals(2, response.page());
    assertEquals(10, response.size());
    assertEquals(27, response.total());
    assertEquals(10, response.data().size());
  }

  @Test
  void shouldReturnEmptyForNonExistentCity() throws IOException {
    // when
    PaginatedResponse<String> response = repository.getZipsByCity("PL", "NONEXISTENT", 1, 10);

    // then
    assertEquals(0, response.total());
    assertEquals(0, response.data().size());
  }

  @Test
  void shouldAllowToGetZipsByCountry() throws IOException {
    // when
    PaginatedResponse<String> response = repository.getZipsByCountry("PL", 1, 10);

    // then
    assertEquals(1, response.page());
    assertEquals(10, response.size());
    assertEquals(10, response.data().size());
  }

  @Test
  void shouldAllowToGetZipsByCountryWithPagination() throws IOException {
    // when
    PaginatedResponse<String> firstPage = repository.getZipsByCountry("PL", 1, 5);
    PaginatedResponse<String> secondPage = repository.getZipsByCountry("PL", 2, 5);

    // then
    assertEquals(5, firstPage.data().size());
    assertEquals(5, secondPage.data().size());
    assertEquals(firstPage.total(), secondPage.total());
  }

  @Test
  void shouldReturnEmptyForNonExistentCountry() throws IOException {
    // when
    PaginatedResponse<String> response = repository.getZipsByCountry("XX", 1, 10);

    // then
    assertEquals(0, response.total());
    assertEquals(0, response.data().size());
  }

  @Test
  void shouldAllowToSearchCityByPrefix() throws IOException {
    // when
    PaginatedResponse<String> response = repository.searchCity("PL", "TOR", 1, 10);

    // then
    assertEquals(1, response.page());
    assertEquals(10, response.size());
    assertEquals(9, response.data().size());
  }

  @Test
  void shouldSearchCityWithPagination() throws IOException {
    // when
    PaginatedResponse<String> response = repository.searchCity("PL", "T", 1, 5);

    // then
    assertEquals(1, response.page());
    assertEquals(5, response.size());
  }

  @Test
  void shouldReturnEmptyForNonMatchingPrefix() throws IOException {
    // when
    PaginatedResponse<String> response = repository.searchCity("PL", "XYZ", 1, 10);

    // then
    assertEquals(0, response.data().size());
  }

  @Test
  void shouldBeCaseInsensitiveForCitySearch() throws IOException {
    // when
    PaginatedResponse<String> upperCase = repository.searchCity("PL", "TOR", 1, 10);
    PaginatedResponse<String> lowerCase = repository.searchCity("PL", "tor", 1, 10);

    // then
    assertEquals(upperCase.data(), lowerCase.data());
  }

}
