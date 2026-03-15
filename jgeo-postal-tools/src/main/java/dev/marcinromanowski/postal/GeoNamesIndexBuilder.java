package dev.marcinromanowski.postal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.lucene.store.ByteBuffersDataOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FSTCompiler;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoNamesIndexBuilder {

  private static final Logger log = LoggerFactory.getLogger(GeoNamesIndexBuilder.class);
  private static final String ALL_COUNTRIES_ZIP = "allCountries.zip";
  private static final String ALL_COUNTRIES_TXT = "allCountries.txt";

  private final String downloadUrl;
  private final String dataDir;
  private final String indexFile;
  private final Set<String> countryFilter;

  public GeoNamesIndexBuilder(IndexBuilder builder) {
    this.downloadUrl = builder.downloadUrl;
    this.dataDir = builder.dataDir;
    this.indexFile = builder.indexFile;
    this.countryFilter = builder.countryFilter;
  }

  public static IndexBuilder builder() {
    return new IndexBuilder();
  }

  public void run() throws IOException {
    Files.createDirectories(Paths.get(dataDir));
    downloadDataset();
    extractDataset();

    List<String[]> raw = readTsv();
    log.info("Loaded {} rows {}",
        raw.size(), (countryFilter != null ? "for country: " + countryFilter : "for all countries"));

    StringTables tables = buildStringTables(raw);
    PostalArrays postal = buildPostalArrays(raw, tables);

    log.info("Sorting and computing offsets...");
    SortedData sorted = sortAndComputeRuns(postal);
    CountryRanges ctyRanges = buildCountryRanges(sorted, tables.countries().length);
    CityRanges cityRanges = buildCityRanges(sorted, tables.cities().length);

    log.info("Building FSTs...");
    FSTBundle fsts = buildFSTs(raw, tables);
    int[] cityStateIds = packCityStateIds(sorted);

    log.info("Writing index to {}...", indexFile);
    writeIndex(tables, sorted, ctyRanges, cityRanges, fsts, cityStateIds);

    log.info("Done");
  }

  private StringTables buildStringTables(List<String[]> raw) {
    Map<String, Integer> countryIdx = new LinkedHashMap<>();
    Map<String, Integer> cityIdx = new LinkedHashMap<>();
    Map<Integer, String> cityOrdToOriginal = new HashMap<>();
    Map<String, Integer> stateIdx = new LinkedHashMap<>();
    Map<Integer, String> stateOrdToCode = new HashMap<>(); // ordinal -> code (may be blank)

    for (String[] f : raw) {
      countryIdx.putIfAbsent(f[0].toUpperCase(), countryIdx.size());

      String cityUpper = f[2].toUpperCase();
      int sizeBefore = cityIdx.size();
      cityIdx.putIfAbsent(cityUpper, sizeBefore);
      int ordinal = cityIdx.get(cityUpper);
      cityOrdToOriginal.putIfAbsent(ordinal, f[2]);

      int stateOrd = stateIdx.computeIfAbsent(f[3], ignored -> stateIdx.size());
      // f[4] is admin_code1 - take the first non-blank value seen for this state
      if (f.length > 4 && !f[4].isBlank()) {
        stateOrdToCode.putIfAbsent(stateOrd, f[4]);
      }
    }

    String[] cities = new String[cityIdx.size()];
    String[] states = new String[stateIdx.size()];
    String[] stateCodes = new String[stateIdx.size()]; // null = absent
    String[] countries = new String[countryIdx.size()];

    cityOrdToOriginal.forEach((ord, original) -> cities[ord] = original);
    stateIdx.forEach((name, ord) -> states[ord] = name);
    stateOrdToCode.forEach((ord, code) -> stateCodes[ord] = code);
    countryIdx.forEach((name, ord) -> countries[ord] = name);

    verifyNoNulls(cities, "city");
    verifyNoNulls(states, "state");

    return new StringTables(cities, states, stateCodes, countries, countryIdx, cityIdx, stateIdx);
  }

  private static void verifyNoNulls(String[] arr, String label) {
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == null) {
        throw new IllegalStateException("Null " + label + " at ordinal " + i);
      }
    }
  }

  private PostalArrays buildPostalArrays(List<String[]> raw, StringTables t) {
    int n = raw.size();
    int[] postalKeys = new int[n];
    int[] cityIds = new int[n];
    int[] stateIds = new int[n];
    String[] zips = new String[n];

    Map<String, String> zipIntern = new HashMap<>();

    for (int i = 0; i < n; i++) {
      String[] f = raw.get(i);
      int ctry = t.countryIdx().get(f[0].toUpperCase());
      int zipHash = f[1].hashCode() & 0xFFFFFF; // 24-bit hash; exact match verified via zips[]
      postalKeys[i] = (ctry << 24) | zipHash;
      cityIds[i] = t.cityIdx().get(f[2].toUpperCase());
      stateIds[i] = t.stateIdx().get(f[3]);
      zips[i] = zipIntern.computeIfAbsent(f[1], k -> k);
    }

    return new PostalArrays(postalKeys, cityIds, stateIds, zips);
  }

  private SortedData sortAndComputeRuns(PostalArrays p) {
    int n = p.postalKeys().length;
    Integer[] sortedRowIdx = new Integer[n];

    for (int i = 0; i < n; i++) {
      sortedRowIdx[i] = i;
    }

    Arrays.sort(sortedRowIdx, Comparator.comparingInt(i -> p.postalKeys()[i]));

    int[] sortedKeys = new int[n];
    int[] sortedCityIds = new int[n];
    int[] sortedStateIds = new int[n];
    String[] sortedZips = new String[n];

    for (int r = 0; r < n; r++) {
      int orig = sortedRowIdx[r];
      sortedKeys[r] = p.postalKeys()[orig];
      sortedCityIds[r] = p.cityIds()[orig];
      sortedStateIds[r] = p.stateIds()[orig];
      sortedZips[r] = p.zips()[orig];
    }

    int[] runStart = new int[n];
    int[] runEnd = new int[n];

    int r = 0;
    while (r < n) {
      int key = sortedKeys[r];
      int end = r;
      while (end < n && sortedKeys[end] == key) {
        end++;
      }
      for (int k = r; k < end; k++) {
        runStart[k] = r;
        runEnd[k] = end;
      }
      r = end;
    }

    return new SortedData(sortedKeys, sortedCityIds, sortedStateIds, sortedZips, runStart, runEnd);
  }

  /**
   * Safe because sortedKeys is sorted by (countryId << 24)|zipHash,
   * so all rows for a given country are contiguous.
   */
  private CountryRanges buildCountryRanges(SortedData s, int countryCount) {
    int[] start = new int[countryCount];
    int[] end = new int[countryCount];
    Arrays.fill(start, -1);

    for (int r = 0; r < s.sortedKeys().length; r++) {
      int cid = (s.sortedKeys()[r] >> 24) & 0xFF;
      if (start[cid] == -1) {
        start[cid] = r;
      }
      end[cid] = r + 1;
    }

    return new CountryRanges(start, end);
  }

  /**
   * sortedKeys is sorted by postalKey, NOT by city, so we need a separate
   * city-sorted index to get contiguous city runs.
   */
  private CityRanges buildCityRanges(SortedData s, int cityCount) {
    int n = s.sortedCityIds().length;
    Integer[] citySortedIdx = new Integer[n];

    for (int i = 0; i < n; i++) {
      citySortedIdx[i] = i;
    }

    Arrays.sort(citySortedIdx, Comparator.comparingInt(i -> s.sortedCityIds()[i]));

    int[] slotIndex = new int[n];
    for (int r = 0; r < n; r++) {
      slotIndex[r] = citySortedIdx[r];
    }

    int[] cityStart = new int[cityCount];
    int[] cityEnd = new int[cityCount];
    Arrays.fill(cityStart, -1);

    int r = 0;
    while (r < n) {
      int cid = s.sortedCityIds()[citySortedIdx[r]];
      int end = r;
      while (end < n && s.sortedCityIds()[citySortedIdx[end]] == cid) {
        end++;
      }
      cityStart[cid] = r;
      cityEnd[cid] = end;
      r = end;
    }

    return new CityRanges(slotIndex, cityStart, cityEnd);
  }

  private FSTBundle buildFSTs(List<String[]> raw, StringTables t) throws IOException {
    // cityFST: original-case city name -> city ordinal
    FST<Long> cityFST = buildFST(t.cities(), null, false);

    // cityPostalFST: "COUNTRY|CITY" (uppercase) -> city ordinal
    Map<String, Integer> countryCityMap = new LinkedHashMap<>();
    for (String[] f : raw) {
      String key = f[0].toUpperCase() + "|" + f[2].toUpperCase();
      countryCityMap.putIfAbsent(key, t.cityIdx().get(f[2].toUpperCase()));
    }

    String[] countryCityKeys = countryCityMap.keySet().toArray(new String[0]);
    int[] countryCityOrds = countryCityMap.values().stream().mapToInt(Integer::intValue).toArray();
    FST<Long> cityPostalFST = buildFST(countryCityKeys, countryCityOrds, false);

    // countryPostalFST: uppercase country code -> country ordinal
    int[] countryOrdinals = new int[t.countries().length];
    for (int i = 0; i < t.countries().length; i++) {
      countryOrdinals[i] = i;
    }

    FST<Long> countryPostalFST = buildFST(t.countries(), countryOrdinals, true);

    return new FSTBundle(cityFST, cityPostalFST, countryPostalFST);
  }

  /**
   * Packs cityId (20 bits) and stateId (11 bits) into a single int per row.
   */
  private int[] packCityStateIds(SortedData s) {
    int n = s.sortedCityIds().length;
    int[] cityStateIds = new int[n];
    for (int r = 0; r < n; r++) {
      if (s.sortedStateIds()[r] > 0x7FF) {
        throw new IllegalStateException("State ordinal exceeds 11 bits: " + s.sortedStateIds()[r]);
      }
      if (s.sortedCityIds()[r] > 0xFFFFF) {
        throw new IllegalStateException("City ordinal exceeds 20 bits: " + s.sortedCityIds()[r]);
      }
      cityStateIds[r] = (s.sortedCityIds()[r] << 11) | (s.sortedStateIds()[r] & 0x7FF);
    }
    return cityStateIds;
  }

  private void writeIndex(
      StringTables t,
      SortedData s,
      CountryRanges cty,
      CityRanges city,
      FSTBundle fsts,
      int[] cityStateIds
  ) throws IOException {
    try (ObjectOutputStream out = new ObjectOutputStream(
        new GZIPOutputStream(new FileOutputStream(indexFile)))) {

      out.writeObject(s.sortedKeys());
      out.writeObject(s.runStart());
      out.writeObject(s.runEnd());
      out.writeObject(cityStateIds);

      writePackedStrings(out, t.cities());
      writePackedStrings(out, t.states());
      // Empty string "" is the sentinel - zero cost in the packed buffer
      writePackedStrings(out, nullsToEmpty(t.stateCodes()));
      writePackedStrings(out, t.countries());
      out.writeObject(s.sortedZips());

      out.writeObject(city.slotIndex());
      out.writeObject(city.start());
      out.writeObject(city.end());
      out.writeObject(cty.start());
      out.writeObject(cty.end());

      saveFST(out, fsts.cityFST());
      saveFST(out, fsts.cityPostalFST());
      saveFST(out, fsts.countryPostalFST());
    }
  }

  private String[] nullsToEmpty(String[] arr) {
    String[] out = new String[arr.length];
    for (int i = 0; i < arr.length; i++) {
      out[i] = arr[i] != null ? arr[i] : "";
    }
    return out;
  }

  private List<String[]> readTsv() throws IOException {
    List<String[]> raw = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(new FileInputStream(dataDir + "/" + ALL_COUNTRIES_TXT), StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.trim().isEmpty()) {
          continue;
        }
        String[] f = line.split("\t");
        if (f.length >= 4 && (countryFilter == null || countryFilter.contains(f[0].toUpperCase()))) {
          raw.add(f);
        }
      }
    }
    return raw;
  }

  private void extractDataset() throws IOException {
    if (Files.exists(Paths.get(dataDir + "/" + ALL_COUNTRIES_TXT))) {
      log.info("Dataset already extracted, skipping...");
      return;
    }
    log.info("Extracting postal codes...");
    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get(dataDir + "/" + ALL_COUNTRIES_ZIP)))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.isDirectory() && entry.getName().equals(ALL_COUNTRIES_TXT)) {
          Files.copy(zis, Paths.get(dataDir, ALL_COUNTRIES_TXT), StandardCopyOption.REPLACE_EXISTING);
          break;
        }
      }
    }
  }

  private void downloadDataset() throws IOException {
    Path path = Paths.get(dataDir + "/" + ALL_COUNTRIES_ZIP);
    if (Files.exists(path)) {
      log.info("Dataset already downloaded, skipping...");
      return;
    }
    log.info("Downloading GeoNames postal codes...");
    try (InputStream in = URI.create(downloadUrl).toURL().openStream()) {
      Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static FST<Long> buildFST(String[] keys, int[] outputs, boolean uppercase) throws IOException {
    PositiveIntOutputs posOut = PositiveIntOutputs.getSingleton();
    FSTCompiler<Long> compiler = new FSTCompiler.Builder<>(FST.INPUT_TYPE.BYTE1, posOut).build();
    IntsRefBuilder ints = new IntsRefBuilder();

    Map<String, Long> deduped = new LinkedHashMap<>();
    for (int i = 0; i < keys.length; i++) {
      String k = uppercase ? keys[i].toUpperCase() : keys[i];
      long val = (outputs == null) ? i : (long) outputs[i];
      deduped.putIfAbsent(k, val);
    }

    List<Map.Entry<String, Long>> entries = new ArrayList<>(deduped.entrySet());
    entries.sort(Map.Entry.comparingByKey());
    for (Map.Entry<String, Long> e : entries) {
      Util.toIntsRef(new BytesRef(e.getKey()), ints);
      compiler.add(ints.get(), e.getValue());
    }

    FST.FSTMetadata<Long> metadata = compiler.compile();
    return FST.fromFSTReader(metadata, compiler.getFSTReader());
  }

  private static void saveFST(ObjectOutputStream out, FST<Long> fst) throws IOException {
    if (fst == null) {
      throw new IllegalStateException("FST must not be null - ensure input keys are non-empty after deduplication");
    }
    ByteBuffersDataOutput metaOut = ByteBuffersDataOutput.newResettableInstance();
    ByteBuffersDataOutput fstOut = ByteBuffersDataOutput.newResettableInstance();
    fst.save(metaOut, fstOut);
    out.writeObject(metaOut.toArrayCopy());
    out.writeObject(fstOut.toArrayCopy());
  }

  private static void writePackedStrings(ObjectOutputStream out, String[] strings) throws IOException {
    int[] offsets = new int[strings.length + 1];
    byte[][] encoded = new byte[strings.length][];
    for (int i = 0; i < strings.length; i++) {
      encoded[i] = strings[i].getBytes(StandardCharsets.UTF_8);
      offsets[i + 1] = offsets[i] + encoded[i].length;
    }

    byte[] buf = new byte[offsets[strings.length]];
    for (int i = 0; i < strings.length; i++) {
      System.arraycopy(encoded[i], 0, buf, offsets[i], encoded[i].length);
    }

    out.writeObject(offsets);
    out.writeObject(buf);
  }

}
