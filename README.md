craken
======

adaptation of the Infinispan


## 

``` html
public class TestHelloWord extends TestCase {

	public void testHello() throws Exception {
		Craken r = Craken.inmemoryCreateWithTest() ;
		r.start() ;
		ReadSession session = r.login("test") ;
		
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/hello").property("greeting", "Hello World") ;
				return null;
			}
		}) ;
		
		assertEquals("Hello World", session.pathBy("/hello").property("greeting").value()) ;
		r.shutdown() ;
	}
	
	
	public void testBlobIO() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode bleujin = wsession.pathBy("/bleujin/my").blob("config", new FileInputStream("./resource/config/server-simple.xml"));
				return null;
			}
		}) ;
		
		final PropertyValue property = session.pathBy("/bleujin/my").property("config");

		final GridBlob blob = property.asBlob();
		InputStream input = blob.toInputStream() ;
		String str = IOUtil.toStringWithClose(input) ;
		
	}
}


public class TestGrid extends TestCase {

	public void testRun() throws Exception {
		Craken craken = Craken.create() ;
		craken.createWorkspace("grid", WorkspaceConfigBuilder.gridDir("./resource/grid")) ;
		
		.....
		
		craken.shutdown() ;
	}


}


```


Html Pt : https://cdn.rawgit.com/bleujin/craken/master/resource/docs/slide/index.htm#landing-slide <br/>
doc : ..ing