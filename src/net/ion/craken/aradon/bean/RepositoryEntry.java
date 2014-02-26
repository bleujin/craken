package net.ion.craken.aradon.bean;

import java.io.IOException;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.radon.core.IService;
import net.ion.radon.core.context.OnEventObject;

public class RepositoryEntry implements OnEventObject {

	private RepositoryImpl r;
	public final static String EntryName = "repository" ;
	
	private RepositoryEntry(RepositoryImpl r) {
		this.r = r ;
	}

	public static RepositoryEntry test() throws IOException {
		return new RepositoryEntry(RepositoryImpl.inmemoryCreateWithTest());

	}
	public ReadSession login(String wsName) throws IOException {
		return r.login(wsName);
	}

	@Override
	public void onEvent(AradonEvent event, IService service) {
		if (event == AradonEvent.START){
			r.start() ;
		} else if (event == AradonEvent.STOP){
			r.shutdown() ;
		}
	}

	public void shutdown() {
		r.shutdown() ;
	}

	
	
	
}
