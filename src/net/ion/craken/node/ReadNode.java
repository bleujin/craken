package net.ion.craken.node;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import net.ion.craken.node.convert.rows.FieldDefinition;
import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.node.crud.RefQueryRequest;
import net.ion.craken.node.crud.WalkReadChildren;
import net.ion.craken.node.crud.WalkRefChildren;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.framework.db.Rows;
import net.ion.framework.parse.gson.JsonObject;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;


public interface ReadNode extends NodeCommon<ReadNode> {

	ReadSession session() ;
	
	<T> T toBean(Class<T> clz);

	Map<String, Object> toPropertyMap(int descendantDepth);

	ReadChildren children();
	
	Rows toRows(String expr, FieldDefinition... fieldDefinitons);

	ChildQueryRequest childQuery(String query) throws IOException;

	ChildQueryRequest childQuery(Query query) throws IOException;

	ChildQueryRequest childQuery(Query query, boolean includeAllTree) throws IOException;

	ChildQueryRequest childQuery(String query, boolean includeAllTree) throws IOException;
	
	ChildQueryRequest childTermQuery(String name, String value, boolean includeDecentTree) throws IOException, ParseException ;

	void template(String propId, Writer writer) throws IOException;

	JsonObject toValueJson();

	boolean isGhost() ;

	void debugPrint();

	ReadChildren refChildren(String refName);

	WalkReadChildren walkChildren();

	WalkRefChildren walkRefChildren(String refName);

	boolean isMatch(String key, String value) throws IOException;

	RefQueryRequest refsToMe(String refName) throws IOException;

	RefQueryRequest refsToChildren(String string) throws IOException;

	ReadChildren walkRefMeChildren(String refName, Fqn parent);

}
