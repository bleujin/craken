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
}

```


Html Pt : http://htmlpreview.github.com/?https://github.com/bleujin/craken/master/resource/docs/html/index.htm#/1