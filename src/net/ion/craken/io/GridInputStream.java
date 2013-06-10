package net.ion.craken.io;

import java.io.IOException;
import java.io.InputStream;

import org.infinispan.Cache;

/**
 * @author Bela Ban
 * @author Marko Luksa
 */
public class GridInputStream extends InputStream {

	private int index = 0; // index into the file for writing
	private int localIndex = 0;
	private byte[] currentBuffer = null;
	private boolean endReached = false;
	private boolean closed = false;
	private FileChunkMapper fileChunkMapper;

	GridInputStream(GridFile file, Cache<String, byte[]> cache) {
		fileChunkMapper = new FileChunkMapper(file, cache);
	}

	@Override
	public int read() throws IOException {
		checkClosed();
		int remaining = getBytesRemainingInChunk();
		if (remaining == 0) {
			if (endReached)
				return -1;
			fetchNextChunk();
			if (currentBuffer == null)
				return -1;
			else if (isLastChunk())
				endReached = true;
		}
		int retval = 0x0ff & currentBuffer[localIndex++];
		index++;
		return retval;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		checkClosed();
		int totalBytesRead = 0;
		while (len > 0) {
			int bytesRead = readFromChunk(b, off, len);
			if (bytesRead == -1)
				return totalBytesRead > 0 ? totalBytesRead : -1;
			off += bytesRead;
			len -= bytesRead;
			totalBytesRead += bytesRead;
		}

		return totalBytesRead;
	}

	private int readFromChunk(byte[] b, int off, int len) {
		int remaining = getBytesRemainingInChunk();
		if (remaining == 0) {
			if (endReached)
				return -1;
			fetchNextChunk();
			if (currentBuffer == null)
				return -1;
			else if (isLastChunk())
				endReached = true;
			remaining = getBytesRemainingInChunk();
		}
		int bytesToRead = Math.min(len, remaining);
		System.arraycopy(currentBuffer, localIndex, b, off, bytesToRead);
		localIndex += bytesToRead;
		index += bytesToRead;
		return bytesToRead;
	}

	private boolean isLastChunk() {
		return currentBuffer.length < getChunkSize();
	}

	@Override
	public long skip(long len) throws IOException {
		checkClosed();
		// naive and inefficient, but working
		long count = 0;
		while (len != count && read() != -1) {
			count++;
		}
		return count;
	}

	@Override
	public int available() throws IOException {
		checkClosed();
		return getBytesRemainingInChunk();
	}

	@Override
	public void close() throws IOException {
		localIndex = index = 0;
		endReached = false;
		closed = true;
	}

	private void checkClosed() throws IOException {
		if (closed) {
			throw new IOException("Stream is closed");
		}
	}

	private int getBytesRemainingInChunk() {
		return currentBuffer == null ? 0 : currentBuffer.length - localIndex;
	}

	private void fetchNextChunk() {
		currentBuffer = fileChunkMapper.fetchChunk(getChunkNumber());
		localIndex = 0;
	}

	private int getChunkNumber() {
		return index / getChunkSize();
	}

	private int getChunkSize() {
		return fileChunkMapper.getChunkSize();
	}
}
