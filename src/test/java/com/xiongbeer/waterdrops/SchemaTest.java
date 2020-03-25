package com.xiongbeer.waterdrops;

import com.xiongbeer.waterdrops.schema.Schema;
import com.xiongbeer.waterdrops.schema.SchemaConfigurer;
import com.xiongbeer.waterdrops.utils.SchemaFormatUtil;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SchemaTest {
    @Before
    public void initTestLogger() {
        Properties prop = new Properties();
        prop.setProperty("log4j.rootLogger", "Debug,CONSOLE");
        prop.setProperty("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
        prop.setProperty("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
        prop.setProperty("log4j.appender.CONSOLE.layout.ConversionPattern", "%d{HH:mm:ss,SSS} [%t] %-5p %C{1} : %m%n");
        PropertyConfigurator.configure(prop);
    }

    @Test
    public void testSimpleNested() throws ClassNotFoundException {
        Schema schema = SchemaConfigurer.defaultInstance().fromClass(SimpleNestedClass.class);
        final String s = SchemaFormatUtil.asSelfDescribingString(schema);
        System.out.println(s);
    }

    @Test
    public void testCollection() throws ClassNotFoundException {
        Schema it = SchemaConfigurer.defaultInstance().fromClass(IterableClass.class);
        String s = SchemaFormatUtil.asSelfDescribingString(it);
        System.out.println(s);
        Schema map = SchemaConfigurer.defaultInstance().fromClass(MapClass.class);
        s = SchemaFormatUtil.asSelfDescribingString(map);
        System.out.println(s);
    }

    @Test
    public void testInheritance() throws ClassNotFoundException {
        Schema it = SchemaConfigurer.defaultInstance().fromClass(ChildClass.class);
        String s = SchemaFormatUtil.asSelfDescribingString(it);
        System.out.println(s);
    }

    static class IterableClass {
        private List<String> basicList;
        private List<NormalClass> userDefinedList;
    }

    static class MapClass {
        private Map<String, Integer> basicMap;
        private Map<String, NormalClass> userDefinedValueMap;
    }

    static class NormalClass {
        private int val;
        private Integer val2;
        private String val3;
        private Double val4;
        private Byte val5;
    }

    static class SimpleNestedClass {
        private NormalClass normalClass;
        private Integer val;
    }

    static class ChildClass extends FatherClass {
        private int b;
        private String x;
    }

    static class FatherClass {
        private int a;
        private String x;
    }
}
