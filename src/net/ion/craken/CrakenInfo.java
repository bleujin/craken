package net.ion.craken;

import java.util.List;

import net.ion.craken.simple.EmanonKey;
import net.ion.craken.simple.SimpleKeyFactory;
import net.ion.framework.util.StringUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.infinispan.manager.DefaultCacheManager;

import sun.swing.StringUIClientPropertyKey;

public class CrakenInfo extends AbstractEntry<CrakenInfo>{

	private static final long serialVersionUID = 6572614066176612882L;
	public static final CrakenInfo NOT_YET = CrakenInfo.NOT_YET ;
	
	private EntryKey key ;
	private String address ;
	private transient DefaultCacheManager manager;
	private CrakenInfo(String address){
		this.key = EmanonKey.create(address) ;
		this.address = address ;
	}
	
	CrakenInfo setManager(DefaultCacheManager manager){
		this.manager = manager ;
		return this ;
	}
	
	public String getId(){
		return address ;
	}
	
	public String clusterName(){
		return manager.getClusterName() ;
	}
	
	@Override
	public EntryKey key() {
		return key;
	}

	public String[] memberNames() {
		String clusterMembers = manager.getClusterMembers();
		if (clusterMembers.equals("local")) return new String[0] ; 
		return StringUtil.split(clusterMembers, "[], ") ;
	}

	
	private static class NotYet extends CrakenInfo{
		public NotYet(){
			super("not-defined") ;
		}
		@Override
		public String clusterName(){
			return "not-defined" ;
		}

		@Override
		public String[] memberNames(){
			return new String[0] ;
		}
	}
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}
}
