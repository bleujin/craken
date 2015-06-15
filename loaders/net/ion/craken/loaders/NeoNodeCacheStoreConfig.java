package net.ion.craken.loaders;

import net.ion.neo.Credential;
import net.ion.neo.NeoRepository;
import net.ion.neo.ReadSession;

import org.apache.lucene.analysis.Analyzer;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheLoaderException;

public class NeoNodeCacheStoreConfig extends AbstractCacheStoreConfig {

	private static final long serialVersionUID = -6047637725057663352L;

	private String accessKey;
	private String secretKey ;
	private String wsName ;
	private String analyzerClzName ;

	private NeoRepository rep = new NeoRepository();

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getWsName() {
		return wsName;
	}

	public void setWsName(String wsName) {
		this.wsName = wsName;
	}

	public String getAnalyzerClzName() {
		return analyzerClzName;
	}

	public void setAnalyzerClzName(String analyzerClzName) {
		this.analyzerClzName = analyzerClzName;
	}

	@Override
	public String getCacheLoaderClassName() {
		return NeoNodeCacheStore.class.getName();
	}

	public Credential getCredential() {
		return new Credential(accessKey, secretKey);
	}

	public ReadSession login() throws CacheLoaderException {
		try {
			final ReadSession session = rep.login(getCredential(), getWsName(), (Class<? extends Analyzer>) Class.forName(getAnalyzerClzName()));
			return session;
		} catch (ClassNotFoundException e) {
			throw new CacheLoaderException(e) ; 
		}
	}
	
	
}

