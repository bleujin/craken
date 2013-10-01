package net.ion.craken.node;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.framework.db.Rows;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;


public interface ReadNode extends NodeCommon<ReadNode> {

	
	
	<T> T toBean(Class<T> clz);

	Map<String, Object> toPropertyMap(int descendantDepth);

	public ReadChildren children();
	
	Rows toRows(String expr);

	ChildQueryRequest childQuery(String query) throws IOException, ParseException;

	ChildQueryRequest childQuery(Query query) throws ParseException, IOException;

	ChildQueryRequest childQuery(String query, boolean includeAllTree) throws ParseException, IOException;


}
