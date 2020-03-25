package com.xiongbeer.waterdrops.schema;

public class Resolver {
    private ClassResolver classResolver;
    private FieldResolver fieldResolver;
    private TypeResolver typeResolver;

    public ClassResolver getClassResolver() {
        return classResolver;
    }

    public Resolver setClassResolver(ClassResolver classResolver) {
        this.classResolver = classResolver;
        return this;
    }

    public FieldResolver getFieldResolver() {
        return fieldResolver;
    }

    public Resolver setFieldResolver(FieldResolver fieldResolver) {
        this.fieldResolver = fieldResolver;
        return this;
    }

    public TypeResolver getTypeResolver() {
        return typeResolver;
    }

    public Resolver setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
        return this;
    }
}
