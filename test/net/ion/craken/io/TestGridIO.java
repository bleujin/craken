package net.ion.craken.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.radon.impl.util.CsvReader;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.manager.DefaultCacheManager;

public class TestGridIO extends TestCase {

	
	private DefaultCacheManager dcm;
	private Cache<String, byte[]>  cache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Configuration conf = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC)
		.locking().lockAcquisitionTimeout(5000)
		.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty("location", "./resource/temp")
		.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build();
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
		.defaultClusteredBuilder()
			.transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
			.build();
		this.dcm = new DefaultCacheManager(gconfig, conf);
		this.cache = dcm.getCache("test.blob", true);
	}
	
	@Override
	protected void tearDown() throws Exception {
		dcm.stop() ;
		super.tearDown();
	}

	
	
	public void testJson() throws Exception {
		final Output tout = targetOutput();
		BufferedWriter writer = new BufferedWriter(tout.writer);
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		int max = 100000 ;
		
		List<JsonObject> js = ListUtil.newList() ;
		while(line != null && line.length > 0 && max-- > 0 ){
			JsonObject json = new JsonObject();
			for (int ii = 0, last = headers.length; ii < last ; ii++) {
				if (line.length > ii) {
					json.addProperty(headers[ii], line[ii]) ;
				}
			}
			js.add(json) ;
//			writer.write(json.toString()) ;
		}
		
		for (JsonObject j : js) {
			writer.write(j.toString()) ;
		}
		
		tout.close() ;
	}
	
	
	public void testJsonWriter() throws Exception {
		final Output tout = targetOutput();
		JsonWriter writer =  new JsonWriter(new BufferedWriter(tout.writer));
		
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		int max = 5000 ;
		
		
		writer.beginArray() ;
		while(line != null && line.length > 0 && max-- > 0 ){
			writer.beginObject() ;
			for (int ii = 0, last = headers.length; ii < last ; ii++) {
				if (line.length > ii) {
					writer.name(headers[ii]).value(line[ii]) ;
				}
			}
			writer.endObject() ;
		}
		writer.endArray() ;
		tout.close() ;
	}
	
	public void testGrid() throws Exception {
		final Output tout = targetOutput();
		BufferedWriter writer =  new BufferedWriter(tout.writer);
		
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		int max = 100000 ;
		
		
		for (String header : headers) {
			writer.write(header) ;
		}
		writer.write("\n") ;
		while(line != null && line.length > 0 && max-- > 0 ){
			for (int ii = 0, last = headers.length; ii < last ; ii++) {
				if (line.length > ii) {
					writer.write(headers[ii] + "," +  line[ii]) ;
				}
			}
			writer.write("\n") ;
		}
		tout.close() ;
	}

	private Output targetOutput() throws IOException {
		int defaultChunkSize = 32 * 1024;
		GridFilesystem gfs = new GridFilesystem(cache, defaultChunkSize);
		
		GridBlob gblob = gfs.gridBlob("/root/temp");
		GridOutputStream output = gfs.getOutput(gblob, false);
		Writer writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));

		
		return new Output(writer, gblob) ;
//		return new JsonWriter(writer);
	}
	
	

	public void xtestJsonWriter() throws Exception {
		final Output tout = targetOutput();
		JsonWriter writer =  new JsonWriter(new BufferedWriter(tout.writer));
		
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		int max = 100000 ;
		
		
		writer.beginArray() ;
		while(line != null && line.length > 0 && max-- > 0 ){
			writer.beginObject() ;
			for (int ii = 0, last = headers.length; ii < last ; ii++) {
				if (line.length > ii) {
					writer.name(headers[ii]).value(line[ii]) ;
				}
			}
			writer.endObject() ;
		}
		writer.endArray() ;
		writer.flush() ;
		tout.close() ;
	}
	
	public void testReadIndex() throws Exception {
//		Central central = CentralConfig.newLocalFile().dirFile("./resource/temp").build();
		
		long start = System.currentTimeMillis() ;
		int defaultChunkSize = 32 * 1024;
		GridFilesystem gfs = new GridFilesystem(cache, defaultChunkSize);
		Metadata mdata = Metadata.loadFromJsonString("{'path':'/root/temp','length':45900001,'modificationTime':1383532359094,'chunkSize':32768,'flags':1,'type':'blob'}");
		Reader input = new BufferedReader(new InputStreamReader(gfs.getInput(gfs.getGridBlob("/root/temp", mdata)), Charset.forName("UTF-8")));
		
//		long size = 0 ;
//		while(input.read() != -1){
//			size++ ;
//		}
//		input.close() ;
		
		String str = IOUtil.toStringWithClose(input) ;
		JsonParser.fromString(str).getAsJsonArray() ;
		
		Debug.line(System.currentTimeMillis() - start) ;
		
	}
	
	public void testWriteRead() throws Exception {
		int defaultChunkSize = 32 * 1024;
		GridFilesystem gfs = new GridFilesystem(cache, defaultChunkSize);
		
		GridBlob gb = gfs.gridBlob("/a/b/c");
		
	}
	
	
	
	
}

class Output {
	
	Writer writer;
	GridBlob gb;

	Output(Writer writer, GridBlob gb){
		this.writer = writer ;
		this.gb = gb ;
	}

	public void close() throws IOException {
		writer.flush() ;
		writer.close() ;
		Debug.line(gb.getMetadata().asPropertyValue().toString()) ;
	}
	
	
	
}






