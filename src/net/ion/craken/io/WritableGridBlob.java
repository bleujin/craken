package net.ion.craken.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class WritableGridBlob extends GridBlob {


	WritableGridBlob(String pathname, Metadata metadata, GridFilesystem gfs) {
		super(pathname, metadata, gfs) ;
	}


	public GridOutputStream outputStream() throws IOException{
		return super.outputStream() ;
	}


	public void delete() {
		super.delete(getMetadata()) ;
	}


}
