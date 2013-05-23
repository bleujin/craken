package net.ion.rosetta.query;

public interface HasMetadata {

	public Object get(Constants tagID);

	public Iterable<Constants> getAllTags();

}
