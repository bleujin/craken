package net.ion.craken.io;


import org.infinispan.Cache;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;


class FileChunkMapper {

	private static final Log log = LogFactory.getLog(FileChunkMapper.class);

	private final GridBlob file;
	private final Cache<String, byte[]> cache;

	public FileChunkMapper(GridBlob file, Cache<String, byte[]> cache) {
		this.file = file;
		this.cache = cache;
	}

	public int getChunkSize() {
		return file.getChunkSize();
	}

	public byte[] fetchChunk(int chunkNumber) {
		String key = getChunkKey(chunkNumber);
		byte[] val = cache.get(key);
		if (log.isTraceEnabled())
			log.trace("fetching key=" + key + ": " + (val != null ? val.length + " bytes" : "null"));
		return val;
	}

	public void storeChunk(int chunkNumber, byte[] buffer, int length) {
		String key = getChunkKey(chunkNumber);
		byte[] val = trim(buffer, length);
		cache.put(key, val);
		if (log.isTraceEnabled())
			log.trace("put(): key=" + key + ": " + val.length + " bytes");
	}

	public void removeChunk(int chunkNumber) {
		cache.remove(getChunkKey(chunkNumber));
	}

	private byte[] trim(byte[] buffer, int length) {
		byte[] val = new byte[length];
		System.arraycopy(buffer, 0, val, 0, length);
		return val;
	}

	private String getChunkKey(int chunkNumber) {
		return getChunkKey(file.getAbsolutePath(), chunkNumber);
	}

	static String getChunkKey(String absoluteFilePath, int chunkNumber) {
		return absoluteFilePath + ".#" + chunkNumber;
	}
}
