package net.ion.craken.node.crud.tree.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;

import org.infinispan.io.GridFilesystem;

public class GridBlob implements Serializable {

	private static final long serialVersionUID = -8661240636213998157L;
	private transient GridFilesystem gfs;
	private String path;

	private GridBlob(GridFilesystem gfs, String path) {
		this.gfs = gfs ;
		this.path = path ;
	}

	public final static GridBlob create(GridFilesystem gfs, String path){
		return new GridBlob(gfs, path) ;
	}

	public static GridBlob read(String expression) {
		return new GridBlob(null, StringUtil.substringBetween(expression, "[BLOB:", "]"));
	}
	
	public String path() {
		return path;
	}
	
	public JsonPrimitive toJsonPrimitive(){
		return new JsonPrimitive("[BLOB:" + path + "]") ;
	}

	public InputStream toInputStream() throws FileNotFoundException {
//		gfs.getInput("/")
		return gfs.getInput(path);
	}
	
	public File toFile(){
		return gfs.getFile(path) ;
	}

	public GridBlob gfs(GridFilesystem gfs) {
		this.gfs = gfs ;
		return this;
	}

	public GridBlob saveAt(InputStream input) throws IOException {
		File file = gfs.getFile(path);
		
		if (file.exists()) file.delete() ;
		if (! file.getParentFile().exists()) file.getParentFile().mkdirs();
		file.createNewFile();
		
		IOUtil.copyNClose(input, gfs.getOutput(path));
		return this ;
	}

	public PropertyValue asPropertyValue() {
		return PropertyValue.createPrimitive(this);
	}

	public OutputStream toOutputStream() throws IOException {
		return gfs.getOutput(path) ;
	}

}
