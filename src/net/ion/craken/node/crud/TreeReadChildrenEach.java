package net.ion.craken.node.crud;

public interface TreeReadChildrenEach<T> {

	public <T> T handle(TreeReadChildrenIterator trc)  ;

}
