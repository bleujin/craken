package net.ion.craken.node.bean;

import java.io.Serializable;
import java.util.Date;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestToChildBean extends TestBaseCrud{

	public void testIncludeChildBean() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/dev").property("name", "dev").property("deptno", 20)
					.addChild("manager").property("name", "bleujin").property("created", new Date()) ;
				return null ;
			}
		}).get() ;
		
		ReadNode dev = session.pathBy("/dev");
		final Dept devBean = dev.toBean(Dept.class);
		assertEquals("dev", devBean.name()) ;
		assertEquals(20, devBean.deptNo()) ;
		
		Debug.line(devBean.manager()) ;
	}
	
	
}

class Dept implements Serializable {
	
	private int deptno ;
	private String name ;
	private FlatPerson manager ;
	
	
	public String name(){
		return name ;
	}
	
	public int deptNo(){
		return deptno ;
	}
	
	public FlatPerson manager(){
		return manager ;
	}
	
}
