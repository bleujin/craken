package net.ion.craken.node;

import java.util.Map;
import java.util.Set;

import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;

import com.google.common.base.Function;

public interface NodeCommon<T extends NodeCommon<T>> {
	
	final static String NameProp = "__name";
	
	public ISession<T> session() ;

	public Fqn fqn();

	public int dataSize();

	public T parent();

	public boolean hasChild(String fqn);
	
	public boolean hasProperty(String pid) ;
	
	public boolean hasPropertyId(PropertyId pid) ;

	PropertyValue extendProperty(String propPath);

	public T root() ;

	public T child(String fqn);

	public Set<String> childrenNames();

	public Iterable<T> children();

	public Set<PropertyId> keys();
	
	public Set<PropertyId> normalKeys();

	public PropertyValue property(String key);
	
	public PropertyValue propertyId(PropertyId pid) ;
	
	public Object id() ;

	Map<PropertyId, PropertyValue> toMap();

	public T ref(String refName) ;
	
	boolean hasRef(String refName);
	
	boolean hasRef(String refName, Fqn fqn);

	public IteratorList<T> refs(String refName) ;

	<R> R transformer(Function<T, R> transformer) ;

}
