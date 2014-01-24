package net.ion.craken.node;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import net.ion.craken.io.GridBlob;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.io.GridOutputStream;
import net.ion.craken.io.Metadata;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.radon.core.config.WSPathConfiguration;

import org.apache.commons.lang.SystemUtils;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.remoting.transport.Address;

public class TranLogManager {

	private Workspace workspace;
	private Cache<String, Metadata> logMeta;
	private String repoId;

	private TranLogManager(Workspace workspace, Cache<String, Metadata> logMeta, String repoName) {
		this.workspace = workspace;
		this.logMeta = logMeta;
		this.repoId = repoName;
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

		writeTransaction(wsession.tranId());
	}

	private void writeTransaction(String appliedTranId) throws IOException {
		final String historyTranPath = historyTranPath();
		final Metadata newMeta = Metadata.create(historyTranPath);
		synchronized (this) {
			Metadata transMeta = ObjectUtil.coalesce(logMeta.putIfAbsent(historyTranPath, newMeta), newMeta);
			GridOutputStream output = workspace.logContent().gridBlob(historyTranPath, transMeta).outputStream(true);
			Writer writer = null ; 
			try {
				writer = new BufferedWriter(new OutputStreamWriter(output));
				writer.write(appliedTranId + SystemUtils.LINE_SEPARATOR);
				writer.flush() ;
			} finally {
				logMeta.put(historyTranPath, output.metadata());
				IOUtil.closeQuietly(writer) ;
				IOUtil.closeQuietly(output);
			}
		}
	}

	public String[] readAll() throws IOException {
		List<String> result = ListUtil.newList() ;
		final BufferedReader reader = readSelfHistory() ;
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				result.add(line);
			}
		} finally {
			IOUtil.closeQuietly(reader);
		}

		return result.toArray(new String[0]) ;
	}

	public int resync() throws IOException {
		
		
		
		
		
		String[] aboveTrans = aboveHistory();
		for (String tranId : aboveTrans) {
			Metadata findMeta = logMeta.get(tranId);
			InputStream input = null ;
			try {
				input = workspace.logContent().gridBlob(tranId, findMeta).toInputStream();
				workspace.storeData(input);
			} finally {
				IOUtil.closeQuietly(input);
				writeTransaction(tranId) ;
				workspace.repository().logger().info(tranId + " applied") ;
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
		List<String> members = memberIds();

		if (members.size() <= 1)
			return new String[0];

		for (String member : members) {
			if (member.equals(repoId))
				continue;
			return create(workspace, logMeta, member).readAll();
		}
		return new String[0];
	}

	public String historyTranPath() {
		return "/__transactions/" + repoId ;
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
		Metadata transMeta = logMeta.get(historyTranPath());
		if (transMeta == null)
			return EmptyReader;


		transMeta.path(historyTranPath()) ;
		return new BufferedReader(new InputStreamReader(workspace.logContent().gridBlob(historyTranPath(), transMeta).toInputStream()));
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

	@Deprecated
	public Cache<String, Metadata> logmeta() {
		return this.logMeta ;
	}

	@Deprecated
	public GridFilesystem logContent(){
		return workspace.logContent() ;
	}
	
	public String selfId() {
		return workspace.repository().memberId() ;
	}
	
	public List<String> memberIds() throws IOException {
		Metadata meta = logmeta().get(SERVERS_PATH);
		if (meta == null){
			meta = Metadata.create(SERVERS_PATH);
			logmeta().put(SERVERS_PATH, meta) ;
		}
		
		InputStream input = logContent().gridBlob(SERVERS_PATH, meta).toInputStream();
		String members = IOUtil.toStringWithClose(input) ;
		if (StringUtil.isBlank(members)) members = "{}" ;
		JsonObject json = JsonObject.fromString(members) ;
		
		List<String> result = ListUtil.newList();
		for(Entry<String, JsonElement> entry : json.entrySet()){
			result.add(entry.getValue().getAsString()) ;
		}
		
		return result;
	}

	public final static String SERVERS_PATH = "/__servers";
	public TranLogManager registerMember(String addressId, String repoId) throws IOException {
		Metadata meta = logmeta().get(SERVERS_PATH);
		if (meta == null){
			meta = Metadata.create(SERVERS_PATH);
			logmeta().put(SERVERS_PATH, meta) ;
		}
		
		InputStream input = logContent().gridBlob(SERVERS_PATH, meta).toInputStream();
		String members = IOUtil.toStringWithClose(input) ;
		if (StringUtil.isBlank(members)) members = "{}" ;
		JsonObject json = JsonObject.fromString(members) ;
		
		String jsonString = json.put(addressId, repoId).toString() ;
		ByteArrayInputStream binput = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
		GridOutputStream output = logContent().gridBlob(SERVERS_PATH, meta).outputStream();
		IOUtil.copyNClose(binput, output) ;
		return this ;
	}

	public void removeMember(List<Address> removed) throws IOException {
		Metadata meta = logmeta().get(SERVERS_PATH);
		if (meta == null){
			meta = Metadata.create(SERVERS_PATH);
			logmeta().put(SERVERS_PATH, meta) ;
		}
		
		InputStream input = logContent().gridBlob(SERVERS_PATH, meta).toInputStream();
		String members = IOUtil.toStringWithClose(input) ;
		if (StringUtil.isBlank(members)) members = "{}" ;
		JsonObject json = JsonObject.fromString(members) ;
		
		for (Address rem : removed) {
			json.remove(rem.toString()) ;
		}
		
		ByteArrayInputStream binput = new ByteArrayInputStream(json.toString().getBytes("UTF-8"));
		GridOutputStream output = logContent().gridBlob(SERVERS_PATH, meta).outputStream();
		IOUtil.copyNClose(binput, output) ;
	}

}
