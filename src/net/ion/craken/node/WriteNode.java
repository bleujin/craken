package net.ion.craken.node;

import java.io.IOException;
import java.io.InputStream;

import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.WriteChildren;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.framework.parse.gson.JsonObject;

import org.apache.lucene.queryparser.classic.ParseException;

public interface WriteNode extends NodeCommon<WriteNode> {

	public WriteSession session() ;
	
	public WriteChildren children();

	public PropertyValue increase(String key);

	public WriteNode property(String key, Object value) ;
	
	public WriteNode encrypt(String key, String value) throws IOException ;

	public WriteNode property(PropertyId key, PropertyValue value) ;
	
	public WriteNode propertyIfAbsent(String key, Object value) ;

	public PropertyValue propertyIfAbsentEnd(String key, Object value) ;

	public WriteNode append(String key, Object value) ;
	
	public WriteNode append(String key, Object... value);

	public PropertyValue replace(String key, Object value) ;
	
	public boolean replace(String key, Object oldValue, Object newValue) ;
	
	public WriteNode unset(String key, Object... values) ;

	public WriteNode unset(String key, Object value) ;

	public WriteNode clear() ;
	
	public WriteNode child(String relativeFqn) ;
	
	public boolean removeChild(String childName) ;

	public boolean removeSelf();
	
	public boolean removeChildren() ;

	public WriteNode refTo(String refName, String fqn);
	
	public WriteNode refTos(String refName, String... fqns);

	public WriteNode refTos(String refName, String fqn);

	public WriteNode unRefTos(String refName, String... fqns);
	
	public WriteNode unRefTos(String refName, String fqn);

	public WriteNode fromJson(JsonObject json);

	public WriteNode blob(String key, InputStream input);

	public ChildQueryRequest childQuery(String query) throws IOException, ParseException  ;
	
	public ChildQueryRequest childTermQuery(String name, String value, boolean includeDecentTree) throws IOException, ParseException  ;
	
	public ChildQueryRequest childQuery(String query, boolean includeDecentTree) throws ParseException, IOException;

	public WriteChildren refChildren(String refName);

	public ReadNode toReadNode() ;

	public WriteNode reindex(boolean includeSub);

	public WriteNode moveTo(String targetParent);
	
	public WriteNode moveTo(String targetParent, int ancestorDepth);

	public WriteNode copyTo(String targetParent, boolean includeChild);


}
