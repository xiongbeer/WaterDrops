package com.xiongbeer.waterdrops.schema;

import org.apache.calcite.sql.type.SqlTypeName;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public interface TypeResolver {
    void resolveBasicType(SchemaField schemaField, SqlTypeName sqlType);

    List<SchemaField> resolveIterable(SchemaField schemaField, Field field, ScopeContext scopeContext, Resolver resolver);

    List<SchemaField> resolveMap(SchemaField schemaField,Field field, ScopeContext scopeContext, Resolver resolver);

    List<SchemaField> resolveUserDefinedClass(SchemaField schemaField, ScopeContext scopeContext, Resolver resolver)
            throws ClassNotFoundException;

    void resolveEnum(SchemaField schemaField, ClassResolver classResolver);

    void resolveIterableGenericType(SchemaField schemaField, Map<String, Class<?>> genericMap);

    void resolveMapGenericType(SchemaField schemaField, Map<String, Class<?>> genericMap);
}
