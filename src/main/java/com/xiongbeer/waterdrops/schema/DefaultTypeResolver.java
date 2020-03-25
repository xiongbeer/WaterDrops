package com.xiongbeer.waterdrops.schema;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.sql.type.SqlTypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultTypeResolver implements TypeResolver {
    private static Logger LOGGER = LoggerFactory.getLogger(DefaultTypeResolver.class);

    @Override
    public void resolveBasicType(SchemaField schemaField, SqlTypeName sqlType) {
        LOGGER.debug("resolveBasicType-path:{} field:{} ", schemaField.getPath(), schemaField.getSelfClass());
        schemaField.setFieldType(FieldTypeEnum.OPTIONAL);
        schemaField.setSqlType(sqlType);
    }

    @Override
    public List<SchemaField> resolveIterable(SchemaField schemaField, Field field, ScopeContext scopeContext, Resolver resolver) {
        LOGGER.debug("resolveIterable-path:{} field:{} ", schemaField.getPath(), schemaField.getSelfClass());
        schemaField.setFieldType(FieldTypeEnum.ITERABLE);
        return iterableGenericTypeParser(field, scopeContext, resolver);
    }

    @Override
    public List<SchemaField> resolveMap(SchemaField schemaField, Field field, ScopeContext scopeContext, Resolver resolver) {
        LOGGER.debug("resolveMap-path:{} field:{} ", schemaField.getPath(), schemaField.getSelfClass());
        schemaField.setFieldType(FieldTypeEnum.MAP);
        return mapGenericTypeParser(field, scopeContext, resolver);
    }

    @Override
    public List<SchemaField> resolveUserDefinedClass(SchemaField schemaField, ScopeContext scopeContext, Resolver resolver)
            throws ClassNotFoundException {
        LOGGER.debug("resolveUserDefinedClass-path:{} field:{} genericMap:{}", schemaField.getPath(),
                schemaField.getClass(), scopeContext.getGenericMap());
        schemaField.setFieldType(FieldTypeEnum.OPTIONAL);
        return resolver.getClassResolver().resolveClass(schemaField.getSelfClass(), scopeContext, resolver);
    }

    @Override
    public void resolveEnum(SchemaField schemaField, ClassResolver classResolver) {
        schemaField.setFieldType(FieldTypeEnum.NOT_SUPPORT);
    }

    @Override
    public void resolveIterableGenericType(SchemaField schemaField, Map<String, Class<?>> genericMap) {
        LOGGER.debug("resolveIterableGenericType-path:{} field:{} genericMap:{}", schemaField.getPath(),
                schemaField.getClass(), genericMap);
    }

    @Override
    public void resolveMapGenericType(SchemaField schemaField, Map<String, Class<?>> genericMap) {
        LOGGER.debug("resolveMapGenericType-path:{} field:{} genericMap:{}", schemaField.getPath(),
                schemaField.getClass(), genericMap);
    }

    /**
     * 主要用于解析List、Set类型的泛型
     */
    private List<SchemaField> iterableGenericTypeParser(Field field, ScopeContext scopeContext, Resolver resolver) {
        Type type = field.getGenericType();
        if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {
            return ImmutableList.of();
        }
        Type genericType = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (ResolvableType.forType(genericType).getRawClass() == Object.class) {
            return ImmutableList.of();
        }
        LOGGER.debug("resolveIterable-genericType:<{}> ", genericType.getTypeName());
        return genericTypeParser(genericType, scopeContext, resolver, "#val");
    }

    private List<SchemaField> mapGenericTypeParser(Field field, ScopeContext scopeContext, Resolver resolver) {
        Type type = field.getGenericType();
        if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {
            return ImmutableList.of();
        }
        Type[] genericTypes = ((ParameterizedType) type).getActualTypeArguments();
        Type genericKeyType = genericTypes[0];
        Type genericValueType = genericTypes[1];
        LOGGER.debug("resolveMap-genericType:<{}, {}> ", genericKeyType.getTypeName(), genericValueType.getTypeName());
        List<SchemaField> keyFields;
        List<SchemaField> valFields;
        if (ResolvableType.forType(genericKeyType).getRawClass() == Object.class) {
            keyFields = ImmutableList.of();
        } else {
            keyFields = genericTypeParser(genericKeyType, scopeContext, resolver, "#key");
        }
        if (ResolvableType.forType(genericValueType).getRawClass() == Object.class) {
            valFields = ImmutableList.of();
        } else {
            valFields = genericTypeParser(genericValueType, scopeContext, resolver, "#val");
        }
        List<SchemaField> res = new ArrayList<>(keyFields);
        res.addAll(valFields);
        return res;
    }


    private List<SchemaField> genericTypeParser(Type genericType, ScopeContext scopeContext, Resolver resolver,
                                                String basicFieldTag) {
        Map<String, Class<?>> genericMap = scopeContext.getGenericMap();
        String genericClazzName = genericType.toString();
        Class<?> actualGenericClazz = genericMap.get(genericType.getTypeName());
        Class<?> genericClazz;
        if (actualGenericClazz != null) {
            genericClazz = ResolvableType.forType(actualGenericClazz).getRawClass();
        } else {
            genericClazz = ResolvableType.forType(genericType).getRawClass();
        }
        if (genericClazz == null) {
            return ImmutableList.of();
        }
        BasicTypeEnum basicType = BasicTypeEnum.anyMatch(genericClazz);
        if (List.class.isAssignableFrom(genericClazz) || Map.class.isAssignableFrom(genericClazz)) {
            return ImmutableList.of();
        } else if (basicType == BasicTypeEnum.UNKNOWN) {
            try {
                ScopeContext context = scopeContext.shallowCopy()
                        .clearGenericMap();
                return resolver.getClassResolver().resolveClass(genericClazz, context, resolver);
            } catch (ClassNotFoundException e) {
                LOGGER.error("load class failed, cannot find class:{}" + genericClazzName, e);
            }
        } else {
            SchemaField schemaField = new SchemaField()
                    .setFieldType(FieldTypeEnum.OPTIONAL)
                    .setFrom(scopeContext.getParent().getClass())
                    .setParent(scopeContext.getParent())
                    .setSelfClass(genericClazz)
                    .setFieldName(basicFieldTag)
                    .setPath(scopeContext.resolvePath(basicFieldTag))
                    .setSqlType(basicType.getSqlType());
            return ImmutableList.of(schemaField);
        }
        return ImmutableList.of();
    }
}
