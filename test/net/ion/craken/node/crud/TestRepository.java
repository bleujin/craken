package net.ion.craken.node.crud;

import net.ion.framework.util.Debug;
import net.ion.framework.util.PathMaker;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;

import junit.framework.TestCase;

public class TestRepository extends TestCase {

	public void xtestNodeName() throws Exception {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("ics6")
			.addProperty("configurationFile",PathMaker.getFilePath("./ics", "/WEB-INF/jgroups-udp.xml")).build();
		Debug.line(gconfig.transport().nodeName()) ;
	}
	
	

}
