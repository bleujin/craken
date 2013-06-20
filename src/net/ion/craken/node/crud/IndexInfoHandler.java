package net.ion.craken.node.crud;

import net.ion.craken.node.ReadSession;
import net.ion.nsearcher.reader.InfoReader;

public interface IndexInfoHandler<T> {

	T handle(ReadSession session, InfoReader infoReader);

}
