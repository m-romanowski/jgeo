package dev.marcinromanowski.postal;

record PerformanceMetric(
    String name,
    int iterations,
    long totalTimeMs,
    double avgTimeMicros,
    double opsPerSecond
) {

}
