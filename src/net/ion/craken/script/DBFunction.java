package net.ion.craken.script;

import net.ion.craken.tree.PropertyValue;

public class DBFunction {

    private static final String JS_UNDEFINED = "undefined";

    public Object nvl(Object value, Object absentValue) {

        if (value == null) {
            if(absentValue instanceof String) {
                return String.valueOf(absentValue);
            } else if(absentValue instanceof Integer) {
                return Integer.valueOf(String.valueOf(absentValue));
            }
        }

        return value;
    }

    public Integer asInt(PropertyValue value) {
        return Integer.valueOf(value.asInt());
    }
}