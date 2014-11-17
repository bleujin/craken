package net.ion.craken.template;

import junit.framework.TestCase;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJobs;
import net.ion.nsearcher.search.SearchResponse;

public class TestArgument extends TestCase {

	public void testReadDoc() throws Exception {
		Central c = CentralConfig.newRam().build();

		c.newIndexer().index(IndexJobs.create("/bleujin", 10));
		SearchResponse response = c.newSearcher().search("");

		Engine engine = Engine.createDefaultEngine();
		String template = IOUtil.toStringWithClose(getClass().getResourceAsStream("tem.tpl"));

		String result = engine.transform(template, MapUtil.<Object> chainKeyMap().put("response", response).toMap());

		response.getDocument().get(0).asString("id");

		Debug.line(result);
	}
}
