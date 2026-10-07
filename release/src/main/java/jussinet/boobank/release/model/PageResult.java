package jussinet.boobank.release.model;

import java.util.List;
import java.util.stream.IntStream;

/**
 * One page of a list.
 *
 * @param number zero-based page number
 */
public record PageResult<T>(List<T> content, int number, int size, long totalElements) {

    public static <T> PageResult<T> of(List<T> all, int page, int size) {
        int from = (int) Math.min((long) page * size, all.size());
        int to = (int) Math.min((long) from + size, all.size());
        return new PageResult<>(List.copyOf(all.subList(from, to)), page, size, all.size());
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    /**
     * One-based page numbers, for pagination links.
     */
    public List<Integer> pageNumbers() {
        return IntStream.rangeClosed(1, totalPages()).boxed().toList();
    }
}
