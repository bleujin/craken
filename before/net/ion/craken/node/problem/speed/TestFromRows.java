package net.ion.craken.node.problem.speed;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.util.Debug;

public class TestFromRows extends TestBaseFromDB{

	
	
	// 160-200 sec
	public void testAfield() throws Exception {
		final long start = System.currentTimeMillis() ;
		int count = dc.createUserCommand("select artId, modSerNo, afieldId, typeCd, dValue, hashValue from afield_content_tblc ").execHandlerQuery(new ResultSetHandler<Integer>() {
			@Override
			public Integer handle(final ResultSet rs) throws SQLException {
				try {
					Debug.line(System.currentTimeMillis() - start) ;
					return readSession().tranSync(new TransactionJob<Integer>() {
						@Override
						public Integer handle(WriteSession wsession) throws Exception {
							int result = 0 ;
							wsession.iwconfig().ignoreIndex() ;
							while(rs.next()){
								wsession.pathBy("/articles/" + rs.getString("artId") + "/" + rs.getString("modSerNo"))
									.property(rs.getString("afieldId"), rs.getString("dvalue")).property("type", rs.getString("typeCd")) ;
								result++ ;
								if ((result % 10001) == 0) { // pathBy : 26 sec
									System.out.print('.') ;
									wsession.continueUnit() ;
								}
							}
							return result;
						}
					}) ;
				} catch (Exception e) {
					throw new SQLException(e) ;
				}
			}
		}) ;
		
		Debug.line(count, System.currentTimeMillis() - start) ;
	}
	
	
	// 120 sec
	public void testArticle() throws Exception {
		final long start = System.currentTimeMillis() ;
		int count = dc.createUserCommand("select catId, artId, modSerNo, artSubject, modUserId, artCont from article_tblc where isUsing = 'T'").execHandlerQuery(new ResultSetHandler<Integer>() {
			@Override
			public Integer handle(final ResultSet rs) throws SQLException {
				try {
					Debug.line(System.currentTimeMillis() - start) ;
					return readSession().tranSync(new TransactionJob<Integer>() {
						@Override
						public Integer handle(WriteSession wsession) throws Exception {
							wsession.iwconfig().ignoreIndex() ;
							int result = 0 ;
//							wsession.iwconfig().ignoreBodyField() ;
							while(rs.next()){
								wsession.pathBy("/articles/" + rs.getString("artId") + "/" + rs.getString("modSerNo"))
									.property("subject", rs.getString("artSubject")).property("modUserId", rs.getString("modUserId")).property("artcont", rs.getString("artCont")) ;
								result++ ;
								if ((result % 15001) == 0) { // pathBy-105, createBy-115, resetBy-112 , except clob : pathBy-183, createBy-37, resetBy-38 
									System.out.print('.') ;
									wsession.continueUnit() ;
								}
							}
							return result;
						}
					}) ;
				} catch (Exception e) {
					throw new SQLException(e) ;
				}
			}
		}) ;
		
		Debug.line(count, System.currentTimeMillis() - start) ;
	}
}
