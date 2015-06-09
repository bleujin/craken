package net.ion.craken.db;

import java.sql.SQLException;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.exception.AlreadyExistsException;
import net.ion.framework.db.DBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.servant.StdOutServant;

public class TestBaseFnManager extends TestCase{

	protected DBController dc;
	private Craken r;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = Craken.inmemoryCreateWithTest() ;
		this.session = r.start().login("test") ;
		
		
		CrakenFnManager dbm = registerFunction() ;
		this.dc = new DBController("craken", dbm, new StdOutServant());
		dc.initSelf() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		dc.destroySelf();
		r.shutdown() ;
	}

	private CrakenFnManager registerFunction() {
		CrakenFnManager dbm = new CrakenFnManager(this.r, "test") ;
		
		dbm.register("dummy", new QueryPackage(){
			
			@Function("addPersonWith")
			public int addPerson(final String name, final int age, final String address) throws Exception{
				return session().tranSync(new TransactionJob<Integer>() {
					public Integer handle(WriteSession wsession) {
						wsession.pathBy("/persons/" + name).property("name", name).property("age", age).property("address", address) ;
						return 1 ;
					}
				}) ;
			}
			
			public int batchWith(final String[] names, final int[] ages, final String[] address) throws Exception{
				return session().tranSync(new TransactionJob<Integer>() {
					public Integer handle(WriteSession wsession) {
						
						
						WriteNode persons = wsession.pathBy("/persons") ;
						for (int i =0 ; i < names.length ; i++) {
							if (wsession.exists("/persons/" + names[i])) throw new AlreadyExistsException(Fqn.fromString("/persons/" + names[i])) ;

							persons.child(names[i]).property("name", names[i]).property("age", ages[i]).property("address", address[i]) ;
						}
						return 1 ;
					}
				}) ;
			}
			
			public Rows findPersonBy(String name){
				return session().ghostBy("/persons").child(name).toRows("name, age");
			}
			
			public Rows listPersonBy() throws SQLException{
				return session().ghostBy("/persons").children().toAdRows("name, age");
			}
			
			public String toString(){
				return "dummy package" ;
			}
		}) ;
		return dbm ;
	}

}
