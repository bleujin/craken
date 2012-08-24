craken
======

adaptation of the Infinispan

<pre>


public class First extends TestCase{

	private Craken craken ;
	
	protected void setUp() throws Exception {
		this.craken = Craken.create();
		craken.globalConfig().transport().clusterName("my-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml") ;
		craken.start() ;
	}
	
	protected void tearDown() throws Exception {
		craken.stop() ;
	}
	
	public void xtestFirstServer() throws Exception {
		LegContainer<Employee> emps = craken.defineLeg(Employee.class);
		
		emps.newInstance(7756).name("bleujin").age(20).colors("bleu", "red", "white").save() ;
		emps.newInstance(8090).name("hero").age(21).colors("black", "green").save() ;
		
		
		Employee found = emps.findByKey(7756) ;
		assertEquals("bleujin", found.name()) ;
		assertEquals(20, found.age()) ;
		
		// update
		found.age(25).save() ;
		
		Employee refound = emps.findByKey(7756) ;
		assertEquals("bleujin", refound.name()) ;
		assertEquals(25, refound.age()) ;
		
		new InfinityThread().startNJoin() ;
	}
	
	public void xtestSecondServer() throws Exception {
		LegContainer<Employee> emps = craken.defineLeg(Employee.class);
		
		List<Employee> founds = emps.find(new EntryFilter<Employee>() {
			@Override
			public boolean filter(Employee entry) {
				return entry.isFavoriteColor("bleu") ;
			}
		}) ;
		assertEquals(1, founds.size()) ;
		assertEquals("bleujin", founds.get(0).name()) ;
		assertEquals(25, founds.get(0).age()) ;
	}
	
}

</pre>