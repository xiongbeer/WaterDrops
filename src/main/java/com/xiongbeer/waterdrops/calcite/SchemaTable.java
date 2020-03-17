package com.xiongbeer.waterdrops.calcite;

import com.google.common.base.Preconditions;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Pair;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SchemaTable extends AbstractTable implements ScannableTable {
    private List<Map<String, Object>> rows = new ArrayList<>();
    private List<String> columnNameList;
    private List<SqlTypeName> columnTypeList;
    private final String tableName;

    public SchemaTable(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public Enumerable<Object[]> scan(DataContext dataContext) {
        final TableEnumerator<Object[]> enumerator = new TableEnumerator<>(this.rows);
        return new AbstractEnumerable<Object[]>() {
            @Override
            public Enumerator<Object[]> enumerator() {
                return enumerator;
            }
        };
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory relDataTypeFactory) {
        List<RelDataType> types = this.columnTypeList.stream()
                .map(relDataTypeFactory::createSqlType)
                .collect(Collectors.toList());
        return relDataTypeFactory.createStructType(Pair.zip(this.columnNameList, types));
    }

    public void setupFieldList(List<String> names, List<SqlTypeName> types) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(names));
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(types));
        Preconditions.checkArgument(names.size() == types.size());
        this.columnNameList = names;
        this.columnTypeList = types;
    }

    public void apply(List<Map<String, Object>> rows) {
        this.rows.addAll(rows);
    }

    public String getTableName() {
        return tableName;
    }
}
