package net.ion.craken.loaders;

import static net.ion.radon.repository.NodeConstants.ARADON_GROUP;
import static net.ion.radon.repository.NodeConstants.ARADON_UID;
import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;
import net.ion.radon.repository.Node;
import net.ion.radon.repository.PropertyFamily;
import net.ion.radon.repository.RepositoryCentral;
import net.ion.radon.repository.Session;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;

public class TestPerf extends TestCase {


	public void xtestNodeSpeed() throws Exception {
		RepositoryCentral rc = RepositoryCentral.testCreate();
		Session session = rc.login("speed");
		
		long start = System.currentTimeMillis() ;
		for (int i = 0; i < 10000 ; i++) {
			session.newNode().setAradonId(Person.class.getCanonicalName(), RandomUtil.nextRandomString(10) + i)
				.inner("fields").put("key", RandomUtil.nextRandomString(10)).put("age", 30).inner("address").put("city", "seoul")
				.getParent().put("__value", RandomUtil.nextRandomString(80).getBytes()) ;
			
			session.commit() ;
			if (i % 1000 == 0) Debug.line(i / 1000, System.currentTimeMillis() - start) ;
		}
	}
	
	public void xtestRare() throws Exception {
		MongoOptions moptions = new MongoOptions();
		moptions.autoConnectRetry = true;
		moptions.connectionsPerHost = 100;
		moptions.threadsAllowedToBlockForConnectionMultiplier = 10;
		ServerAddress srvAddr = new ServerAddress("61.250.201.78", 27017);
		Mongo m = new Mongo(srvAddr, moptions);
		// setting WriteConcern to true enables fsync, however performance degradation is very big: 5-10 times!
		// It makes sense to enable it only on particular updates (1.4ms vs 12ms fsynced per 1KB update)
//		m.setWriteConcern(WriteConcern.SAFE);
		DBCollection coll = m.getDB("craken").getCollection("rspeed");
	
		BasicDBObject aradon_options = new BasicDBObject();
		aradon_options.put("name", "_aradon_id");
		aradon_options.put("unique", Boolean.TRUE);
	
		coll.ensureIndex(PropertyFamily.create(ARADON_GROUP, 1).put(ARADON_UID, -1).getDBObject(), aradon_options);

		RepositoryCentral rc = RepositoryCentral.testCreate();
		Session session = rc.login("rspeed");
		long start = System.currentTimeMillis() ;
		for (int i = 0; i < 5000 ; i++) {
			Node node = session.newNode();
			node.setAradonId(Person.class.getCanonicalName(), RandomUtil.nextRandomString(10) + i)
				.inner("fields").put("key", RandomUtil.nextRandomString(10)).put("age", 30).inner("address").put("city", "seoul")
				.getParent().put("__value", RandomUtil.nextRandomString(80).getBytes()) ;
			
			coll.save(node.getDBObject()) ;
			
			if (i % 1000 == 0) Debug.line(i / 1000, System.currentTimeMillis() - start) ;
		}
		
	}
}
