package net.ion.craken.loaders.lucene;

import java.io.File;
import java.io.IOException;

import net.ion.craken.loaders.WorkspaceConfig;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;

public class ISearcherWorkspaceConfig extends WorkspaceConfig {

	private static final long serialVersionUID = 5891372400491793884L;

	private String location = "./resource/local";
	private int lockTimeoutMs = 60 * 1000;

	private int maxNodeEntry = 15000;
	private LazyCentralConfig lazyConfig = new LazyCentralConfig();

	public ISearcherWorkspaceConfig() {
		setCacheLoaderClassName(ISearcherWorkspaceStore.class.getName());
	}

	public static ISearcherWorkspaceConfig create() {
		return new ISearcherWorkspaceConfig();
	}

	public static ISearcherWorkspaceConfig createDefault() {
		return ISearcherWorkspaceConfig.create().location("./resource/local");
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		testImmutability("location");
		this.location = location;
	}

	public ISearcherWorkspaceConfig resetDir() throws IOException {
		FileUtil.deleteDirectory(new File(location));
		return this;
	}

	public ISearcherWorkspaceConfig location(String path) {
		setLocation(path);
		return this;
	}

	public String location() {
		return location;
	}

	public int maxNodeEntry() {
		return maxNodeEntry;
	}

	public int lockTimeoutMs() {
		return lockTimeoutMs;
	}

	public ISearcherWorkspaceConfig maxNodeEntry(int maxNodeEntry) {
		this.maxNodeEntry = maxNodeEntry;
		return this;
	}

	public ISearcherWorkspaceConfig lockTimeoutMs(int lockTimeoutMs) {
		this.lockTimeoutMs = lockTimeoutMs;
		return this;
	}

	public Central buildCentral() throws CorruptIndexException, IOException {
		Directory dir = null;
		if (StringUtil.isBlank(location)) {
			dir = new RAMDirectory();
		} else {
			final File file = new File(location);
			if (!file.exists())
				file.mkdirs();
			dir = FSDirectory.open(file);
		}
		final Central result = lazyConfig.dir(dir).indexConfigBuilder().indexAnalyzer(new MyKoreanAnalyzer()).parent().searchConfigBuilder().queryAnalyzer(new MyKoreanAnalyzer()).build();

		// Debug.line('i', this.hashCode(), this) ;

		return result;
		// CentralConfig.newLocalFile().dirFile(location()).indexConfigBuilder().setRamBufferSizeMB(128).build();
	}

	public CentralConfig centralConfig() {
		return lazyConfig;
	}

	@Override
	public Workspace createWorkspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName) throws IOException {
		return new ISearchWorkspace(repository, cache, wsName, this);
	}

}