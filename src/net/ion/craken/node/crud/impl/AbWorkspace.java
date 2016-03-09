package net.ion.craken.node.crud.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.ion.craken.node.crud.tree.impl.GridBlob;
import net.ion.framework.util.IOUtil;

import org.infinispan.batch.AutoBatchSupport;
import org.infinispan.io.GridFilesystem;

public abstract class AbWorkspace extends AutoBatchSupport{


	public InputStream toInputStream(GridBlob gblob) throws FileNotFoundException {
//		gfs.getInput("/")
		return gfs().getInput(gblob.path());
	}
	
	public File toFile(GridBlob gblob){
		return gfs().getFile(gblob.path()) ;
	}

	public GridBlob saveAt(GridBlob gblob, InputStream input) throws IOException {
		File file = gfs().getFile(gblob.path());
		
		if (file.exists()) file.delete() ;
		if (! file.getParentFile().exists()) file.getParentFile().mkdirs();
		file.createNewFile();
		
		IOUtil.copyNClose(input, gfs().getOutput(gblob.path()));
		return gblob ;
	}

	public abstract GridFilesystem gfs() ;

	public OutputStream toOutputStream(GridBlob gblob) throws IOException {
		return gfs().getOutput(gblob.path()) ;
	}

	
}
