package com.xiongbeer.waterdrops;


import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author liushaoxiong on 2020/3/10.
 */
public class SchemaField {
    private String path;
    private String jsonFieldName;
    private String fieldName;
    private FieldTypeEnum fieldType;
    /**
     * 来源于哪个类
     */
    private Class from;
    private Class selfClass;
    private SchemaField parent;
    private boolean markAsKey = false;
    private SqlTypeName sqlType;
    private String warn;

    // TODO 支持map
    public Object get(Object object, String offset) throws NoSuchFieldException, IllegalAccessException {
        if (object instanceof Iterable) {
            List<Object> res = new ArrayList<>();
            for (Object o : (Iterable) object) {
                res.add(getByPath(o, offset));
            }
            return res;
        }
        return getByPath(object, offset);
    }

    private Object getByPath(Object object, String offset) throws NoSuchFieldException, IllegalAccessException {
        String[] pointers;
        if (StringUtils.isNotBlank(offset) && this.path.startsWith(offset)) {
            final String relativelyPath = this.path.substring(offset.length() + 1);
            pointers = relativelyPath.split("\\.");
        } else {
            pointers = this.path.split("\\.");
        }
        Map<String, Field> fieldMap = new HashMap<>();
        Class<?> clazz = object.getClass();
        while (!clazz.equals(Object.class)) {
            for (Field field : clazz.getDeclaredFields()) {
                fieldMap.put(field.getName(), field);
            }
            clazz = clazz.getSuperclass();
        }
        String head = pointers[0];
        Object res;
        if (fieldMap.containsKey(head)) {
            Field field = fieldMap.get(head);
            field.setAccessible(true);
            res = field.get(object);
            clazz = field.getDeclaringClass();
        } else {
            throw new NoSuchFieldException(head);
        }
        if (pointers.length > 1) {
            for (int i = 1; i < pointers.length; ++i) {
                final String name = pointers[i];
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                res = field.get(res);
                clazz = field.getDeclaringClass();
            }
        }
        return res;
    }

    public String getPath() {
        return path;
    }

    public SchemaField setPath(String path) {
        this.path = path;
        return this;
    }

    public String getJsonFieldName() {
        return jsonFieldName;
    }

    public SchemaField setJsonFieldName(String jsonFieldName) {
        this.jsonFieldName = jsonFieldName;
        return this;
    }

    public String getFieldName() {
        return fieldName;
    }

    public SchemaField setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public FieldTypeEnum getFieldType() {
        return fieldType;
    }

    public SchemaField setFieldType(FieldTypeEnum fieldType) {
        this.fieldType = fieldType;
        return this;
    }

    public Class getFrom() {
        return from;
    }

    public SchemaField setFrom(Class from) {
        this.from = from;
        return this;
    }

    public Class getSelfClass() {
        return selfClass;
    }

    public SchemaField setSelfClass(Class selfClass) {
        this.selfClass = selfClass;
        return this;
    }

    public SchemaField getParent() {
        return parent;
    }

    public SchemaField setParent(SchemaField parent) {
        this.parent = parent;
        return this;
    }

    public boolean isMarkAsKey() {
        return markAsKey;
    }

    public SchemaField setMarkAsKey(boolean markAsKey) {
        this.markAsKey = markAsKey;
        return this;
    }

    public SqlTypeName getSqlType() {
        return sqlType;
    }

    public SchemaField setSqlType(SqlTypeName sqlType) {
        this.sqlType = sqlType;
        return this;
    }

    public String getWarn() {
        return warn;
    }

    public SchemaField setWarn(String warn) {
        this.warn = warn;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SchemaField)) {
            return false;
        }
        SchemaField that = (SchemaField) o;
        return Objects.equals(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath());
    }
}