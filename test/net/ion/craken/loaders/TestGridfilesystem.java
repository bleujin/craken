package net.ion.craken.loaders;

import java.io.File;
import java.io.OutputStream;

import junit.framework.TestCase;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import org.infinispan.Cache;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.tx.PrepareCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.io.GridFile.Metadata;
import org.infinispan.io.GridFilesystem;
import org.infinispan.manager.DefaultCacheManager;

public class TestGridfilesystem extends TestCase {

	
	public void testInterceptor() throws Exception {
		DefaultCacheManager dm = new DefaultCacheManager() ;
		Cache<String, Metadata> metadata = dm.getCache("metadata") ;
		Cache<String, byte[]> data = dm.getCache("data") ;
		
		data.getAdvancedCache().addInterceptor(new BaseCustomInterceptor(){
			protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
				switch(command.getCommandId()){
				
					case CommitCommand.COMMAND_ID :
						Debug.line("commit", command.getParameters()) ;
						break ;
					case PrepareCommand.COMMAND_ID :
						Debug.line("prepare" ,command.getParameters().length, command.getParameters(), ((PrepareCommand)command).getModifications()) ;
						break ;
					default :
						Debug.line("other", command, command.getParameters());
						break ;
				}
				return invokeNextInterceptor(ctx, command);
			}
		}, 0);
		
		
		GridFilesystem gfs = new GridFilesystem(data, metadata) ;
		
		File file = gfs.getFile("/bleuijn") ;
		file.mkdirs() ;
		
		OutputStream output = gfs.getOutput("/bleuijn/data.node") ;
		IOUtil.copyNClose(new StringInputStream("Hello World"), output);
		
		
		
		dm.stop(); 
	}
}
