package net.ion.craken.node.search;

import java.io.IOException;

import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.store.Directory;

public class MyCentralConfig extends CentralConfig {

	
	private Directory dir;
	public MyCentralConfig(Directory dir) {
		this.dir = dir ;
	}

	public static CentralConfig create(Directory dir) {
		return new MyCentralConfig(dir);
	}

	@Override
	public Directory buildDir() throws IOException {
		return dir;
	}

}
