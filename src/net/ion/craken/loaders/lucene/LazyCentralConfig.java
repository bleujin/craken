package net.ion.craken.loaders.lucene;

import java.io.IOException;

import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.store.Directory;

public class LazyCentralConfig extends CentralConfig{

	private Directory dir;

	public LazyCentralConfig dir(Directory dir){
		this.dir = dir ;
		return this ;
	}
	
	@Override
	public Directory buildDir() throws IOException {
		if (dir == null) throw new IllegalStateException("not setted dir") ;
		
		return dir;
	}

}
