package net.ion.craken.node;

import java.io.IOException;
import java.util.Set;

public interface NodeWriter {
	public final static NodeWriter BLANK = new NodeWriter(){
		public void writeLog(Set<TouchedRow> logRows) throws IOException {
		}
	} ;
	
	public void writeLog(final Set<TouchedRow> logRows) throws IOException ;
}
