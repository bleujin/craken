package net.ion.ics6.dfile;

import net.ion.craken.node.crud.Craken;

public class FileContext {

	private Craken craken;
	public FileContext(Craken craken) {
		this.craken = craken ;
	}

	public static FileContext create(Craken craken) {
		return new FileContext(craken);
	}
	
	public Craken craken(){
		return craken ;
	}

	public void stop() {
		craken.stop() ; 
	}

}
