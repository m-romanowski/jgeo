package dev.marcinromanowski.postal;

import static dev.marcinromanowski.postal.TestFixture.indexPath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S2699") // Just metrics
class PerformanceTest {

  private static final int WARMUP_ITERATIONS = 1_000;
  private static final int TEST_ITERATIONS = 10_000;

  private static PostalRepositoryWrapper repository;

  @BeforeAll
  static void setup() throws Exception {
    String indexPath = indexPath();
    repository = new PostalRepositoryWrapper(PostalRepositoryFactory.fileIndex(indexPath));

    // Warmup
    System.out.println("Warming up JVM...");

    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      repository.getByZip("PL", "87-100");
      repository.searchCity("PL", "TOR", 1, 10);
    }

    System.out.println("Warmup complete.\n");
  }

  @Test
  void shouldMeasureGetByZipPerformance() {
    measureOperation("getByZip (existing)", TEST_ITERATIONS,
        () -> repository.getByZip("PL", "87-100"));
  }

  @Test
  void shouldMeasureGetByZipNotFoundPerformance() {
    measureOperation("getByZip (not found)", TEST_ITERATIONS,
        () -> repository.getByZip("PL", "99-999"));
  }

  @Test
  void shouldMeasureSearchCityPerformance() {
    measureOperation("searchCity (3 chars)", TEST_ITERATIONS,
        () -> repository.searchCity("PL", "TOR", 1, 10));
  }

  @Test
  void shouldMeasureSearchCityCommonPrefixPerformance() {
    measureOperation("searchCity (2 chars - common)", TEST_ITERATIONS,
        () -> repository.searchCity("PL", "WA", 1, 20));
  }

  @Test
  void shouldMeasureGetZipsByCityPerformance() {
    measureOperation("getZipsByCity", TEST_ITERATIONS,
        () -> repository.getZipsByCity("PL", "WARSZAWA", 1, 10));
  }

  @Test
  void shouldMeasureGetZipsByCountryPerformance() {
    measureOperation("getZipsByCountry", TEST_ITERATIONS / 10, // Slower operation
        () -> repository.getZipsByCountry("PL", 1, 100));
  }

  @Test
  void shouldMeasureMixedWorkloadPerformance() {
    measureOperation("Mixed workload", TEST_ITERATIONS / 5, () -> {
      repository.getByZip("PL", "87-100");
      repository.getByZip("PL", "00-001");
      repository.searchCity("PL", "TOR", 1, 10);
      repository.getZipsByCity("PL", "WARSZAWA", 1, 10);
    });
  }

  @Test
  void shouldGeneratePerformanceReport() {
    System.out.println("\n" + "=".repeat(100));
    System.out.println("COMPREHENSIVE PERFORMANCE REPORT");
    System.out.println("=".repeat(100));

    List<PerformanceMetric> metrics = new ArrayList<>();

    metrics.add(measure("getByZip (existing)", 10000,
        () -> repository.getByZip("PL", "87-100")));
    metrics.add(measure("getByZip (not found)", 10000,
        () -> repository.getByZip("PL", "99-999")));
    metrics.add(measure("searchCity (3 chars)", 10000,
        () -> repository.searchCity("PL", "TOR", 1, 10)));
    metrics.add(measure("searchCity (2 chars)", 10000,
        () -> repository.searchCity("PL", "WA", 1, 20)));
    metrics.add(measure("searchCity (1 char)", 5000,
        () -> repository.searchCity("PL", "W", 1, 20)));
    metrics.add(measure("getZipsByCity (small)", 10000,
        () -> repository.getZipsByCity("PL", "TORUŃ", 1, 10)));
    metrics.add(measure("getZipsByCity (large)", 5000,
        () -> repository.getZipsByCity("PL", "WARSZAWA", 1, 100)));
    metrics.add(measure("getZipsByCountry", 1000,
        () -> repository.getZipsByCountry("PL", 1, 100)));
    metrics.add(measure("Case-insensitive search", 10000,
        () -> repository.searchCity("PL", "tor", 1, 10)));

    System.out.printf("%n%-35s | %12s | %12s | %15s | %12s%n",
        "Operation", "Iterations", "Total (ms)", "Avg (μs)", "Ops/sec");
    System.out.println("-".repeat(100));

    for (PerformanceMetric metric : metrics) {
      System.out.printf("%-35s | %,12d | %,12d | %,15.2f | %,12.0f%n",
          metric.name(),
          metric.iterations(),
          metric.totalTimeMs(),
          metric.avgTimeMicros(),
          metric.opsPerSecond());
    }

    System.out.println("-".repeat(100));

    double avgOpsPerSec = metrics.stream()
        .mapToDouble(PerformanceMetric::opsPerSecond)
        .average()
        .orElse(0);

    System.out.printf("%nSummary:%n");
    System.out.printf("  Total Operations:        %,d%n",
        metrics.stream().mapToLong(PerformanceMetric::iterations).sum());
    System.out.printf("  Average Throughput:      %,.0f ops/sec%n", avgOpsPerSec);
    System.out.printf("  Fastest Operation:       %s (%.2f μs)%n",
        metrics.stream().min(Comparator.comparingDouble(PerformanceMetric::avgTimeMicros))
            .map(PerformanceMetric::name).orElse("N/A"),
        metrics.stream().mapToDouble(PerformanceMetric::avgTimeMicros).min().orElse(0));
    System.out.printf("  Slowest Operation:       %s (%.2f μs)%n",
        metrics.stream().max(Comparator.comparingDouble(PerformanceMetric::avgTimeMicros))
            .map(PerformanceMetric::name).orElse("N/A"),
        metrics.stream().mapToDouble(PerformanceMetric::avgTimeMicros).max().orElse(0));

    System.out.println("=".repeat(100) + "\n");
  }

  private void measureOperation(String name, int iterations, Runnable operation) {
    long startTime = System.nanoTime();

    for (int i = 0; i < iterations; i++) {
      operation.run();
    }

    long endTime = System.nanoTime();
    long totalTime = endTime - startTime;
    double avgTime = totalTime / (double) iterations;

    System.out.println("=".repeat(80));
    System.out.printf("Performance: %s%n", name);
    System.out.println("-".repeat(80));
    System.out.printf("Iterations:              %,d%n", iterations);
    System.out.printf("Total time:              %,d ms%n", TimeUnit.NANOSECONDS.toMillis(totalTime));
    System.out.printf("Average time:            %.2f μs%n", avgTime / 1000.0);
    System.out.printf("Throughput:              %,.0f ops/sec%n",
        1_000_000_000.0 / avgTime);
    System.out.println("=".repeat(80) + "\n");
  }

  private PerformanceMetric measure(String name, int iterations, Runnable operation) {
    long startTime = System.nanoTime();

    for (int i = 0; i < iterations; i++) {
      operation.run();
    }

    long endTime = System.nanoTime();
    long totalTime = endTime - startTime;

    return new PerformanceMetric(
        name,
        iterations,
        TimeUnit.NANOSECONDS.toMillis(totalTime),
        totalTime / (double) iterations / 1000.0,
        1_000_000_000.0 / (totalTime / (double) iterations)
    );
  }

}
