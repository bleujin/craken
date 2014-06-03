package net.ion.craken.loaders;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LegacyStoreConfigurationBuilder;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheStore;

public abstract class WorkspaceConfig extends AbstractCacheStoreConfig  {

	public final static String Location = "location";
	public final static String MaxEntries = "maxEntries";
	public final static String ChunkSize = "chunkSize";

	private static final long serialVersionUID = -449909701034789210L;

	public abstract String location();
	public abstract int maxNodeEntry();
	public abstract int lockTimeoutMs();
	
	private Map<String, String> otherProps = MapUtil.newMap();
	
	public Map<String, String> otherProps(){
		return Collections.unmodifiableMap(otherProps) ;
	}
	
	public WorkspaceConfig otherProp(String key, String value){
		otherProps.put(key, value) ;
		return this ;
	}
	
	public String defaultValue(String key, String dftString){
		return ObjectUtil.coalesce(otherProps.get(key), dftString) ;
	}
	

	public abstract Workspace createWorkspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName) throws IOException ;

	
	private LegacyStoreConfigurationBuilder setProps(LegacyStoreConfigurationBuilder builder){
		for (Entry<String, String> entry : otherProps().entrySet()) {
			builder.addProperty(entry.getKey(), entry.getValue()) ;
		}
		return builder ;
	}
	
	
	public Configuration build() throws IOException {
		try {
			CacheStore cloader = (CacheStore) Class.forName(getCacheLoaderClassName()).newInstance();
			ConfigurationBuilder cbuilder = new ConfigurationBuilder();
			
			this.setProps(cbuilder.loaders().addStore()
				.cacheStore(cloader).addProperty(Location, location())).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(false).async().enabled(false) ;

//			dm.defineConfiguration(wsName, new ConfigurationBuilder()
//			.clustering().hash().numOwners(2).clustering().cacheMode(CacheMode.DIST_SYNC).invocationBatching().enable()
//			.eviction().maxEntries(config.maxNodeEntry()).transaction().syncCommitPhase(true).syncRollbackPhase(true)
//			.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
//			.loaders().preload(false).shared(false).passivation(false).addCacheLoader().cacheLoader(cloader).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async()
//			.enabled(false).build());
			
//			cbuilder.loaders().addStore().addProperty(Location, location()) ;
			
			return cbuilder.clustering().hash().numOwners(2).clustering().cacheMode(CacheMode.DIST_SYNC)
			.invocationBatching().enable().eviction().eviction().maxEntries(maxNodeEntry()).transaction().syncCommitPhase(true)
			.syncRollbackPhase(true).locking().lockAcquisitionTimeout(lockTimeoutMs())
			.loaders().preload(true).shared(false).passivation(false).build() ;
		} catch (ClassNotFoundException ex) {
			throw new IOException(ex);
		} catch (InstantiationException ex) {
			throw new IOException(ex);
		} catch (IllegalAccessException ex) {
			throw new IOException(ex);
		}
	}

	public Configuration buildLocal() throws IOException {
		try {
			CacheStore cloader = (CacheStore) Class.forName(getCacheLoaderClassName()).newInstance();
			ConfigurationBuilder cbuilder = new ConfigurationBuilder();
			
			this.setProps(cbuilder.loaders().addStore()
				.cacheStore(cloader).addProperty(Location, location())).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false) ;

//			cbuilder.loaders().addStore().addProperty(Location, location()) ;
			
			return cbuilder.clustering().cacheMode(CacheMode.LOCAL)
			.invocationBatching().enable().eviction().eviction().maxEntries(maxNodeEntry()).transaction().syncCommitPhase(true)
			.syncRollbackPhase(true).locking().lockAcquisitionTimeout(lockTimeoutMs())
			.loaders().preload(true).shared(false).passivation(false).build() ;
		} catch (ClassNotFoundException ex) {
			throw new IOException(ex);
		} catch (InstantiationException ex) {
			throw new IOException(ex);
		} catch (IllegalAccessException ex) {
			throw new IOException(ex);
		}
	}

}
