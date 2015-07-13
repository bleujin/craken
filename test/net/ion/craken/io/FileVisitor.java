package net.ion.craken.io;

import java.io.File;
import java.io.IOException;


public interface FileVisitor {

	enum FileVisitResult {
		CONTINUE, TERMINATE ;
	}

	public FileVisitResult visitFile(File file) throws IOException ;
}
