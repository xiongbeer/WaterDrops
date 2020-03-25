package com.xiongbeer.waterdrops.schema.filter;

import java.lang.reflect.Field;

public class ThriftInternalFieldFilter implements FieldFilter {
    @Override
    public boolean filter(Field field) {
        return !field.getName().startsWith("_");
    }
}
