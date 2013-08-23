package net.ion.craken.loaders;

import org.infinispan.loaders.AbstractCacheStoreConfig;

public class MongoDBCacheStoreConfig extends AbstractCacheStoreConfig {

	private static final long serialVersionUID = -6047637725057663352L;
	private String host;
	private int port = 27017;
	private String dbName;
	private String dbCollection;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getDbCollection() {
		return dbCollection;
	}

	public void setDbCollection(String dbCollection) {
		this.dbCollection = dbCollection;
	}

	@Override
	public String getCacheLoaderClassName() {
		return MongoDBCacheStore.class.getName();
	}
}
