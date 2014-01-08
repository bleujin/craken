package net.ion.craken.node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import junit.framework.TestCase;

public class TestWriter extends TestCase {

	public void testWriteFirst() throws Exception {
		File file = File.createTempFile("pppp", "xxxx");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
		
		for (int i = 0; i < 5; i++) {
			writer.write("hello" + i, 0, 6) ;
		}
		writer.close() ;
		
		
		String result = IOUtil.toString(file.toURI()) ;
		Debug.line(result) ;
	}
	
}
