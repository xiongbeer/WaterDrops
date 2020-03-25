package com.xiongbeer.waterdrops.schema;

import java.util.List;
import java.util.Map;

public interface ClassResolver {
    boolean stopCondition(Class<?> clazz);

    Map<String, Class<?>> resolveSuperclassGenericMap(Class<?> clazz) throws ClassNotFoundException;

    List<SchemaField> resolveClassField(Class<?> clazz, ScopeContext scopeContext, Resolver resolver);

    List<SchemaField> resolveClass(Class<?> clazz, ScopeContext scopeContext, Resolver resolver) throws ClassNotFoundException;
}
