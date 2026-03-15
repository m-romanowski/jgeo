package dev.marcinromanowski.postal;

import java.util.List;

public record PaginatedResponse<T>(
    List<T> data,
    int page,
    int size,
    int total
) {

}
