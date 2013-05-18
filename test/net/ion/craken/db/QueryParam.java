package net.ion.craken.db;

public interface QueryParam {
	public Object getParam(int index);

	public int getParamType(int index);

	public String getString(int i);

	public int getInt(int i);

	public String procName();

}
