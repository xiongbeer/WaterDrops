package com.xiongbeer.waterdrops.calcite;

import org.apache.calcite.linq4j.Enumerator;

import java.util.List;
import java.util.Map;

public class TableEnumerator<E> implements Enumerator<E> {
    private List<Map<String, Object>> rows;
    private int index = -1;
    private E e;

    public TableEnumerator(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    @Override
    public E current() {
        return this.e;
    }

    @Override
    public boolean moveNext() {
        if (this.index + 1 >= this.rows.size()) {
            return false;
        } else {
            this.e = (E) this.rows.get(this.index + 1).values().toArray();
            ++this.index;
            return true;
        }
    }

    @Override
    public void reset() {
        this.index = -1;
        this.e = null;
    }

    @Override
    public void close() {
        // empty
    }
}
