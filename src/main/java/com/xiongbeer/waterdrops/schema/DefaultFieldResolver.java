package com.xiongbeer.waterdrops.schema;

import com.google.common.collect.ImmutableList;
import com.xiongbeer.waterdrops.schema.filter.AsmInjectFieldFilter;
import com.xiongbeer.waterdrops.schema.filter.FieldFilter;
import com.xiongbeer.waterdrops.schema.filter.StaticFieldFilter;
import com.xiongbeer.waterdrops.schema.filter.ThriftInternalFieldFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultFieldResolver implements FieldResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFieldResolver.class);
    private final List<FieldFilter> defaultFilters = ImmutableList.of(new AsmInjectFieldFilter(),
            new StaticFieldFilter(), new ThriftInternalFieldFilter());

    @Override
    public List<SchemaField> resolveField(Field field, ScopeContext scopeContext, Resolver resolver) {
        for (FieldFilter fieldFilter : filters()) {
            if (!fieldFilter.filter(field)) {
                return ImmutableList.of();
            }
        }
        List<SchemaField> result = new ArrayList<>();
        SchemaField schemaField = new SchemaField()
                .setPath(scopeContext.resolvePath(field.getName()))
                .setParent(scopeContext.getParent())
                .setFieldType(FieldTypeEnum.UNKNOWN)
                .setSelfClass(field.getType())
                .setFieldName(field.getName());
        ScopeContext context = scopeContext.shallowCopy()
                .clearGenericMap()
                .setParent(schemaField)
                .setPathPrefix(schemaField.getPath());
        resolveAnnotation(field, schemaField);
        String genericTypeString = field.getGenericType().toString();
        Class<?> genericClazz = scopeContext.getGenericMap().get(genericTypeString);
        genericClazz = (genericClazz == null ? field.getType() : genericClazz);
        BasicTypeEnum basicType = BasicTypeEnum.anyMatch(field.getType());
        TypeResolver typeResolver = resolver.getTypeResolver();
        ClassResolver classResolver = resolver.getClassResolver();
        if (Object.class == genericClazz) {
            schemaField.setWarn("UndefinedGenericType<" + genericTypeString + ">");
        } else if (basicType != BasicTypeEnum.UNKNOWN) {
            typeResolver.resolveBasicType(schemaField, basicType.getSqlType());
        } else if (Iterable.class.isAssignableFrom(field.getType())) {
            List<SchemaField> nested = typeResolver.resolveIterable(schemaField, field, context, resolver);
            result.addAll(nested);
        } else if (Map.class.isAssignableFrom(field.getType())) {
            List<SchemaField> nested = typeResolver.resolveMap(schemaField, field, context, resolver);
            result.addAll(nested);
        } else if (Enum.class.isAssignableFrom(field.getType())) {
            typeResolver.resolveEnum(schemaField, classResolver);
        } else {
            try {
                List<SchemaField> nested = typeResolver.resolveUserDefinedClass(schemaField, context, resolver);
                result.addAll(nested);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        result.add(schemaField);
        return result;
    }

    @Override
    public void resolveAnnotation(Field field, SchemaField schemaField) {

    }

    @Override
    public List<FieldFilter> filters() {
        return this.defaultFilters;
    }
}
