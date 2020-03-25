package com.xiongbeer.waterdrops.schema;

import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

public enum BasicTypeEnum {
    STRING(String.class, "", SqlTypeName.VARCHAR), LONG(Long.class, "long", SqlTypeName.BIGINT),
    SHORT(Short.class, "short", SqlTypeName.SMALLINT), INT(Integer.class, "int", SqlTypeName.INTEGER),
    DOUBLE(Double.class, "double", SqlTypeName.DOUBLE), FLOAT(Float.class, "float", SqlTypeName.FLOAT),
    BYTE(Byte.class, "byte", SqlTypeName.TINYINT), CHAR(Character.class, "char", SqlTypeName.VARCHAR),
    BOOLEAN(Boolean.class, "boolean", SqlTypeName.BOOLEAN), TIMESTAMP(Date.class, "", SqlTypeName.VARCHAR),
    UNKNOWN(Object.class, "", SqlTypeName.ANY);

    private Class<?> clazz;
    private String basicName;
    private SqlTypeName sqlType;

    BasicTypeEnum(Class<?> clazz, String basicName, SqlTypeName sqlType) {
        this.clazz = clazz;
        this.basicName = basicName;
        this.sqlType = sqlType;
    }

    public static BasicTypeEnum anyMatch(Class clazz) {
        for (BasicTypeEnum basicType : values()) {
            if (clazz.equals(basicType.clazz)) {
                return basicType;
            }
            if (StringUtils.isNoneBlank(basicType.basicName) && basicType.basicName.equals(clazz.getCanonicalName())) {
                return basicType;
            }
        }
        return UNKNOWN;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getBasicName() {
        return basicName;
    }

    public SqlTypeName getSqlType() {
        return sqlType;
    }
}
