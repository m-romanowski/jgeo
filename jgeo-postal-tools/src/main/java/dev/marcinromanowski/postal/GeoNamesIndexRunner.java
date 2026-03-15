package dev.marcinromanowski.postal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class GeoNamesIndexRunner {

  private GeoNamesIndexRunner() {

  }

  static void main(String[] args) throws IOException {
    IndexBuilder builder = GeoNamesIndexBuilder.builder();

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--url" -> builder.downloadUrl(args[++i]);
        case "--data-dir" -> builder.dataDir(args[++i]);
        case "--index" -> builder.indexFile(args[++i]);
        case "--country" -> {
          i++;
          Set<String> filter = new HashSet<>();
          // comma-separated: --country PL, DE, FR or repeated: --country PL --country DE
          for (String c : args[i].split(",")) {
            filter.add(c.trim().toUpperCase());
          }
          builder.countryFilter(filter);
        }
        default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
      }
    }

    builder.build()
        .run();
  }

}
