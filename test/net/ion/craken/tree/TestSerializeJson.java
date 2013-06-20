package net.ion.craken.tree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

public class TestSerializeJson extends TestCase {

	public void testWrite() throws Exception {
		PropertyValue sjson = PropertyValue.createPrimitive(new Date());
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(bout);
		
		oout.writeObject(sjson) ;
		
		ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
		PropertyValue read = (PropertyValue) oin.readObject() ;
		
		Debug.line(read.value()) ;
	}
}
