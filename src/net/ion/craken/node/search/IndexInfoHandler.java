package net.ion.craken.node.search;

import net.ion.nsearcher.reader.InfoReader;

public interface IndexInfoHandler<T> {

	T handle(ReadSearchSession session, InfoReader infoReader);

}
