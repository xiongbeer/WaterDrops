package com.xiongbeer.waterdrops.schema;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Joiner;
import com.xiongbeer.waterdrops.KeyMark;
import com.xiongbeer.waterdrops.calcite.SchemaTable;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 将业务模型统一抽象为内部的Schema管理，包含数据源模型、外部thrift引入的模型
 * 目前不支持：父类为Enum、无实际定义的泛型类型的属性
 * TODO 识别数组、增加过滤器
 *
 * @author liushaoxiong on 2020/2/24.
 */
@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
public class Schema extends AbstractSchema {
    private static final Logger LOGGER = LoggerFactory.getLogger(Schema.class);
    public static final String SPLITTER = "";
    private static final String CLASS_FLAG = "class ";
    private String tag;
    private Map<String, SchemaField> fieldPathMap = Collections.synchronizedMap(new LinkedHashMap<>());
    private Set<String> loadedClassName = Collections.synchronizedSet(new HashSet<>());
    private ThreadLocal<Map<String, Table>> tableMap = new ThreadLocal<>();
    private boolean flat = false;

    public Schema(String tag) {
        this.tag = tag;
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return this.tableMap.get();
    }

    public Map<String, SchemaField> getFieldPathMap() {
        return this.fieldPathMap;
    }

    public Schema flatMode() {
        this.flat = true;
        return this;
    }

    public void loadData(Object object) {
        if (this.tableMap.get() == null) {
            this.tableMap.set(new HashMap<>());
        }
        Map<SchemaField, List<SchemaField>> levelMap = new LinkedHashMap<>();
        this.fieldPathMap.forEach((path, field) ->
                levelMap.computeIfAbsent(field.getParent(), v -> new ArrayList<>()).add(field));
        loadData(object, this.tag, null, levelMap, new LinkedHashMap<>());
    }

    private void loadData(Object object, String tableName, SchemaField key, Map<SchemaField, List<SchemaField>> levelMap,
                          Map<SchemaField, Object> keyMarkMap) {
        Map<SchemaField, Object> selfKeyMarkMap = new LinkedHashMap<>(keyMarkMap);
        List<SchemaField> columns = new ArrayList<>();
        List<SchemaField> subTable = new ArrayList<>();
        levelMap.get(key).forEach(schemaField -> {
            if (CollectionUtils.isEmpty(levelMap.get(schemaField))) {
                columns.add(schemaField);
            } else {
                subTable.add(schemaField);
            }
        });
        List<String> names = new ArrayList<>();
        List<SqlTypeName> types = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        columns.forEach(schemaField -> {
            if (schemaField.getSqlType() != null) {
                try {
                    Object fieldData = schemaField.get(object, getOffset(schemaField));
                    Map<String, Object> row;
                    if (fieldData instanceof Iterable) {
                        int index = 0;
                        for (Object ob : (Iterable) fieldData) {
                            if (index >= rows.size()) {
                                row = new LinkedHashMap<>(32);
                                rows.add(row);
                            } else {
                                row = rows.get(index);
                            }
                            row.put(schemaField.getFieldName(), ob);
                            ++index;
                        }
                    } else {
                        if (CollectionUtils.isEmpty(rows)) {
                            row = new LinkedHashMap<>(32);
                            rows.add(row);
                        } else {
                            row = rows.get(0);
                        }
                        row.put(schemaField.getFieldName(), fieldData);
                        if (schemaField.isMarkAsKey()) {
                            selfKeyMarkMap.put(schemaField, fieldData);
                        }
                    }
                    names.add(schemaField.getFieldName());
                    types.add(schemaField.getSqlType());
                } catch (Exception e) {
                    LOGGER.error("Cover field to sql data failed.", e);
                }
            }
        });
        keyMarkMap.forEach(((schemaField, value) -> {
            names.add(schemaField.getFieldName());
            types.add(schemaField.getSqlType());
            rows.forEach(row -> row.put(schemaField.getFieldName(), value));
        }));
        SchemaTable table = new SchemaTable(this.tag);
        table.setupFieldList(names, types);
        table.apply(rows);
        this.tableMap.get().put(tableName, table);
        subTable.forEach(schemaField -> {
            try {
                loadData(schemaField.get(object, getOffset(schemaField)), schemaField.getFieldName(),
                        schemaField, levelMap, selfKeyMarkMap);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.error("xxx", e);
            }
        });
    }

    private String getOffset(SchemaField schemaField) {
        SchemaField parent = schemaField.getParent();
        String offset = "";
        if (parent != null) {
            offset = parent.getPath();
        }
        return offset;
    }

    public void finalizeData() {
        this.tableMap.remove();
    }

    private void typeParser(Field field, String pathPrefix,
                            Class from, SchemaField parent, Map<String, Class> genericMap) {
        // TODO add filter
        if (field.getName().startsWith("_") || Modifier.isStatic(field.getModifiers())) {
            return;
        }
        SchemaField schemaField = new SchemaField()
                .setFieldType(FieldTypeEnum.UNKNOWN)
                .setFrom(from)
                .setParent(parent)
                .setSelfClass(field.getType())
                .setFieldName(field.getName())
                .setPath(resolve(pathPrefix, field.getName()));
        String genericTypeString = field.getGenericType().toString();
        Class genericClazz = genericMap.get(genericTypeString);
        genericClazz = (genericClazz == null ? field.getType() : genericClazz);
        annotationPlugin(field, schemaField);
        if (ignore(schemaField)) {
            return;
        }
        BasicTypeEnum basicType = BasicTypeEnum.anyMatch(field.getType());
        if (Object.class == genericClazz) {
            schemaField.setWarn("UndefinedGenericType<" + genericTypeString + ">");
        } else if (basicType != BasicTypeEnum.UNKNOWN) {
            schemaField.setFieldType(FieldTypeEnum.OPTIONAL);
            schemaField.setSqlType(basicType.getSqlType());
        } else if (isJdkInternal(genericClazz)) {
            schemaField.setFieldType(FieldTypeEnum.OPTIONAL);
        } else if (Iterable.class.isAssignableFrom(field.getType())) {
            schemaField.setFieldType(FieldTypeEnum.ITERABLE);
            String cannotResolve = iterableGenericTypeParser(field, schemaField, genericMap, schemaField.getPath());
            if (StringUtils.isNotBlank(cannotResolve)) {
                schemaField.setWarn(cannotResolve);
            }
        } else if (Map.class.isAssignableFrom(field.getType())) {
            schemaField.setFieldType(FieldTypeEnum.MAP);
            String cannotResolve = mapGenericTypeParser(field, schemaField, genericMap, schemaField.getPath());
            if (StringUtils.isNotBlank(cannotResolve)) {
                schemaField.setWarn(cannotResolve);
            }
        } else if (Enum.class.isAssignableFrom(field.getType())) {
            schemaField.setWarn("ENUM");
            schemaField.setFieldType(FieldTypeEnum.NOT_SUPPORT);
        } else {
            // 忽略非Map或者Iterable的接口
            if (field.getType().isInterface()) {
                return;
            }
            // 不为基础类型，不为集合类型，说明是用户定义的其他类型
            try {
                if (this.flat) {
                    loadClass(genericClazz, pathPrefix, parent, new HashMap<>());
                    return;
                } else {
                    loadClass(genericClazz, schemaField.getPath(), schemaField, new HashMap<>());
                }
                schemaField.setFieldType(FieldTypeEnum.OPTIONAL);
            } catch (Exception e) {
                System.out.println("Crash at " + field.getName());
                LOGGER.error("load class failed, cannot find class:{}" + field.getName(), e);
            }
        }
        if (StringUtils.isNotBlank(schemaField.getWarn())) {
            schemaField.setFieldType(FieldTypeEnum.NOT_SUPPORT);
        }
        this.fieldPathMap.put(schemaField.getPath(), schemaField);
    }

    public Map<String, Class> generateSuperclassGenericMap(Class clazz)
            throws ClassNotFoundException {
        Map<String, Class> fieldGenericMap = new HashMap<>(4);
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

    /**
     * 主要用于解析List、Set类型的泛型
     */
    private String iterableGenericTypeParser(Field field, SchemaField parent,
                                             Map<String, Class> genericMap, String pathPrefix) {
        Type type = field.getGenericType();
        if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {
            return undefineWarn("Empty");
        }
        Type genericType = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (ResolvableType.forType(genericType).getRawClass() == Object.class) {
            return undefineWarn("Object");
        }
        String resolve = genericTypeParser(genericType, pathPrefix, parent, genericMap, "#val");
        return undefineWarn(resolve);
    }


    private String mapGenericTypeParser(Field field, SchemaField parent,
                                        Map<String, Class> genericMap, String pathPrefix) {
        Type type = field.getGenericType();
        if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {
            return "Empty";
        }
        Type[] genericTypes = ((ParameterizedType) type).getActualTypeArguments();
        Type genericKeyType = genericTypes[0];
        Type genericValueType = genericTypes[1];
        String undefinedKey;
        String undefinedValue;
        if (ResolvableType.forType(genericKeyType).getRawClass() == Object.class) {
            undefinedKey = "Object";
        } else {
            undefinedKey = genericTypeParser(genericKeyType, pathPrefix, parent, genericMap, "#key");
        }
        if (ResolvableType.forType(genericValueType).getRawClass() == Object.class) {
            undefinedValue = "Object";
        } else {
            undefinedValue = genericTypeParser(genericValueType, pathPrefix, parent, genericMap, "#val");

        }
        return undefineWarn(undefinedKey, undefinedValue);
    }

    private String genericTypeParser(Type genericType, String pathPrefix,
                                     SchemaField parent, Map<String, Class> genericMap, String basicFieldTag) {
        String genericClazzName = genericType.toString();
        Class actualGenericClazz = genericMap.get(genericType.getTypeName());
        Class genericClazz;
        if (actualGenericClazz != null) {
            genericClazz = ResolvableType.forType(actualGenericClazz).getRawClass();
        } else {
            genericClazz = ResolvableType.forType(genericType).getRawClass();
        }
        if (genericClazz == null) {
            return genericClazzName;
        }
        BasicTypeEnum basicType = BasicTypeEnum.anyMatch(genericClazz);
        if (List.class.isAssignableFrom(genericClazz) || Map.class.isAssignableFrom(genericClazz)) {
            return nestedWarn(genericClazz.getSimpleName());
        } else if (basicType == BasicTypeEnum.UNKNOWN) {
            try {
                loadClass(genericClazz, pathPrefix, parent, new HashMap<>());
            } catch (ClassNotFoundException e) {
                LOGGER.error("load class failed, cannot find class:{}" + genericClazzName, e);
            }
        } else {
            SchemaField schemaField = new SchemaField()
                    .setFieldType(FieldTypeEnum.OPTIONAL)
                    .setFrom(parent.getSelfClass())
                    .setParent(parent)
                    .setSelfClass(genericClazz)
                    .setFieldName(basicFieldTag)
                    .setPath(resolve(pathPrefix, basicFieldTag))
                    .setSqlType(basicType.getSqlType());
            this.fieldPathMap.put(schemaField.getPath(), schemaField);
        }
        return "";
    }

    private void loadClass(Class clazz, final String pathPrefix,
                           SchemaField parent, Map<String, Class> genericMap) throws ClassNotFoundException {
        if (top(clazz)) {
            return;
        }
        final String clazzPath = clazz.getCanonicalName();
        Class superClazz = clazz;
        while (true) {
            Map<String, Class> superclassGenericMap = generateSuperclassGenericMap(superClazz);
            superClazz = superClazz.getSuperclass();
            if (superClazz == null || top(superClazz)) {
                break;
            }
            for (Field field : superClazz.getDeclaredFields()) {
                typeParser(field, pathPrefix, superClazz, parent, superclassGenericMap);
            }
            this.loadedClassName.add(superClazz.getCanonicalName());
        }
        for (Field field : clazz.getDeclaredFields()) {
            typeParser(field, pathPrefix, clazz, parent, genericMap);
        }
        this.loadedClassName.add(clazzPath);
    }

    private boolean top(Class clazz) {
        return clazz == Object.class || Enum.class.isAssignableFrom(clazz) || clazz.isInterface();
    }

    private boolean ignore(SchemaField schemaField) {
        // 忽略ASM动态注入的字段
        return schemaField.getJsonFieldName().startsWith("$");
    }

    private void annotationPlugin(Field field, SchemaField schemaField) {
        JSONField jsonFieldAnnotation = field.getAnnotation(JSONField.class);
        if (jsonFieldAnnotation != null && StringUtils.isNotBlank(jsonFieldAnnotation.name())) {
            schemaField.setJsonFieldName(jsonFieldAnnotation.name());
        } else {
            schemaField.setJsonFieldName(field.getName());
        }
        if (field.getAnnotation(KeyMark.class) != null) {
            schemaField.setMarkAsKey(true);
        }
    }

    private void loadClass(final String clazzPath, final String pathPrefix,
                           SchemaField parent) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(clazzPath);
        loadClass(clazz, pathPrefix,  parent, new HashMap<>());
    }

    private String resolve(String prefix, String path) {
        if (StringUtils.isNotBlank(prefix)) {
            return prefix + SPLITTER + path;
        }
        return path;
    }

    private boolean isJdkInternal(Class clazz) {
        String genericInfo = clazz.toGenericString();
        return genericInfo.contains(CLASS_FLAG + "java.") ||
                genericInfo.contains(CLASS_FLAG + "sun.");
    }

    private String undefineWarn(String... types) {
        List<String> undefineList = Arrays.stream(types)
                .filter(StringUtils::isNoneBlank)
                .collect(Collectors.toList());
        String res = Joiner.on(",").join(undefineList);
        if (StringUtils.isNotBlank(res)) {
            if (res.contains("")) {
                return "_NestedCollectionType<" + Joiner.on(",").join(undefineList) + ">";
            } else {
                return "_UndefinedGenericType<" + Joiner.on(",").join(undefineList) + ">";
            }
        }
        return "";
    }

    private String nestedWarn(String type) {
        return "" + type;
    }

    /**
     * FIXME 循环依赖检测，现在如果读取的类有循环依赖会导致死循环
     */
    private void circularReferenceCheck() {

    }

    public String getTag() {
        return tag;
    }
}
