package dev.marcinromanowski.postal;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.URL;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S2699") // Just metrics
class MemoryUsageTest {

  private static final String INDEX_FILE = "index.bin.gz";
  private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

  @Test
  void shouldReportMemoryUsageForIndexLoading() throws Exception {
    // given
    String indexPath = getIndexPath();
    File indexFile = new File(indexPath);
    long fileSize = indexFile.length();

    forceGc();

    MemoryUsage heapBefore = memoryBean.getHeapMemoryUsage();
    long usedBefore = heapBefore.getUsed();

    // when
    PostalIndexLoader loader = PostalIndexLoader.load(indexPath);

    forceGc();

    MemoryUsage heapAfter = memoryBean.getHeapMemoryUsage();
    long usedAfter = heapAfter.getUsed();

    // then
    long memoryUsed = usedAfter - usedBefore;

    System.out.println("\n" + "=".repeat(80));
    System.out.println("MEMORY USAGE REPORT: Index Loading");
    System.out.println("=".repeat(80));
    System.out.printf("Index File Size:           %,d bytes (%.2f MB)%n",
        fileSize, fileSize / 1024.0 / 1024.0);
    System.out.printf("Heap Used Before:          %,d bytes (%.2f MB)%n",
        usedBefore, usedBefore / 1024.0 / 1024.0);
    System.out.printf("Heap Used After:           %,d bytes (%.2f MB)%n",
        usedAfter, usedAfter / 1024.0 / 1024.0);
    System.out.printf("Memory Consumed:           %,d bytes (%.2f MB)%n",
        memoryUsed, memoryUsed / 1024.0 / 1024.0);
    System.out.printf("Memory vs File Size:       %.2fx%n",
        (double) memoryUsed / fileSize);

    int totalEntries = loader.postalKeys.length;
    int totalCities = loader.citiesBuf.length;
    int totalCountries = loader.countriesBuf.length;

    System.out.printf("%nIndex Statistics:%n");
    System.out.printf("  Total Postal Keys:       %,d%n", totalEntries);
    System.out.printf("  Total Cities:            %,d%n", totalCities);
    System.out.printf("  Total Countries:         %,d%n", totalCountries);
    System.out.printf("  Memory per Postal Key:   %.2f bytes%n",
        (double) memoryUsed / totalEntries);
    System.out.printf("  Memory per City:         %.2f bytes%n",
        (double) memoryUsed / totalCities);

    System.out.printf("%nHeap Memory Status:%n");
    System.out.printf("  Committed:               %,d bytes (%.2f MB)%n",
        heapAfter.getCommitted(), heapAfter.getCommitted() / 1024.0 / 1024.0);
    System.out.printf("  Max:                     %,d bytes (%.2f MB)%n",
        heapAfter.getMax(), heapAfter.getMax() / 1024.0 / 1024.0);
    System.out.printf("  Usage:                   %.2f%%%n",
        (double) usedAfter / heapAfter.getMax() * 100);
    System.out.println("=".repeat(80) + "\n");
  }

  @Test
  void shouldReportMemoryUsageAfterOperations() throws Exception {
    // given
    String indexPath = getIndexPath();

    forceGc();

    MemoryUsage heapBefore = memoryBean.getHeapMemoryUsage();
    long usedBefore = heapBefore.getUsed();

    PostalRepository repository = PostalRepositoryFactory.fileIndex(indexPath);

    // when
    for (int i = 0; i < 1000; i++) {
      repository.getByZip("PL", "87-100");
      repository.searchCity("PL", "TOR", 1, 10);
      repository.getZipsByCity("PL", "WARSZAWA", 1, 10);
    }

    forceGc();

    MemoryUsage heapAfter = memoryBean.getHeapMemoryUsage();
    long usedAfter = heapAfter.getUsed();

    // then
    long memoryGrowth = usedAfter - usedBefore;

    System.out.println("\n" + "=".repeat(80));
    System.out.println("MEMORY USAGE REPORT: After 1000 Operations");
    System.out.println("=".repeat(80));
    System.out.printf("Heap Before Operations:    %,d bytes (%.2f MB)%n",
        usedBefore, usedBefore / 1024.0 / 1024.0);
    System.out.printf("Heap After Operations:     %,d bytes (%.2f MB)%n",
        usedAfter, usedAfter / 1024.0 / 1024.0);
    System.out.printf("Memory Growth:             %,d bytes (%.2f MB)%n",
        memoryGrowth, memoryGrowth / 1024.0 / 1024.0);
    System.out.printf("Average per Operation:     %.2f bytes%n",
        (double) memoryGrowth / 1000);
    System.out.println("=".repeat(80) + "\n");
  }

  private String getIndexPath() {
    URL resource = getClass().getClassLoader()
        .getResource(INDEX_FILE);
    if (resource == null) {
      throw new IllegalStateException("Index file not found");
    }
    return resource.getPath();
  }

  @SuppressWarnings("java:S2925")
  private void forceGc() throws InterruptedException {
    long gcCountBefore = getGcCount();
    System.gc();
    // Wait until at least one GC cycle has completed
    while (getGcCount() == gcCountBefore) {
      //noinspection BusyWait
      Thread.sleep(1); // to avoid a busy-wait spin
    }
  }

  private long getGcCount() {
    return ManagementFactory.getGarbageCollectorMXBeans()
        .stream()
        .mapToLong(GarbageCollectorMXBean::getCollectionCount)
        .sum();
  }

}
