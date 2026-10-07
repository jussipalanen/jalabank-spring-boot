package jussinet.jalabank.release.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One page of a list.
 *
 * @param number zero-based page number
 */
public record PageResult<T>(List<T> content, int number, int size, long totalElements) {

    /**
     * The given page of {@code all}. A page past the end is empty.
     */
    public static <T> PageResult<T> of(List<T> all, int page, int size) {
        int from = (int) Math.min((long) page * size, all.size());
        int to = (int) Math.min((long) from + size, all.size());
        return new PageResult<>(List.copyOf(all.subList(from, to)), page, size, all.size());
    }

    /**
     * Like {@link #of}, but a page past the end shows the last page instead.
     */
    public static <T> PageResult<T> ofClamped(List<T> all, int page, int size) {
        int lastPage = Math.max(0, (all.size() - 1) / size);
        return of(all, Math.min(page, lastPage), size);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    /** One-based number of the current page */
    public int currentPage() {
        return number + 1;
    }

    public boolean hasPrevious() {
        return number > 0;
    }

    public boolean hasNext() {
        return number + 1 < totalPages();
    }

    /**
     * One-based page numbers for pagination links: the first and last page and the pages around the current one.
     * {@code null} marks a gap, shown as an ellipsis.
     */
    public List<Integer> pageWindow() {
        List<Integer> pages = new ArrayList<>();
        int total = totalPages();
        int current = currentPage();
        for (int p = 1; p <= total; p++) {
            if (p == 1 || p == total || Math.abs(p - current) <= 2) {
                if (!pages.isEmpty() && pages.getLast() != null && p - pages.getLast() > 1) {
                    pages.add(null);
                }
                pages.add(p);
            }
        }
        return pages;
    }
}
