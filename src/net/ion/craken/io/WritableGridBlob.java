package net.ion.craken.io;

import java.io.IOException;

public class WritableGridBlob extends GridBlob {


	WritableGridBlob(String pathname, Metadata metadata, GridFilesystem gfs) {
		super(pathname, metadata, gfs) ;
	}


	public GridOutputStream outputStream() throws IOException{
		return super.outputStream() ;
	}


	public void delete() {
		super.delete(false) ;
	}


}
