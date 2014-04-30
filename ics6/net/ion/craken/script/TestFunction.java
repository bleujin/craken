package net.ion.craken.script;

import junit.framework.TestCase;

public class TestFunction extends TestCase {

    private DBFunction func = new DBFunction();

    public void testNVL_String() {
        // given
        String value = null;

        // when
        Object nvl = func.nvl(value, "test");
        Object nvl2 = func.nvl("test2", "test3");

        // then
        assertEquals("test", nvl);
        assertEquals("test2", nvl2);
    }
}


