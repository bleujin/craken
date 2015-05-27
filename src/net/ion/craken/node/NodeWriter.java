package net.ion.craken.node;

import java.io.IOException;
import java.util.Set;

public interface NodeWriter {
	public void writeLog(final Set<TouchedRow> logRows) throws IOException ;
}
