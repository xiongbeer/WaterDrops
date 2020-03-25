package com.xiongbeer.waterdrops.schema;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultClassResolver implements ClassResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClassResolver.class);

    @Override
    public boolean stopCondition(Class<?> clazz) {
        return clazz == Object.class || Enum.class.isAssignableFrom(clazz) || clazz.isInterface();
    }

    @Override
    public Map<String, Class<?>> resolveSuperclassGenericMap(Class<?> clazz) throws ClassNotFoundException {
        Map<String, Class<?>> fieldGenericMap = new HashMap<>(4);
        ResolvableType resolvableType = ResolvableType.forClass(clazz.getSuperclass());
        if (!resolvableType.hasGenerics()) {
            return fieldGenericMap;
        }
        ResolvableType[] abstractTypes = resolvableType.getGenerics();
        Type genericSuperType = clazz.getGenericSuperclass();
        Type[] actualTypes = ((ParameterizedType) genericSuperType).getActualTypeArguments();
        final int num = abstractTypes.length;
        for (int i = 0; i < num; ++i) {
            fieldGenericMap.put(abstractTypes[i].getType().getTypeName(), Class.forName(actualTypes[i].getTypeName()));
        }
        return fieldGenericMap;
    }

    @Override
    public List<SchemaField> resolveClassField(Class<?> clazz, ScopeContext scopeContext, Resolver resolver) {
        List<SchemaField> schemaFields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            List<SchemaField> fields = resolver.getFieldResolver().resolveField(field, scopeContext, resolver);
            schemaFields.addAll(fields);
        }
        return schemaFields;
    }

    @Override
    public List<SchemaField> resolveClass(Class<?> clazz, ScopeContext scopeContext, Resolver resolver)
            throws ClassNotFoundException {
        LOGGER.debug("Enter class:{}, prefix:{}", clazz.getCanonicalName(), scopeContext.getPathPrefix());
        if (stopCondition(clazz)) {
            return ImmutableList.of();
        }
        Class<?> next = clazz;
        List<SchemaField> result = new ArrayList<>(resolveClassField(clazz, scopeContext, resolver));
        while (true) {
            next = next.getSuperclass();
            if (next == null || stopCondition(next)) {
                break;
            }
            Map<String, Class<?>> genericMap = this.resolveSuperclassGenericMap(clazz);
            ScopeContext context = scopeContext.shallowCopy()
                    .setGenericMap(genericMap);
            LOGGER.debug("Resolve superclass:{}", next.getCanonicalName());
            List<SchemaField> superclassFields = resolveClassField(next, context, resolver);
            {
                List<SchemaField> retainChecker = new ArrayList<>(superclassFields);
                retainChecker.retainAll(result);
                if (CollectionUtils.isNotEmpty(retainChecker)) {
                    LOGGER.debug("Duplicate private field name, from superclass:{}, as:{}", next.getCanonicalName(), retainChecker);
                }
            }
            result.addAll(superclassFields);
        }
        LOGGER.debug("Leave class:{}, prefix:{}", clazz.getCanonicalName(), scopeContext.getPathPrefix());
        return result;
    }
}
