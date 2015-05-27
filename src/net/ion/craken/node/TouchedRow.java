package net.ion.craken.node;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.ListUtil;

public class TouchedRow {

	private WriteNode source;
	private Touch touch;
	private Fqn target;
	private Map<String, Fqn> affected;
	private Map<PropertyId, PropertyValue> value ;

	private TouchedRow(WriteNode source, Touch touch, Fqn target, Map<String, Fqn> affected) {
		this.source = source;
		this.touch = touch;
		this.target = target;
		this.affected = affected ;
		this.value = source.toMap() ;
	}

	public final static TouchedRow create(WriteNode source, Touch touch, Fqn target, Map<String, Fqn> affected) {
		return new TouchedRow(source, touch, target, affected);
	}

	public Touch touch() {
		return touch;
	}

	public Fqn target() {
		return target;
	}
	
	public Map<String, Fqn> affected(){
		return Collections.unmodifiableMap(affected) ;
	}

	public WriteNode source() {
		return source;
	}

	public Map<PropertyId, PropertyValue> value(){
		return value ;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!TouchedRow.class.isInstance(obj))
			return false;

		TouchedRow that = (TouchedRow) obj;
		return this.touch == that.touch && this.target.equals(that.target);
	}

	@Override
	public int hashCode() {
		return target.hashCode() + touch.ordinal();
	}

	public String toString() {
		return target + ", " + touch;
	}

	public CDDModifiedEvent modifyEvent() {
		return CDDModifiedEvent.create(this) ;
	}

	public CDDRemovedEvent deleteEvent() {
		return CDDRemovedEvent.create(this) ;
	}

	public CDDRemovedEvent[] deleteChildrenEvent() {
		List<CDDRemovedEvent> result = ListUtil.newList() ;
		for (Fqn fqn : affected.values()) {
			result.add(CDDRemovedEvent.create(fqn));
		}
		return result.toArray(new CDDRemovedEvent[0]) ;
	}
}
