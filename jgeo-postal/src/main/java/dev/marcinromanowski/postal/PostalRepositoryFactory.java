package dev.marcinromanowski.postal;

import java.io.IOException;

public class PostalRepositoryFactory {

  private PostalRepositoryFactory() {

  }

  public static PostalRepository fileIndex(String filePath) throws IOException, ClassNotFoundException {
    return new FileIndexPostalRepository(filePath);
  }

}
