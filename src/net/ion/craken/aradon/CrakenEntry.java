package net.ion.craken.aradon;

import net.ion.craken.Craken;
import net.ion.nradon.Radon;
import net.ion.nradon.config.OnEventObject;
import net.ion.nradon.handler.event.ServerEvent.EventType;
import net.ion.radon.core.IService;

public class CrakenEntry implements OnEventObject{

	private Craken craken ;
	public CrakenEntry(String clusterName, String configPath) {
		this.craken = Craken.create() ;
		craken.globalConfig().transport().clusterName(clusterName).addProperty("configurationFile", configPath) ;
		craken.start() ;
	}
	
	public final static CrakenEntry test(){
		return new CrakenEntry("my-cluster", "resource/config/jgroups-udp.xml") ;
	}

	@Override
	public void onEvent(EventType ae, Radon service) {
		if (ae == EventType.START){
			craken.start() ;
		} else if (ae == EventType.STOP){
			craken.stop() ;
		}
	}

	public Craken getCraken() {
		return craken ;
	}
	
	
	
	
}
