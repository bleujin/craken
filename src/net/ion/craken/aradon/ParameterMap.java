package net.ion.craken.aradon;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.fileupload.FileItem;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.radon.core.let.MultiValueMap;

public class ParameterMap {

	private MultiValueMap inner;
	private ParameterMap(MultiValueMap inner) {
		this.inner = inner ;
	}

	public static ParameterMap create(MultiValueMap inner){
		return new ParameterMap(inner) ;
	}
	
	public static ParameterMap create(JsonObject json){
		return new ParameterMap(MultiValueMap.create(json.toMap())) ;
	}
	
	public String asString(String name){
		return ObjectUtil.toString(inner.getFirstValue(name)) ;
	}
	
	public int asInt(String name){
		return NumberUtil.toInt(asString(name), 0) ;
	}

	public long asLong(String name){
		return NumberUtil.toLong(asString(name), 0L) ;
	}
	
	public FileItem asFileItem(String name){
		final Object item = inner.get(name);
		if (item instanceof FileItem){
			FileItem fitem = (FileItem) item;
			return fitem ;
		} else {
			throw new IllegalArgumentException(name + " : not fileitem") ;
		}
		
	}
	
	public InputStream asStream(String name) throws IOException{
		final Object item = inner.get(name);
		if (item instanceof FileItem){
			FileItem fitem = (FileItem) item;
			return fitem.getInputStream() ;
		} else {
			return new ByteArrayInputStream(asString(name).getBytes("UTF-8")) ;
		}
	}
	
	

	
	public String asString(String name, int index){
		
		return ObjectUtil.toString(inner.getAsList(name).get(index)) ;
	}
	
	public int asInt(String name, int index){
		return NumberUtil.toInt(asString(name, index), 0) ;
	}

	public long asLong(String name, int index){
		return NumberUtil.toLong(asString(name, index), 0L) ;
	}
	

	public String[] asStrings(String name){
		return (String[]) inner.getAsList(name).toArray(new String[0]) ;
	}

	
	

}
