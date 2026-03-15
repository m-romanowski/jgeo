package dev.marcinromanowski.postal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@WireMockTest
class GeoNamesIndexBuilderTest {

  private Path tempDir;
  private Path indexFile;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    this.tempDir = tempDir;
    this.indexFile = tempDir.resolve("postal-index-test.bin.gz");
  }

  @Test
  void shouldDownloadAndExtractZip(WireMockRuntimeInfo wm) throws IOException {
    // when
    byte[] zip = buildZip(row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"));
    GeoNamesIndexBuilder builder = builderFor(wm, zip, Set.of("PL"));
    builder.run();

    // then
    assertTrue(Files.exists(indexFile), "Index file should be created");
    verify(1, getRequestedFor(urlEqualTo("/export/zip/allCountries.zip")));
  }

  @Test
  void shouldNotRedownloadIfZipAlreadyExists(WireMockRuntimeInfo wm) throws IOException {
    // when
    // write zip manually, so it's already present
    byte[] zip = buildZip(row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"));
    Files.write(tempDir.resolve("allCountries.zip"), zip);

    GeoNamesIndexBuilder builder = builderFor(wm, zip, Set.of("PL"));
    builder.run();

    // then
    verify(0, getRequestedFor(urlEqualTo("/export/zip/allCountries.zip")));
  }

  @Test
  void shouldFailGracefullyOnHttpError(WireMockRuntimeInfo wm) {
    // given
    stubFor(
        get(urlEqualTo("/export/zip/allCountries.zip"))
            .willReturn(
                aResponse()
                    .withStatus(503)
            )
    );

    // when
    GeoNamesIndexBuilder builder = GeoNamesIndexBuilder.builder()
        .downloadUrl(wm.getHttpBaseUrl() + "/export/zip/allCountries.zip")
        .dataDir(tempDir.toString())
        .indexFile(indexFile.toString())
        .build();

    // then
    assertThrows(IOException.class, builder::run);
  }

  @Test
  void shouldFilterByCountry(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"),
        row("DE", "10115", "Berlin", "Berlin"),
        row("FR", "75001", "Paris", "Île-de-France")
    );

    // expect
    assertThat(lookup.getByZip("PL", "87-100")).hasSize(1);
    assertThat(lookup.getByZip("DE", "10115")).isEmpty();
    assertThat(lookup.getByZip("FR", "75001")).isEmpty();
  }

  @Test
  void shouldIncludeAllCountriesWhenFilterIsNull(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, null,
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"),
        row("DE", "10115", "Berlin", "Berlin"),
        row("FR", "75001", "Paris", "Île-de-France")
    );

    // expect
    assertThat(lookup.getByZip("PL", "87-100")).hasSize(1);
    assertThat(lookup.getByZip("DE", "10115")).hasSize(1);
    assertThat(lookup.getByZip("FR", "75001")).hasSize(1);
  }

  @Test
  void shouldIncludeAllCountriesInEurope(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("EU"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"),
        row("DE", "10115", "Berlin", "Berlin"),
        row("AU", "2000", "Sydney", "NSW"),
        row("FR", "75001", "Paris", "Île-de-France"),
        row("US", "10001", "New York", "NY"),
        row("CA", "M5V 2T6", "Toronto", "ON")
    );

    // expect
    assertThat(lookup.getByZip("PL", "87-100")).hasSize(1);
    assertThat(lookup.getByZip("DE", "10115")).hasSize(1);
    assertThat(lookup.getByZip("FR", "75001")).hasSize(1);
    assertThat(lookup.getByZip("AU", "2000")).isEmpty();
    assertThat(lookup.getByZip("US", "10001")).isEmpty();
    assertThat(lookup.getByZip("CA", "M5V 2T6")).isEmpty();
  }

  @Test
  void shouldHandleOptionalStateCodes(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL", "IT"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"),
        row("IT", "00010", "Setteville Di Guidonia", "Roma", "RM"),
        row("IT", "20010", "Pogliano Milanese", "Milano", "MI")
    );

    // expect
    assertThat(lookup.getByZip("PL", "87-100"))
        .hasSize(1)
        .first()
        .satisfies(location -> {
          assertThat(location.city())
              .isEqualTo("Toruń");
          assertThat(location.state())
              .isEqualTo("Kujawsko-Pomorskie");
          assertThat(location.stateCode())
              .isNull();
        });

    assertThat(lookup.getByZip("IT", "00010"))
        .hasSize(1)
        .first()
        .satisfies(location -> {
          assertThat(location.city())
              .isEqualTo("Setteville Di Guidonia");
          assertThat(location.state())
              .isEqualTo("Roma");
          assertThat(location.stateCode())
              .isEqualTo("RM");
        });
  }

  @Test
  void shouldLookupByZip(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie")
    );

    // when
    List<PostalLocation> result = lookup.getByZip("PL", "87-100");

    // expect
    assertThat(result)
        .hasSize(1);
    assertThat(result.getFirst().city())
        .isEqualTo("Toruń");
    assertThat(result.getFirst().state())
        .isEqualTo("Kujawsko-Pomorskie");
  }

  @Test
  void shouldReturnEmptyForUnknownZip(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie")
    );

    // expect
    assertThat(lookup.getByZip("PL", "99-999"))
        .isEmpty();
  }

  @Test
  void shouldReturnEmptyForUnknownCountry(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie")
    );

    // expect
    assertThat(lookup.getByZip("XX", "87-100"))
        .isEmpty();
  }

  @Test
  void shouldReturnMultipleSettlementsForSameZip(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "22-170", "Rejowiec Fabryczny", "Lublin"),
        row("PL", "22-170", "Toruń", "Lublin"),  // hamlet, same zip
        row("PL", "22-170", "Kanie", "Lublin")
    );

    // expect
    assertThat(lookup.getByZip("PL", "22-170"))
        .hasSize(3)
        .extracting(PostalLocation::city)
        .containsExactlyInAnyOrder("Rejowiec Fabryczny", "Toruń", "Kanie");
  }

  @Test
  void shouldLookupZipsByCity(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"),
        row("PL", "87-101", "Toruń", "Kujawsko-Pomorskie"),
        row("PL", "87-102", "Toruń", "Kujawsko-Pomorskie")
    );

    // expect
    assertThat(lookup.getZipsByCity("PL", "TORUŃ", 1, 10).data())
        .containsExactlyInAnyOrder("87-100", "87-101", "87-102");
  }

  @Test
  void shouldReturnZipsFromAllHomonymousSettlements(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    // two places named "Toruń" in different provinces - both zips should be returned
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"),
        row("PL", "22-170", "Toruń", "Lublin")
    );

    // expect
    assertThat(lookup.getZipsByCity("PL", "TORUŃ", 1, 10).data())
        .containsExactlyInAnyOrder("87-100", "22-170");
  }

  @Test
  void shouldBeCaseInsensitiveForCityLookup(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie")
    );

    // expect
    assertThat(lookup.getZipsByCity("PL", "toruń", 1, 10).data())
        .isNotEmpty();
    assertThat(lookup.getZipsByCity("PL", "TORUŃ", 1, 10).data())
        .isNotEmpty();
    assertThat(lookup.getZipsByCity("PL", "Toruń", 1, 10).data())
        .isNotEmpty();
  }

  @Test
  void shouldReturnEmptyForUnknownCity(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie")
    );

    // expect
    assertThat(lookup.getZipsByCity("PL", "GDAŃSK", 1, 10).data())
        .isEmpty();
  }

  @Test
  void shouldSearchCityByPrefix(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"),
        row("PL", "87-200", "Toru", "Kujawsko-Pomorskie"), // shorter prefix match
        row("PL", "80-001", "Gdańsk", "Pomorskie"),
        row("PL", "61-001", "Poznań", "Wielkopolskie")
    );

    // expect
    assertThat(lookup.searchCity("PL", "Tor", 1, 10).data())
        .containsExactlyInAnyOrder("Toruń", "Toru")
        .doesNotContain("Gdańsk", "Poznań");
  }

  @Test
  void shouldPaginateSearchResults(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"),
        row("PL", "87-200", "Torunek", "Kujawsko-Pomorskie"),
        row("PL", "87-300", "Toruszew", "Kujawsko-Pomorskie")
    );

    // expect
    List<String> page1 = lookup.searchCity("PL", "Tor", 1, 2)
        .data();
    List<String> page2 = lookup.searchCity("PL", "Tor", 2, 2)
        .data();

    assertThat(page1)
        .hasSize(2);
    assertThat(page2)
        .hasSize(1);
    assertThat(page1)
        .doesNotContainAnyElementsOf(page2);
  }

  @Test
  void shouldHandlePolishDiacritics(WireMockRuntimeInfo wm) throws IOException, ClassNotFoundException {
    // given
    PostalRepository lookup = buildAndLoad(wm, Set.of("PL"),
        row("PL", "87-100", "Toruń", "Kujawsko-Pomorskie"),
        row("PL", "80-001", "Gdańsk", "Pomorskie"),
        row("PL", "31-001", "Kraków", "Małopolskie"),
        row("PL", "60-001", "Łódź", "Łódź Province"),
        row("PL", "50-001", "Wrocław", "Dolnośląskie")
    );

    // expect
    assertThat(lookup.getZipsByCity("PL", "TORUŃ", 1, 10).data())
        .isNotEmpty();
    assertThat(lookup.getZipsByCity("PL", "GDAŃSK", 1, 10).data())
        .isNotEmpty();
    assertThat(lookup.getZipsByCity("PL", "KRAKÓW", 1, 10).data())
        .isNotEmpty();
    assertThat(lookup.getZipsByCity("PL", "ŁÓDŹ", 1, 10).data())
        .isNotEmpty();
    assertThat(lookup.getZipsByCity("PL", "WROCŁAW", 1, 10).data())
        .isNotEmpty();
  }

  private byte[] buildZip(String... tsvRows) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      zos.putNextEntry(new ZipEntry("allCountries.txt"));
      for (String row : tsvRows) {
        zos.write((row + "\n").getBytes(StandardCharsets.UTF_8));
      }
      zos.closeEntry();
    }
    return baos.toByteArray();
  }

  private String row(String country, String postal, String city, String state) {
    return row(country, postal, city, state, "");
  }

  private String row(String country, String postal, String city, String state, String stateCode) {
    return String.join("\t", country, postal, city, state, stateCode, "", "", "", "", "0.0", "0.0", "6");
  }

  private GeoNamesIndexBuilder builderFor(WireMockRuntimeInfo wm, byte[] zip, Set<String> filter) {
    stubFor(
        get(urlEqualTo("/export/zip/allCountries.zip"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/zip")
                    .withBody(zip)
            )
    );
    return GeoNamesIndexBuilder.builder()
        .downloadUrl(wm.getHttpBaseUrl() + "/export/zip/allCountries.zip")
        .dataDir(tempDir.toString())
        .indexFile(indexFile.toString())
        .countryFilter(filter)
        .build();
  }

  private PostalRepository buildAndLoad(
      WireMockRuntimeInfo wm,
      Set<String> filter,
      String... rows
  ) throws IOException, ClassNotFoundException {
    byte[] zip = buildZip(rows);
    GeoNamesIndexBuilder builder = builderFor(wm, zip, filter);
    builder.run();
    return PostalRepositoryFactory.fileIndex(indexFile.toString());
  }

}
