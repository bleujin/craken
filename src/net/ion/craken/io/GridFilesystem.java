package net.ion.craken.io;

import static org.infinispan.context.Flag.FORCE_ASYNCHRONOUS;
import static org.infinispan.context.Flag.FORCE_SYNCHRONOUS;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.context.Flag;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public class GridFilesystem {

	private static final Log log = LogFactory.getLog(GridFilesystem.class);

	public static final int DefaultChunkSize = 32 * 1024;

	protected final Cache<String, byte[]> data;
	protected final int defaultChunkSize;

	public GridFilesystem(Cache<String, byte[]> data, int defaultChunkSize) {
		this.data = data;
		this.defaultChunkSize = defaultChunkSize;
	}

	public GridFilesystem(Cache<String, byte[]> data) {
		this(data, DefaultChunkSize);
	}

	private boolean isNotValid(Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> metaCache) {
		return metaCache.getCacheConfiguration().clustering().cacheMode().isClustered() && !metaCache.getCacheConfiguration().clustering().cacheMode().isSynchronous();
	}

	public GridBlob gridBlob(String pathname, Metadata metadata) {
		return new GridBlob(pathname, metadata, this);
	}

	public GridBlob newGridBlob(String pathname) {
		return gridBlob(pathname, Metadata.create(pathname)) ;
	}

	
	public WritableGridBlob getWritableGridBlob(String pathname, Metadata meta) {
		return new WritableGridBlob(pathname, meta, this);
	}

	public GridOutputStream getOutput(GridBlob gblob, boolean append) throws IOException {
		return new GridOutputStream(gblob, append, data);
	}

	InputStream getInput(GridBlob gblob) {
		return new GridInputStream(gblob, data);
	}

	ReadableGridFileChannel getReadableChannel(GridBlob gblob) throws FileNotFoundException {
		return new ReadableGridFileChannel(gblob, data);
	}

	WritableGridFileChannel getWritableChannel(GridBlob gblob) throws IOException {
		return getWritableChannel(gblob, false);
	}

	WritableGridFileChannel getWritableChannel(GridBlob gblob, boolean append) throws IOException {
		return new WritableGridFileChannel(gblob, data, append);
	}

	/**
	 * Removes the file denoted by absolutePath. This operation can either be executed synchronously or asynchronously.
	 * 
	 * @param absolutePath
	 *            the absolute path of the file to remove
	 * @param synchronous
	 *            if true, the method will return only after the file has actually been removed; if false, the method will return immediately and the file will be removed asynchronously.
	 */
	void remove(String absolutePath, Metadata md, boolean synchronous) {
		if (absolutePath == null)
			return;
		if (md == null || md.getLength() == 0)
			return;

		Flag flag = synchronous ? FORCE_SYNCHRONOUS : FORCE_ASYNCHRONOUS;
		AdvancedCache<String, byte[]> advancedCache = data.getAdvancedCache().withFlags(flag);
		int numChunks = md.getLength() / md.getChunkSize() + 1;
		for (int i = 0; i < numChunks; i++)
			advancedCache.remove(FileChunkMapper.getChunkKey(absolutePath, i));

		// removeMeta
	}


}
