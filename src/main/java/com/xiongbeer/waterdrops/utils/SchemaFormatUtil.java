package com.xiongbeer.waterdrops.utils;

import com.xiongbeer.waterdrops.schema.Schema;
import com.xiongbeer.waterdrops.schema.SchemaField;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SchemaFormatUtil {
    private SchemaFormatUtil() {

    }

    public static String asSelfDescribingString(Schema schema) {
        StringBuilder dsl = new StringBuilder(schema.getTag() + " {" + System.lineSeparator());
        Map<SchemaField, List<SchemaField>> levelMap = new LinkedHashMap<>();
        schema.getFieldPathMap().forEach((path, field) ->
                levelMap.computeIfAbsent(field.getParent(), v -> new ArrayList<>()).add(field));
        asSelfDescribingString(dsl, null, levelMap, "");
        return dsl.append("}").toString();
    }

    private static void asSelfDescribingString(StringBuilder dsl, SchemaField key, Map<SchemaField,
            List<SchemaField>> levelMap, final String prefix) {
        final String tab = key == null ? "" : prefix + "\t";
        List<SchemaField> childList = levelMap.get(key);
        if (CollectionUtils.isNotEmpty(childList)) {
            if (key != null) {
                final String warn = key.getWarn() == null ? "" : key.getWarn() + " ";
                dsl.append(tab).append(warn).append(key.getFieldName()).append(" ").append(key.getFieldType())
                        .append(" AS LEVELED TABLE").append(" {").append(System.lineSeparator());
            }
            for (SchemaField field : childList) {
                asSelfDescribingString(dsl, field, levelMap, tab);
            }
            if (key != null) {
                dsl.append(tab).append("}").append(System.lineSeparator());
            }
        } else if (key != null) {
            final String sqlType = key.getSqlType() != null ? key.getSqlType().getName() : "";
            final String markKey = key.isMarkAsKey() ? " AS MARK KEY" : "";
            final String warn = key.getWarn() == null ? "" : key.getWarn();
            dsl.append(tab).append(warn).append(sqlType).append(" ")
                    .append(key.getFieldName()).append(" ").append(key.getFieldType())
                    .append(markKey).append(";").append(System.lineSeparator());
        }
    }
}
