package net.ion.craken.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class BlobValue {

	private final GridFilesystem gfs;
	private final BlobProxy proxy;

	public BlobValue(GridFilesystem gfs, BlobProxy proxy) {
		this.gfs = gfs;
		this.proxy = proxy;
	}

	public static BlobValue create(GridFilesystem gfs, BlobProxy proxy) {
		return new BlobValue(gfs, proxy);
	}

	File toFile() {
		return gfs.getFile(proxy.path());
	}

	public InputStream toInputStream() throws FileNotFoundException {
		return gfs.getInput(proxy.path());
	}

}
