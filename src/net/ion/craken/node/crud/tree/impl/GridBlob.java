package net.ion.craken.node.crud.tree.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import net.ion.craken.node.Workspace;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;

import org.infinispan.io.GridFilesystem;

public class GridBlob implements Serializable {

	private static final long serialVersionUID = -8661240636213998157L;
	private String path;
	private transient Workspace workspace;

	private GridBlob(Workspace workspace, String path) {
		this.workspace = workspace ;
		this.path = path ;
	}

	public final static GridBlob create(Workspace workspace, String path){
		return new GridBlob(workspace, path) ;
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

	public PropertyValue asPropertyValue() {
		return PropertyValue.createPrimitive(this);
	}


	
	
	public InputStream toInputStream() throws FileNotFoundException {
		return workspace.toInputStream(this) ;
	}
	
	public File toFile(){
		return workspace.toFile(this) ;
	}

	public GridBlob workspace(Workspace workspace) {
		this.workspace = workspace ;
		return this;
	}

	public GridBlob saveAt(InputStream input) throws IOException {
		return workspace.saveAt(this, input) ;
	}

	public OutputStream toOutputStream() throws IOException {
		return workspace.toOutputStream(this) ;
	}

}
