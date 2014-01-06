package net.ion.craken.io;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

import net.ion.craken.tree.PropertyValue;
import net.ion.framework.parse.gson.JsonObject;

import org.jgroups.util.Util;



public class Metadata implements Externalizable {
	public static final byte FILE = 1;
	public static final byte DIR = 1 << 1;

	private String path ;
	private int length = 0;
	private long modificationTime = 0;
	private int chunkSize = 0;
	private byte flags = 0;

	public Metadata() {
	}

	public Metadata(String path, int length, long modificationTime, int chunkSize, byte flags) {
		this.path = path ;
		this.length = length;
		this.modificationTime = modificationTime;
		this.chunkSize = chunkSize;
		this.flags = flags;
	}

	public final static Metadata create(String path){
		return new Metadata(path, 0, System.currentTimeMillis(), GridFilesystem.DefaultChunkSize, Metadata.FILE) ;			
	}
	
	public static Metadata loadFromJsonString(String jsonString) {
		return JsonObject.fromString(jsonString).getAsObject(Metadata.class);
	}

	public static boolean isValid(Object value) {
		if (value == null || (!(value instanceof String))) return false ;
		
		// TODO Auto-generated method stub
		return true;
	}

	public int getLength() {
		return length;
	}

	void setLength(int length) {
		this.length = length;
	}

	public long getModificationTime() {
		return modificationTime;
	}

	void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	public int getChunkSize() {
		return chunkSize;
	}

	public boolean isFile() {
		return Util.isFlagSet(flags, FILE);
	}

	public boolean isDirectory() {
		return Util.isFlagSet(flags, DIR);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getType());
		if (isFile())
			sb.append(", len=" + Util.printBytes(length) + ", chunkSize=" + chunkSize);
		sb.append(", modTime=").append(new Date(modificationTime));
		return sb.toString();
	}

	private String getType() {
		if (isFile())
			return "file";
		if (isDirectory())
			return "dir";
		return "n/a";
	}

	public PropertyValue asPropertyValue() {
		JsonObject result = new JsonObject();
		result.addProperty("path", path);
		result.addProperty("length", length);
		result.addProperty("modificationTime", modificationTime);
		result.addProperty("chunkSize", chunkSize);
		result.addProperty("flags", flags);
		result.addProperty("type", "blob");

		return PropertyValue.createPrimitive(result.toString()) ;
	}


	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(length);
		out.writeLong(modificationTime);
		out.writeInt(chunkSize);
		out.writeByte(flags);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		length = in.readInt();
		modificationTime = in.readLong();
		chunkSize = in.readInt();
		flags = in.readByte();
	}

	public String path() {
		return path;
	}

}
