package com.xiongbeer.waterdrops.schema;

import java.util.List;

public class SchemaConfigurer {
    private ClassResolver classResolver;
    private FieldResolver fieldResolver;
    private TypeResolver typeResolver;

    public void fromName(String canonicalClassName) throws ClassNotFoundException {
        fromClass(Class.forName(canonicalClassName));
    }

    public Schema fromClass(Class<?> clazz) throws ClassNotFoundException {
        Resolver resolver = new Resolver()
                .setClassResolver(this.classResolver)
                .setFieldResolver(this.fieldResolver)
                .setTypeResolver(this.typeResolver);
        Schema schema = new Schema("test");
        List<SchemaField> fields = this.classResolver.resolveClass(clazz, ScopeContext.empty(), resolver);
        fields.forEach(field -> schema.getFieldPathMap().put(field.getPath(), field));
        return schema;
    }

    public static SchemaConfigurer defaultInstance() {
        return new SchemaConfigurer()
                .setClassResolver(new DefaultClassResolver())
                .setFieldResolver(new DefaultFieldResolver())
                .setTypeResolver(new DefaultTypeResolver());
    }

    public SchemaConfigurer setClassResolver(ClassResolver classResolver) {
        this.classResolver = classResolver;
        return this;
    }

    public SchemaConfigurer setFieldResolver(FieldResolver fieldResolver) {
        this.fieldResolver = fieldResolver;
        return this;
    }

    public SchemaConfigurer setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
        return this;
    }
}
