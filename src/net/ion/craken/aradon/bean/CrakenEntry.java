package net.ion.craken.aradon.bean;

import java.io.IOException;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.nradon.Radon;
import net.ion.nradon.config.OnEventObject;
import net.ion.nradon.handler.event.ServerEvent.EventType;

public class CrakenEntry implements OnEventObject{

	private Craken craken ;
	public CrakenEntry(String clusterName, String configPath) throws IOException {
		this.craken = Craken.create() ;
//		craken.globalConfig().transport().clusterName(clusterName).addProperty("configurationFile", configPath) ;
		craken.start() ;
	}
	
	public final static CrakenEntry test() throws IOException{
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

	public ReadSession login(String wsname) throws IOException {
		return craken.login(wsname);
	}

	public void shutdown() {
		craken.shutdown(); 
	}
	
	
	
	
}
