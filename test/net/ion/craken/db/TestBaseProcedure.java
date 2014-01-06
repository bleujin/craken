package net.ion.craken.db;

import java.sql.SQLException;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.exception.AlreadyExistsException;
import net.ion.craken.tree.Fqn;
import net.ion.framework.db.DBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.servant.StdOutServant;

public class TestBaseProcedure extends TestBaseCrud{

	protected DBController dc;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		CrakenManager dbm = registerFunction() ;
		this.dc = new DBController("craken", dbm, new StdOutServant());
		dc.initSelf() ;
	}

	
	private CrakenManager registerFunction() {
		CrakenManager dbm = new CrakenManager(this.r, "mywork.node") ;
		
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
