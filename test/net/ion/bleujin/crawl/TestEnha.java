package net.ion.bleujin.crawl;

import java.net.URLDecoder;
import java.net.URLEncoder;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;

public class TestEnha extends TestCase {

	public void testHanTOUTF() throws Exception {
		String name = "야구" ;
		String expect = "%EC%95%BC%EA%B5%AC" ;
		
		assertEquals(expect, URLEncoder.encode(name, "UTF-8")) ;
	}
	
	public void testUTF2() throws Exception {
		String name = "1998 방콕 아시안 게임" ;
		String expect = "1998%20%EB%B0%A9%EC%BD%95%20%EC%95%84%EC%8B%9C%EC%95%88%20%EA%B2%8C%EC%9E%84" ;
		Debug.line(URLEncoder.encode(name, "UTF-8"));
		// assertEquals(expect, URLEncoder.encode(name, "UTF-8")) ;
	}
	
	public void testDecode() throws Exception {
		String name = "1998%20%EB%B0%A9%EC%BD%95%20%EC%95%84%EC%8B%9C%EC%95%88%20%EA%B2%8C%EC%9E%84" ;
		Debug.line(URLDecoder.decode(name, "UTF-8")) ;
	}
	
	public void testHanPath() throws Exception {
		Repository r = Craken.inmemoryCreateWithTest() ;
		ReadSession session = r.login("test") ;
		
		final String name = "1998 방콕 아시안 게임" ;
		final String name2 = "야구" ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/", name, name2).property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		
		assertEquals("bleujin", session.pathBy("/", name, name2).property("name").asString()) ;
		session.workspace().repository().shutdown() ;
	}
}
