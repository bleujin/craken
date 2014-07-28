package net.ion.craken;

import java.util.Map;

import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class Craken {

	private GlobalConfigurationBuilder globalBuilder ;
	private ConfigurationBuilder defaultConf ;
	private DefaultCacheManager dftManager ;
	private boolean started = false ;
	private CrakenInfo myInfo = CrakenInfo.NOT_YET;
	
	private Map<String, Configuration> preDefinedConfig = MapUtil.newSyncMap();
	private Map<String, LegContainer> containers = MapUtil.newSyncMap() ;
	
	
	private Craken(){
		this.globalBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder() ;
		this.defaultConf = new ConfigurationBuilder() ;
		defaultConf.clustering().cacheMode(CacheMode.DIST_ASYNC).clustering().l1().enable().lifespan(6000000).invocationBatching().clustering().hash().numOwners(2) ;
	}
	
	public final static Craken create(){
		return new Craken() ;
	}
	
	public GlobalConfigurationBuilder globalConfig(){
		return globalBuilder ;
	}

	public ConfigurationBuilder defineDefault() {
		return defaultConf ;
	}

	public synchronized Craken start() {
		
		if (started) {
			Debug.warn("already started") ;
			return this ;
		}
		Debug.warn("craken starting") ;

		this.dftManager = new DefaultCacheManager(globalBuilder.build(), defaultConf.build(), true) ;
		dftManager.start() ;
		
		LegContainer<CrakenInfo> leg = defineLeg(CrakenInfo.class, new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).jmxStatistics().enable().clustering().l1().disable().lifespan(Long.MAX_VALUE).build());
		this.myInfo = leg.newInstance(dftManager.getAddress().toString()).setManager(dftManager) ;
		
		this.started = true ;
		
		return this ;
	}
	
	public synchronized <E extends AbstractEntry> LegContainer<E> defineLeg(Class<E> clz, Configuration confOverride){
		
		String cacheName = clz.getCanonicalName();
		if (containers.containsKey(cacheName)) return containers.get(cacheName) ;

		if (! dftManager.cacheExists(cacheName)) {
			dftManager.defineConfiguration(cacheName, confOverride) ;
		}
		
		Cache<EntryKey, E> cache = dftManager.getCache(cacheName);
		LegContainer<E> result = LegContainer.create(this, cache, clz);
		containers.put(cacheName, result) ;
		
		return result ;
	}

	@Deprecated
	public <E extends AbstractEntry> LegContainer<E> defineLeg(Class<E> clz) {
		if (preDefinedConfig.containsKey(clz.getCanonicalName())){
			Configuration config = preDefinedConfig.get(clz.getCanonicalName()) ;
			return defineLeg(clz, config) ;
		} 
		return defineLeg(clz, defaultConf.build()) ;
	}
	
	public Craken stop() {
		Debug.warn("craken stopped") ;
		if (dftManager == null) return this ;
		dftManager.stop() ;
		return this ;
	}
	
	public DefaultCacheManager getManager(){
		return dftManager ;
	}

	public Craken addListener(Object listener) {
		dftManager.addListener(listener) ;
		return this ;
	}

	public CrakenInfo getInfo() {
		return myInfo ;
	}

	@Deprecated
	public <E extends AbstractEntry> Craken preDefineConfig(Class<E> clz, Configuration config) {
		preDefinedConfig.put(clz.getCanonicalName(), config) ;
		return this ;
	}
}
