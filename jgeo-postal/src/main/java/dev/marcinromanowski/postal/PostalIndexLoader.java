package dev.marcinromanowski.postal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import org.apache.lucene.store.ByteBuffersDataInput;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;

class PostalIndexLoader {

  int[] postalKeys;
  int[] runStart;
  int[] runEnd;
  int[] cityStateIds; // packed: cityId << 11 | stateId (0x7FF)

  // Packed string dictionaries
  int[] citiesOffsets;
  byte[] citiesBuf;
  int[] statesOffsets;
  byte[] statesBuf;
  int[] countriesOffsets;
  byte[] countriesBuf;
  int[] stateCodesOffsets;
  byte[] stateCodesBuf;

  String[] postalCodes; // sortedZips - postalKey-sorted

  int[] citySlotIndex; // city-sorted -> postalKey-sorted slot
  int[] cityPostalStart;
  int[] cityPostalEnd;
  int[] countryPostalStart;
  int[] countryPostalEnd;

  Map<String, Integer> countryCodeMap;

  FST<Long> cityFST;
  FST<Long> cityPostalFST;
  FST<Long> countryPostalFST;

  static PostalIndexLoader load(String filePath) throws IOException, ClassNotFoundException {
    PostalIndexLoader loader = new PostalIndexLoader();

    try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filePath)))) {
      loader.postalKeys = (int[]) in.readObject();
      loader.runStart = (int[]) in.readObject();
      loader.runEnd = (int[]) in.readObject();
      loader.cityStateIds = (int[]) in.readObject();

      loader.citiesOffsets = (int[]) in.readObject();
      loader.citiesBuf = (byte[]) in.readObject();
      loader.statesOffsets = (int[]) in.readObject();
      loader.statesBuf = (byte[]) in.readObject();
      loader.stateCodesOffsets = (int[]) in.readObject();
      loader.stateCodesBuf = (byte[]) in.readObject();
      loader.countriesOffsets = (int[]) in.readObject();
      loader.countriesBuf = (byte[]) in.readObject();

      loader.postalCodes = (String[]) in.readObject();

      loader.citySlotIndex = (int[]) in.readObject();
      loader.cityPostalStart = (int[]) in.readObject();
      loader.cityPostalEnd = (int[]) in.readObject();
      loader.countryPostalStart = (int[]) in.readObject();
      loader.countryPostalEnd = (int[]) in.readObject();

      loader.cityFST = loadFST(in);
      loader.cityPostalFST = loadFST(in);
      loader.countryPostalFST = loadFST(in);
    }

    loader.countryCodeMap = buildCountryCodeMap(loader);

    return loader;
  }

  String getCity(int ordinal) {
    return new String(
        citiesBuf,
        citiesOffsets[ordinal],
        citiesOffsets[ordinal + 1] - citiesOffsets[ordinal],
        StandardCharsets.UTF_8
    );
  }

  String getState(int ordinal) {
    return new String(
        statesBuf,
        statesOffsets[ordinal],
        statesOffsets[ordinal + 1] - statesOffsets[ordinal],
        StandardCharsets.UTF_8
    );
  }

  Optional<String> getStateCode(int ordinal) {
    int start = stateCodesOffsets[ordinal];
    int len = stateCodesOffsets[ordinal + 1] - start;
    if (len == 0) {
      return Optional.empty();
    }
    String value = new String(
        stateCodesBuf,
        start,
        len,
        StandardCharsets.UTF_8
    );
    return Optional.of(value);
  }

  String getCountry(int ordinal) {
    return new String(
        countriesBuf,
        countriesOffsets[ordinal],
        countriesOffsets[ordinal + 1] - countriesOffsets[ordinal],
        StandardCharsets.UTF_8
    );
  }

  int countryCount() {
    return countriesOffsets.length - 1;
  }

  private static Map<String, Integer> buildCountryCodeMap(PostalIndexLoader loader) {
    int count = loader.countryCount();
    Map<String, Integer> map = HashMap.newHashMap(count * 2);
    for (int i = 0; i < count; i++) {
      map.put(loader.getCountry(i), i);
    }
    return map;
  }

  private static FST<Long> loadFST(ObjectInputStream in) throws IOException, ClassNotFoundException {
    byte[] metaBytes = (byte[]) in.readObject();
    byte[] fstBytes = (byte[]) in.readObject();

    ByteBuffersDataInput metaInput = new ByteBuffersDataInput(List.of(ByteBuffer.wrap(metaBytes)));
    ByteBuffersDataInput fstInput = new ByteBuffersDataInput(List.of(ByteBuffer.wrap(fstBytes)));

    FST.FSTMetadata<Long> metadata = FST.readMetadata(metaInput, PositiveIntOutputs.getSingleton());

    return new FST<>(metadata, fstInput);
  }

}
