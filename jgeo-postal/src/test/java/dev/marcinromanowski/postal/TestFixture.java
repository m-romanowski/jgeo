package dev.marcinromanowski.postal;

import java.net.URL;

class TestFixture {

  static final String INDEX_FILE = "index.bin.gz";

  private TestFixture() {

  }

  static String indexPath() {
    URL indexFileUrl = TestFixture.class.getClassLoader()
        .getResource(INDEX_FILE);
    if (indexFileUrl == null) {
      throw new IllegalStateException("Unknown index file");
    }
    return indexFileUrl.getPath();
  }

}
