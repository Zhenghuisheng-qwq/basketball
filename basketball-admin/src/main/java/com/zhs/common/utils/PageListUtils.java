package com.zhs.common.utils;

import com.github.pagehelper.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PageHelper 分页列表工具：原地 enrich 或 map 时保留 Page 元数据，避免 stream().collect() 丢失 total。
 */
public final class PageListUtils {

    private PageListUtils() {
    }

    public static <T> List<T> enrich(List<T> list, Consumer<T> enricher) {
        if (list == null || list.isEmpty() || enricher == null) {
            return list;
        }
        list.forEach(enricher);
        return list;
    }

    public static <T, R> List<R> map(List<T> source, Function<T, R> mapper) {
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return new ArrayList<>();
        }
        if (source instanceof Page<?> page) {
            Page<R> result = new Page<>(page.getPageNum(), page.getPageSize());
            result.setTotal(page.getTotal());
            for (T item : source) {
                result.add(mapper.apply(item));
            }
            return result;
        }
        return source.stream().map(mapper).collect(Collectors.toList());
    }
}
