package net.ion.craken.script;

import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import net.ion.craken.node.ReadNode;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ObjectUtil;

/**
 * Author: Ryunhee Han Date: 2014. 1. 14.
 */
public class BasicBuilder extends AbstractBuilder {

	private AbstractBuilder parent;

	protected BasicBuilder(AbstractBuilder parent) {
		this.parent = parent;
	}

	public AbstractBuilder parent() {
		return parent;
	}

	public BasicBuilder property(String name, Object value) {
		
		props().put(name, value);
		return this;
	}

    public BasicBuilder property(String name, JsonElement value){
        props().put(name, value);
        return this;
    }

	public BasicBuilder property(ReadNode node, String values) {
		return (BasicBuilder) super.property(node, values);
	}

	public ListBuilder inlist(final String name) {
		try {
			ListBuilder result = (ListBuilder) props().get(name, new Callable<ListBuilder>() {
				@Override
				public ListBuilder call() {
					ListBuilder lb = new ListBuilder(BasicBuilder.this);
					props().put(name, lb);
					return lb;
				}
			});
			return result.next();
		} catch (ExecutionException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public AbstractBuilder inlist(String name, Iterable<ReadNode> nodes, String values) {
		for (ReadNode node : nodes) {
			inlist(name).property(node, values);
		}

		return this;
	}

	public JsonElement makeJson() {
		JsonObject json = JsonObject.create();

		for (Entry<String, Object> entry : props().asMap().entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
            if (value instanceof ListBuilder) {
				json.put(name, ((ListBuilder) value).makeJson());
			} else if (value instanceof BasicBuilder) {
				json.put(name, ((BasicBuilder) value).makeJson());
			} else if (value == ObjectUtil.NULL) {
				json.put(name, null) ;
            } else if (value instanceof JsonElement) {
                json.put(name, value);
			} else {
				json.put(name, value);
			}
		}

		return json;
	}


}
