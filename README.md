craken
======

adaptation of the Infinispan

<pre>


public class TestCreate extends TestCase{

	private Craken craken ;
	
	protected void setUp() throws Exception {
		this.craken = Craken.create();
		craken.globalConfig().transport().clusterName("my-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml") ;
		craken.start() ;
	}
	
	protected void tearDown() throws Exception {
		craken.stop() ;
	}
	
	public void testCreate() throws Exception {
		LegContainer<SimpleMapNode> container = craken.defineLeg(SimpleMapNode.class,  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).jmxStatistics().enable().clustering().invocationBatching().build()) ;
		craken.addListener(new ContainerListener()) ;

		container.addListener(new EntryListener()) ;

		while(true){
			SimpleMapNode node = container.newInstance("bleujin" + RandomUtil.nextInt(10)).put("age", RandomUtil.nextInt(100)).put("server", craken.getManager().getAddress().toString());
			node.save() ;
			Thread.sleep(1000) ;
		}
	}
	
	
	public void testEmpNode() throws Exception {
		LegContainer<EmpNode> container = craken.defineLeg(EmpNode.class,  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).jmxStatistics().enable().clustering().invocationBatching().build()) ;
//		container.addListener(new EntryListener()) ;
		
		EmpNode emp = container.newInstance("7789") ;
		emp.name("bleujin").age(20) ;
		emp.save() ;
		
		assertEquals("bleujin", container.findByKey("7789").name()) ;
	}
	
	
	public void testConfirmEmp() throws Exception {
		LegContainer<EmpNode> container = craken.findLeg(EmpNode.class) ;
		
		for (NodeKey key : container.keySet()){
			Debug.line(container.findByKey(key)) ;
		}
	}
	
	
	public void testRET() throws Exception {
		LegContainer<SimpleMapNode> container = craken.findLeg(SimpleMapNode.class) ;
		
		container.newInstance("bleujin").put("name", "bleujin").put("age", 20).save() ;
		container.newInstance(7756).put("name", "hero").put("age", 364).save() ;
		
		
		SimpleMapNode bleujin = container.findByKey("bleujin") ;
		Debug.line(bleujin.field("age"), bleujin.fieldAsInt("age") + 1, bleujin.fieldAsString("name")) ;

		SimpleMapNode f7756 = container.findByKey(7756) ;
		Debug.line(f7756.field("age"), f7756.fieldAsInt("age") + 1, f7756.fieldAsString("name")) ;

	}
	
	
	public void testFindOne() throws Exception {
		LegContainer<SimpleMapNode> container = craken.findLeg(SimpleMapNode.class) ;
		
		container.newInstance("bleujin").put("name", "bleujin").put("age", 20).save() ;
		container.newInstance(7756).put("name", "hero").put("age", 364).save() ;

		SimpleMapNode found = container.findOne(new NodeFilter<SimpleMapNode>(){
			public boolean filter(SimpleMapNode node){
				return node.fieldAsInt("age") > 100;
			}
		}) ;
		
		assertEquals(364, found.fieldAsInt("age")) ;
	}
	
	
	public void testFind() throws Exception {
		LegContainer<SimpleMapNode> container = craken.findLeg(SimpleMapNode.class) ;
		
		for (int i = 0; i < 15 ; i++) {
			container.newInstance("user" + i).put("index", i).save() ;
		}

		List<SimpleMapNode> found = container.find(new NodeFilter<SimpleMapNode>(){
			public boolean filter(SimpleMapNode node){
				return node.fieldAsInt("index") < 5;
			}
		}) ;
		
		assertEquals(5, found.size()) ;

		List<SimpleMapNode> foundPage = container.find(new NodeFilter<SimpleMapNode>(){
			public boolean filter(SimpleMapNode node){
				return node.fieldAsInt("index") < 5;
			}
		}, Page.create(2, 2)) ;
		
		assertEquals(2, foundPage.size()) ;
	}

	
	
	@Listener
	public class ContainerListener {
		@CacheStarted
		public void cacheStarted(CacheStartedEvent e){
			Debug.line('c', "") ;
		} 
		
		@CacheStopped
		public void cacheStopped(CacheStoppedEvent e){
		}
		
		@ViewChanged
		public void viewChanged(ViewChangedEvent e){
			Debug.line('x', e.getOldMembers(), e.getNewMembers()) ;
		}
	}
	
	

	@Listener
	public class EntryListener {

		public EntryListener() {
		}

		@CacheEntryCreated
		public void cacheEntryCreated(CacheEntryCreatedEvent<NodeKey, AbstractNode> e) {
			// if (!e.isPre()) Debug.line(e.getKey()) ;
		}

		@CacheEntryModified
		public void cacheEntryModified(CacheEntryModifiedEvent<NodeKey, AbstractNode> e) {
			if (!e.isPre()) Debug.line(e.getKey(), e.getValue()) ;
		}
	}
	
	
}

</pre>