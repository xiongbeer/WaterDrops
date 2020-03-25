package com.xiongbeer.waterdrops.schema;

import com.xiongbeer.waterdrops.schema.filter.FieldFilter;

import java.lang.reflect.Field;
import java.util.List;

public interface FieldResolver {
    List<SchemaField> resolveField(Field field, ScopeContext scopeContext, Resolver resolver);

    void resolveAnnotation(Field field, SchemaField schemaField);

    List<FieldFilter> filters();
}
