package net.ion.craken;

import net.ion.framework.util.Debug;

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
	
	private Craken(){
		this.globalBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder() ;
		this.defaultConf = new ConfigurationBuilder() ;
		defaultConf.clustering().cacheMode(CacheMode.DIST_ASYNC).jmxStatistics().enable().clustering().l1().enable().lifespan(6000000).invocationBatching().clustering().hash().numOwners(2) ;
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

	public Craken start() {
		Debug.warn("craken starting") ;
		
		if (started) {
			Debug.warn("already started") ;
			return this ;
		}
		this.dftManager = new DefaultCacheManager(globalBuilder.build(), defaultConf.build(), true) ;
		dftManager.start() ;
		
		LegContainer<CrakenInfo> leg = defineLeg(CrakenInfo.class, new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).jmxStatistics().enable().clustering().l1().disable().lifespan(Long.MAX_VALUE).build());
		this.myInfo = leg.newInstance(dftManager.getAddress().toString()).setManager(dftManager) ;
		
		this.started = true ;
		
		return this ;
	}
	
	public <E extends AbstractEntry> LegContainer<E> defineLeg(Class<E> clz, Configuration confOverride){
		dftManager.defineConfiguration(clz.getCanonicalName(), confOverride) ;
		return defineLeg(clz) ;
	}

	public <E extends AbstractEntry> LegContainer<E> defineLeg(Class<E> clz) {
		Cache<EntryKey, E> cache = dftManager.getCache(clz.getCanonicalName());
		return LegContainer.create(cache, clz);
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
}
