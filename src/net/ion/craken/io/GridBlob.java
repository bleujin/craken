package net.ion.craken.io;

import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

import net.ion.craken.tree.PropertyValue;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;

import org.jgroups.util.Util;

public class GridBlob implements Comparable<GridBlob> {
	private static final long serialVersionUID = 552534285862004134L;
	private static final char SEPARATOR_CHAR = '/';
	private static final String SEPARATOR = "" + SEPARATOR_CHAR;

	private final GridFilesystem gfs;
	private final String pathname;
	private Metadata metadata;

	GridBlob(String pathname, Metadata metadata, GridFilesystem gfs) {
		this.gfs = gfs;
		this.pathname = pathname;
		this.metadata = metadata ;
	}

	public String getName() {
		return filename(getPath());
	}

	public String getPath() {
		return pathname;
	}

	public String getAbsolutePath() {
		return convertToAbsolute(getPath());
	}

	public boolean isAbsolute() {
		return getPath().startsWith(SEPARATOR);
	}

	private String convertToAbsolute(String path) {
		if (!path.startsWith(SEPARATOR))
			return SEPARATOR + path;
		else
			return path;
	}

	public long length() {
		if (metadata != null)
			return metadata.length;
		return 0;
	}

	public Metadata getMetadata() {
		return metadata ;
	}

	public InputStream toInputStream() throws FileNotFoundException {
		checkIsReadable();
		return gfs.getInput(this);
	}

	
	public ReadableGridFileChannel getReadableChannel() throws FileNotFoundException {
		checkIsReadable();
		return gfs.getReadableChannel(this);
	}

	
	
	void checkIsReadable() throws FileNotFoundException {
		checkExists();
	}

	void checkExists() throws FileNotFoundException {
		if (!exists())
			throw new FileNotFoundException(getPath());
	}

	public int getChunkSize() {
		return metadata.getChunkSize();
	}

	public boolean exists() {
		return metadata != null ;
	}

	public long lastModified() {
		return metadata == null ? 0 : metadata.getModificationTime();
	}


	protected static String filename(String fullPath) {
		String[] comps = Util.components(fullPath, SEPARATOR);
		return comps != null ? comps[comps.length - 1] : "";
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof GridBlob)) {
			return compareTo((GridBlob) obj) == 0;
		}
		return false;
	}

	@Override
	public int compareTo(GridBlob file) {
		return getAbsolutePath().compareTo(file.getAbsolutePath());
	}

	@Override
	public int hashCode() {
		return getAbsolutePath().hashCode();
	}

	@Override
	public String toString() {
		return "GridBlob{" + "path='" + pathname + '\'' + '}';
	}

	
	
	// .....
	protected void setLength(int newLength) {
		if (metadata == null)
			throw new IllegalStateException("metadata for " + getAbsolutePath() + " not found.");

		metadata.setLength(newLength);
		metadata.setModificationTime(System.currentTimeMillis());
	}

	protected GridOutputStream outputStream() throws IOException {
		return gfs.getOutput(this, false);
	}

	protected WritableGridFileChannel getWritableChannel(boolean append) throws IOException {
		return gfs.getWritableChannel(this, append);
	}


	protected boolean delete(Metadata meta) {
		return delete(meta, false); // asynchronous delete by default
	}

	@Deprecated
	protected boolean delete(Metadata meta, boolean synchronous) {
		if (!exists())
			return false;

		gfs.remove(getAbsolutePath(), meta, synchronous); // removes all the chunks belonging to the file

		return true;
	}

	protected boolean setLastModified(long time) {
		if (time < 0) {
			throw new IllegalArgumentException("Negative time");
		}
		if (metadata == null) {
			return false;
		}
		metadata.setModificationTime(time);
		return true;
	}
	// .....
	
	
	
	
	
	
	
	
	
	
	public static class Metadata implements Externalizable {
		public static final byte FILE = 1;
		public static final byte DIR = 1 << 1;

		private String path ;
		private int length = 0;
		private long modificationTime = 0;
		private int chunkSize = 0;
		private byte flags = 0;

		Metadata() {
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

	}
}
