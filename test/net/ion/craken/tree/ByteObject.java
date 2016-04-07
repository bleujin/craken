package net.ion.craken.tree;

import java.io.Serializable;

public class ByteObject implements Serializable{

	private static final long serialVersionUID = -6243870331502017159L;
	private byte[] bytes;
	public ByteObject(byte[] bytes){
		this.bytes = bytes ;
	}
	public byte[] bytes() {
		return bytes;
	}
}
