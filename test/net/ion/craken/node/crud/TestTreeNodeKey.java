package net.ion.craken.node.crud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestTreeNodeKey extends TestCase{
	
	public void testSerial() throws Exception {
		TreeNodeKey tkey = TreeNodeKey.fromString("/emp/bleujin") ;
		ByteArrayOutputStream bout = new ByteArrayOutputStream() ;
		ObjectOutputStream oout = new ObjectOutputStream(bout) ;
		oout.writeObject(tkey);
		oout.flush(); 
		
		byte[] bytes = bout.toByteArray() ;
		ObjectInputStream oins = new ObjectInputStream(new ByteArrayInputStream(bytes)) ;
		TreeNodeKey readed = (TreeNodeKey) oins.readObject() ;
		
//		ObjectOutputStream nout = new ObjectOutputStream(new ByteArrayOutputStream());
//		new TreeNodeKey.Externalizer().writeObject(nout, tkey);
		
		
		Debug.line(readed, bytes.length, bytes);
		
	}

}
