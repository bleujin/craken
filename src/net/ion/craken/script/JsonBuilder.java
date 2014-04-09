package net.ion.craken.script;

import net.ion.craken.node.ReadNode;

/**
 * Author: Ryunhee Han
 * Date: 2014. 1. 14.
 */
public class JsonBuilder {

	private static JsonBuilder SELF = new JsonBuilder() ;
    private JsonBuilder() {
    }

    public static JsonBuilder instance(){
        return SELF;
    }

    public BasicBuilder newInner() {
        return new BasicBuilder(null);
    }

    public ListBuilder newInlist(){
        return new ListBuilder(null).next();
    }

	public AbstractBuilder newInlist(Iterable<ReadNode> nodes, String values) {
		ListBuilder created = new ListBuilder(null);
		for (ReadNode node : nodes) {
			created.next().property(node, values) ;
		}
		return created;
	}

}
