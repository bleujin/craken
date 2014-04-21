package net.ion.craken.node.crud;

public interface WalkChildrenEach<T> {

	public <T> T handle(WalkChildrenIterator trc)  ;

}
