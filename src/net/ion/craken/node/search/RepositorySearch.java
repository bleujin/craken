package net.ion.craken.node.search;

import java.io.IOException;

import net.ion.craken.node.Repository;

public interface RepositorySearch extends Repository{

	public ReadSearchSession testLogin(String wsname) throws IOException  ;
}
