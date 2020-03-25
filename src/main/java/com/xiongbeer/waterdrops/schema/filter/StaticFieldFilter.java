package com.xiongbeer.waterdrops.schema.filter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class StaticFieldFilter implements FieldFilter {
    @Override
    public boolean filter(Field field) {
        return !Modifier.isStatic(field.getModifiers());
    }
}
