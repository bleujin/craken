package net.ion.ics6.dfile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.ByteObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.radon.core.ContextParam;

import org.apache.ecs.xhtml.font;
import org.jboss.resteasy.plugins.providers.multipart.InputBody;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.HttpRequest;

@Path("/file")
public class FileUploadWeb {

	private ReadSession rsession;
	FileUploadWeb(@ContextParam("FileContext") FileContext fcontext) throws IOException{
		this.rsession = fcontext.craken().login("fcommon") ;
	}
	
	@Path("/upload")
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String parseEntityInfo(@Context HttpRequest request, MultipartFormDataInput  input) throws IOException {
// http://www.mkyong.com/webservices/jax-rs/file-upload-example-in-resteasy/
		Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
		for (Entry<String, List<InputPart>> entry : uploadForm.entrySet()) {
			for (InputPart part : entry.getValue()) {
				
				InputBody ib = InputBody.create(entry.getKey(), part) ;
				if (ib.isFilePart()){
					
					final String fname = "./resource/temp/fs/" + ib.filename();
					FileOutputStream fos = new FileOutputStream(fname) ;
					IOUtil.copyNClose(ib.asStream(), fos) ;
					
					rsession.tran(new TransactionJob<Void>() {
						@Override
						public Void handle(WriteSession wsession) throws Exception {
							byte[] bytes = IOUtil.toByteArrayWithClose(new FileInputStream(fname)) ;
							wsession
								.pathBy("/upload/edit").property("path", fname)
								.property("bytes", new ByteObject(bytes))
								.property("address", wsession.workspace().repository().addressId());
							return null;
						}
					}) ;
				}
//				Debug.line(ib.name(), ib.isFilePart(), ib.mediaType(), ib.charset(), ib.transferEncoding(), ib.filename(), ib.asStream());
			}
		}

		return "";
	}
}
