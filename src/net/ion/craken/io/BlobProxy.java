package net.ion.craken.io;

import java.io.Serializable;

public class BlobProxy implements Serializable {

	private static final long serialVersionUID = -3119451023593442193L;

	private String fqnPath;

	private BlobProxy(String fqnPath) {
		this.fqnPath = fqnPath;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof BlobProxy))
			return false;
		BlobProxy that = (BlobProxy) obj;
		return this.fqnPath.equals(that.fqnPath);
	}

	public int hashCode() {
		return fqnPath.hashCode();
	}

	public static BlobProxy create(String fqnPath) {
		return new BlobProxy(fqnPath);
	}

	public String path() {
		return fqnPath;
	}

}
