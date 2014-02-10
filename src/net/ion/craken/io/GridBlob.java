package net.ion.craken.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.jgroups.util.Util;

public class GridBlob implements Comparable<GridBlob> {
	private static final long serialVersionUID = 552534285862004134L;
	private static final char SEPARATOR_CHAR = '/';
	private static final String SEPARATOR = "" + SEPARATOR_CHAR;

	private final GridFilesystem gfs;
	private Metadata metadata;

	GridBlob(String pathname, Metadata metadata, GridFilesystem gfs) {
		this.gfs = gfs;
		this.metadata = metadata.path(pathname) ;
	}

	public String getName() {
		return filename(getPath());
	}

	public String getPath() {
		return metadata.path();
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
			return metadata.getLength();
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
		return "GridBlob{" + "path='" + metadata.path() + '\'' + '}';
	}

	
	
	// .....
	protected void setLength(int newLength) {
		if (metadata == null)
			throw new IllegalStateException("metadata for " + getAbsolutePath() + " not found.");

		metadata.setLength(newLength);
		metadata.setModificationTime(System.currentTimeMillis());
	}

	public GridOutputStream outputStream() throws IOException {
		return gfs.getOutput(this, false);
	}

	public GridOutputStream outputStream(boolean append) throws IOException {
		return gfs.getOutput(this, append);
	}

	
	protected WritableGridFileChannel getWritableChannel(boolean append) throws IOException {
		return gfs.getWritableChannel(this, append);
	}


	public boolean delete(boolean synchronous) {
		return delete(metadata, synchronous); // asynchronous delete by default
	}

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
	
	
	
	
	
	
	
	
	
}
