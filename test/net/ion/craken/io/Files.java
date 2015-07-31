package net.ion.craken.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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
	

	public final static void walkFileJDK7(File parent, final FileVisitor fvisitor) throws IOException{
	
		Path start = parent.toPath() ;
		
		java.nio.file.FileVisitor<? super Path> visitor = new SimpleFileVisitor<Path>() {
		    public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException{
		    	fvisitor.visitFile(file.toFile());
		    	return java.nio.file.FileVisitResult.CONTINUE ;
		    }
		};
		java.nio.file.Files.walkFileTree(start, visitor) ;
	}
	
}
