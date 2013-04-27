package net.ion.craken.node;


public interface ReadNode extends NodeCommon<ReadNode> {

	ReadNode ref(String relName);
	// .. common 

	IteratorList<ReadNode> refs(String relName);

	<T> T toBean(Class<T> clz);


	
}
