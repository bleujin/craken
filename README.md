craken
======

adaptation of the Infinispan


## 

``` html
public class TestHelloWord extends TestCase {

	public void testHello() throws Exception {
		Repository r = RepositoryImpl.testSingle() ;
		r.start() ;
		ReadSession session = r.testLogin("mywork") ;
		
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




```


Html Pt : http://ec2-46-51-233-197.ap-northeast-1.compute.amazonaws.com:9000/craken/index.htm