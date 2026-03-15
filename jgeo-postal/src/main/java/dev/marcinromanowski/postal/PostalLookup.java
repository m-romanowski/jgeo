package dev.marcinromanowski.postal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

class PostalLookup {

  private final PostalIndexLoader loader;

  PostalLookup(PostalIndexLoader loader) {
    this.loader = loader;
  }

  List<PostalLocation> lookupByZip(String countryCode, String zip) {
    Integer countryId = loader.countryCodeMap.get(countryCode.toUpperCase());
    if (countryId == null) {
      return List.of();
    }

    int zipHash = zip.hashCode() & 0xFFFFFF;
    int postalKey = (countryId << 24) | zipHash;

    int idx = binarySearchUnsigned(loader.postalKeys, postalKey);
    if (idx < 0) {
      return List.of();
    }

    int start = loader.runStart[idx];
    int end = loader.runEnd[idx];

    List<PostalLocation> result = new ArrayList<>(end - start);
    for (int i = start; i < end; i++) {
      int packed = loader.cityStateIds[i];
      int cityId = packed >>> 11;
      int stateId = packed & 0x7FF;
      PostalLocation location = new PostalLocation(
          loader.getCity(cityId),
          loader.getState(stateId),
          loader.getStateCode(stateId)
              .orElse(null)
      );
      result.add(location);
    }

    return result;
  }

  // Instead of Arrays.binarySearch, use unsigned binary search
  private int binarySearchUnsigned(int[] arr, int key) {
    int lo = 0;
    int hi = arr.length - 1;
    while (lo <= hi) {
      int mid = (lo + hi) >>> 1;
      int cmp = Integer.compareUnsigned(arr[mid], key);
      if (cmp < 0) {
        lo = mid + 1;
      } else if (cmp > 0) {
        hi = mid - 1;
      } else {
        return mid;
      }
    }
    return -(lo + 1);
  }

  String[] lookupByCityArray(String countryCode, String city) throws IOException {
    String key = countryCode.toUpperCase() + "|" + city.toUpperCase();
    Long cityOrdinal = fstLookup(loader.cityPostalFST, key);
    if (cityOrdinal == null) {
      return new String[0]; // now correctly returns empty, not country range
    }

    int ord = cityOrdinal.intValue();
    int start = loader.cityPostalStart[ord];
    int end = loader.cityPostalEnd[ord];
    if (start == -1) {
      return new String[0];
    }

    String[] result = new String[end - start];
    for (int i = start; i < end; i++) {
      result[i - start] = loader.postalCodes[loader.citySlotIndex[i]];
    }
    return result;
  }

  String[] lookupByCountryArray(String countryCode) throws IOException {
    Long idx = fstLookup(loader.countryPostalFST, countryCode.toUpperCase());
    if (idx == null) {
      return new String[0];
    }

    int ord = idx.intValue();
    int start = loader.countryPostalStart[ord];
    int end = loader.countryPostalEnd[ord];
    if (start == -1) {
      return new String[0];
    }

    String[] result = new String[end - start];
    if (end - start >= 0) {
      System.arraycopy(loader.postalCodes, start, result, 0, end - start);
    }

    return result;
  }

  List<String> searchCity(String country, String prefix, int page, int pageSize) throws IOException {
    if (loader.cityPostalFST == null) {
      return Collections.emptyList();
    }

    String countryUpper = country.toUpperCase();
    // cityPostalFST keys are fully uppercase so the prefix must be uppercased to match
    String fstPrefix = countryUpper + "|" + prefix.toUpperCase();
    int toSkip = (page - 1) * pageSize; // page is 1-based

    BytesRefFSTEnum<Long> fstEnum = new BytesRefFSTEnum<>(loader.cityPostalFST);

    // seekCeil jumps directly to the first key >= fstPrefix - O(prefix-length)
    BytesRefFSTEnum.InputOutput<Long> io = fstEnum.seekCeil(new BytesRef(fstPrefix));

    List<String> results = new ArrayList<>();

    while (io != null) {
      String key = io.input.utf8ToString();
      // Keys are sorted so the first key that doesn't start with fstPrefix means we're done
      if (!key.startsWith(fstPrefix)) {
        break;
      }

      if (toSkip > 0) {
        toSkip--;
      } else {
        // Resolve original-case city name from the city ordinal stored as the FST value
        String originalCity = loader.getCity((int) (long) io.output);
        results.add(originalCity);
        if (results.size() >= pageSize) {
          break;
        }
      }

      io = fstEnum.next();
    }

    return results;
  }

  private Long fstLookup(FST<Long> fst, String key) throws IOException {
    BytesRef bytes = new BytesRef(key);
    IntsRefBuilder ints = new IntsRefBuilder();
    Util.toIntsRef(bytes, ints);
    return Util.get(fst, ints.get());
  }

}
