package com.example.platform.dto;

import java.util.List;

public class PageResult<T> {

    private List<T> items;
    private long total;

    public PageResult(List<T> items, long total) {
        this.items = items;
        this.total = total;
    }

    public List<T> getItems() { return items; }
    public long getTotal() { return total; }
}
