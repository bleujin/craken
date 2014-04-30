package net.ion.ics6.core;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.craken.node.exception.NotFoundPath;
import net.ion.craken.script.DBFunction;
import net.ion.framework.util.Debug;

public class TestJustCraken extends TestBasePackage {

    public void testFirst() throws Exception {
        // given
        session.tranSync(new TransactionJob<Void>() {
            @Override
            public Void handle(WriteSession wsession) throws Exception {
                wsession.pathBy("/").property("");
                return null;
            }
        });

        // when

        // then
    }

    public void testScriptEngine() throws ScriptException, NoSuchMethodException {

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        String script = "function nvlTest(value) {\n" +
                "        return func.nvl(value, 'F');\n" +
                "    }";

        String script2 = "function test(value) { return value; }";

        engine.put("func", new DBFunction());
        engine.eval(script);
        engine.eval(script2);

        Object result = ((Invocable) engine).invokeFunction("nvlTest", null);
        Object result2 = ((Invocable) engine).invokeFunction("test", null);

        Debug.line(result);
        Debug.line(result2);
    }

    public void testRemoveFiltered() throws Exception {
        // given
        session.tranSync(new TransactionJob<Void>() {
            @Override
            public Void handle(WriteSession wsession) throws Exception {
                wsession.pathBy("/afield_rels/100/rels/afielda").property("lowerid", "A" ).property("upperid", "ROOT");
                wsession.pathBy("/afield_rels/100/rels/afieldb").property("lowerid", "B" ).property("upperid", "ROOT");
                wsession.pathBy("/afield_rels/100/rels/afieldc").property("lowerid", "C" ).property("upperid", "A");
                wsession.pathBy("/afield_rels/100/rels/afieldd").property("lowerid", "D" ).property("upperid", "C");

                return null;
            }
        });

        // when
        ChildQueryResponse response = session.pathBy("/afield_rels").childQuery("", true).eq("lowerid", "C").ne("upperid", "ROOT").find();
        final IteratorList<ReadNode> iterator = response.iterator();

        session.tranSync(new TransactionJob<Void>() {
            @Override
            public Void handle(WriteSession wsession) throws Exception {
                wsession.pathBy(iterator.next().fqn()).removeSelf();
                return null;
            }
        });

        // then
        try {
            session.pathBy("/afield_rels/100/rels/afieldc");
            fail();
        } catch(NotFoundPath e) {

        }
    }

    public void testGuavaFunction() throws ScriptException, NoSuchMethodException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        String script = "function nvlTest(value) {\n" +
                "        return func.nvl(value, 'F');\n" +
                "    }";

        engine.eval(script);

        Object result = ((Invocable) engine).invokeFunction("nvlTest", null);

        Debug.line(result);
    }
    
}
