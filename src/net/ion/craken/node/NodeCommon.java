package net.ion.craken.node;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;

public interface NodeCommon<T extends NodeCommon<T>> {
	
	public final static String IDProp = "__id" ;
	public final static String NameProp = "__name";
	
	public ISession<T> session() ;

	public Fqn fqn();

	public int dataSize();

	public T parent();

	public boolean hasChild(String fqn);
	
	public boolean hasProperty(PropertyId pid) ;

	PropertyValue extendProperty(String propPath);

	public T root() ;

	public T child(String fqn);

	public Set<String> childrenNames();

	public IteratorList<T> children();

	public Set<PropertyId> keys();

	public PropertyValue property(String key);
	
	public Object id() ;

	Map<PropertyId, PropertyValue> toMap();

	public T ref(String refName) ;
	
	boolean hasRef(String refName);
	
	boolean hasRef(String refName, Fqn fqn);

	public IteratorList<T> refs(String refName) ;


}
