package net.ion.craken.io;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import net.ion.craken.io.FileVisitor.FileVisitResult;
import net.ion.framework.util.Debug;

import org.apache.commons.io.FilenameUtils;

public class Files extends TestCase{

	public final static void walkFileTree(File parent, FileVisitor fvisitor) throws IOException{
		FileVisitResult result = fvisitor.visitFile(parent) ;
		if (result == FileVisitResult.TERMINATE) return ;
		
		if (parent.isFile()) return ;
		File[] files = parent.listFiles() ;
		for (File child : files) {
			walkFileTree(parent, fvisitor);
		}
	}
	

}
