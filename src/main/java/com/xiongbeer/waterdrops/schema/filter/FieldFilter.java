package com.xiongbeer.waterdrops.schema.filter;

import java.lang.reflect.Field;

public interface FieldFilter {
    boolean filter(Field field);
}
