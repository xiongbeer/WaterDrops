package com.xiongbeer.waterdrops.schema;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ScopeContext {
    private Map<String, Object> context = new HashMap<>();
    private String pathPrefix;
    private SchemaField parent;
    private Map<String, Class<?>> genericMap = new HashMap<>();

    public static ScopeContext empty() {
        return new ScopeContext();
    }

    public ScopeContext shallowCopy() {
        ScopeContext instance = new ScopeContext();
        instance.context = new HashMap<>(this.context);
        instance.pathPrefix = this.pathPrefix;
        instance.parent = this.parent;
        instance.genericMap = new HashMap<>(this.genericMap);
        return instance;
    }

    public ScopeContext clearGenericMap() {
        this.genericMap.clear();
        return this;
    }

    public static String resolvePath(final String prefix, final String path) {
        if (StringUtils.isBlank(prefix)) {
            return path;
        }
        return prefix + "." + path;
    }

    public String resolvePath(final String path) {
        if (StringUtils.isBlank(this.pathPrefix)) {
            return path;
        }
        return this.pathPrefix + "." + path;
    }

    public Object get(String key) {
        return this.context.get(key);
    }

    public void input(String key, Object val) {
        this.context.put(key, val);
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public ScopeContext setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
        return this;
    }

    public SchemaField getParent() {
        return parent;
    }

    public ScopeContext setParent(SchemaField parent) {
        this.parent = parent;
        return this;
    }

    public Map<String, Class<?>> getGenericMap() {
        return genericMap;
    }

    public ScopeContext setGenericMap(Map<String, Class<?>> genericMap) {
        this.genericMap = genericMap;
        return this;
    }
}
