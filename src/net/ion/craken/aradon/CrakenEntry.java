package net.ion.craken.aradon;

import net.ion.craken.Craken;
import net.ion.radon.core.IService;
import net.ion.radon.core.context.OnEventObject;

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
	public void onEvent(AradonEvent ae, IService service) {
		if (ae == AradonEvent.START){
			craken.start() ;
		} else if (ae == AradonEvent.STOP){
			craken.stop() ;
		}
	}

	public Craken getCraken() {
		return craken ;
	}
	
	
	
	
}
