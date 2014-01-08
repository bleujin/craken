package net.ion.craken.node;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import net.ion.craken.io.GridBlob;
import net.ion.craken.io.GridOutputStream;
import net.ion.craken.io.Metadata;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;

import org.apache.commons.lang.SystemUtils;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

public class TranLogManager {

	private Workspace workspace;
	private Cache<String, Metadata> logMeta;
	private String repoName;

	private TranLogManager(Workspace workspace, Cache<String, Metadata> logMeta, String repoName) {
		this.workspace = workspace;
		this.logMeta = logMeta;
		this.repoName = repoName;
	}

	public static TranLogManager create(Workspace workspace, Cache<String, Metadata> logMeta, String repoName) {
		return new TranLogManager(workspace, logMeta, repoName);
	}

	public void start() {
		logMeta.addListener(new SyncLogListener(workspace));
		logMeta.start();
	}

	public void endTran(WriteSession wsession, Metadata metadata) throws IOException {
		logMeta.put(wsession.tranId(), metadata);

		final String tranPath = selfTranPath();
		final Metadata newMeta = Metadata.create(tranPath);
		synchronized (this) {
			Metadata transMeta = ObjectUtil.coalesce(logMeta.putIfAbsent(tranPath, newMeta), newMeta);
			GridOutputStream output = workspace.logContent().gridBlob(tranPath, transMeta).outputStream(true);
			try {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
				writer.write(wsession.tranId() + SystemUtils.LINE_SEPARATOR);
				writer.close();
			} finally {
				logMeta.put(tranPath, output.metadata());
				IOUtil.close(output);
			}
		}
	}

	public String[] readAll() throws IOException {
		List<String> result = ListUtil.newList();
		final BufferedReader reader = readSelfHistory();
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				result.add(line);
			}
		} finally {
			IOUtil.closeQuietly(reader);
		}

		return result.toArray(new String[0]);
	}

	public int resync() throws IOException {
		String[] aboveTrans = aboveHistory();

		for (String tran : aboveTrans) {
			Metadata meta = logMeta.get(tran);
			InputStream input = null ;
			try {
				input = workspace.logContent().gridBlob(tran, meta).toInputStream();
				workspace.storeData(input);
			} finally {
				IOUtil.close(input);
				workspace.repository().logger().info(tran + " applied") ;
			}
		}

		return aboveTrans.length;
	}

	private String[] aboveHistory() throws IOException {

		String lastTran = lastTran();
		if (StringUtil.isBlank(lastTran))
			return otherHistory();

		String[] others = otherHistory();
		List<String> result = ListUtil.newList();
		for (String tran : others) {
			if (tran.compareTo(lastTran) > 0)
				result.add(tran);
		}
		return result.toArray(new String[0]);
	}

	private String[] otherHistory() throws IOException {
		final Repository repository = workspace.repository();
		List<String> members = repository.memberNames();

		if (members.size() <= 1)
			return new String[0];

		for (String member : members) {
			if (member.equals(repoName))
				continue;
			return create(workspace, logMeta, member).readAll();
		}
		return new String[0];
	}

	public String selfTranPath() {
		return "/transactions/" + repoName + "/" + workspace.wsName();
	}

	public String lastTran() throws IOException {
		final BufferedReader reader = readSelfHistory();
		String lastTran = null;
		try {
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				lastTran = line;
			}
		} finally {
			IOUtil.closeQuietly(reader);
		}
		return lastTran;
	}

	private static BufferedReader EmptyReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(new byte[0])));

	private BufferedReader readSelfHistory() throws IOException {
		Metadata transMeta = logMeta.get(selfTranPath());
		if (transMeta == null)
			return EmptyReader;

		return new BufferedReader(new InputStreamReader(workspace.logContent().gridBlob(selfTranPath(), transMeta).toInputStream()));
	}

	@Listener
	public static class SyncLogListener {

		private Workspace wspace;
		private static Logger logger = LogBroker.getLogger(SyncLogListener.class);
		private ExecutorService aexec = Executors.newSingleThreadExecutor();

		public SyncLogListener(Workspace wspace) {
			this.wspace = wspace;

		}

		@CacheEntryModified
		public void entryModified(final CacheEntryModifiedEvent<String, Metadata> e) throws IOException {
			if (e.isPre())
				return;
			if (e.isOriginLocal())
				return;

			applyModify(e.getKey());
		}

		void applyModify(final String tranId) throws IOException {
			aexec.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					GridBlob gridBlob = wspace.logContent().newGridBlob(tranId);
					InputStream input = gridBlob.toInputStream();
					try {
						wspace.storeData(input);
						logger.info(tranId + " applied");
					} finally {
						IOUtil.closeQuietly(input);
					}
					return null;
				}
			}); // async
		}

	}

}
