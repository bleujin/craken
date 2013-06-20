package net.ion.craken.node.convert.to.type;

import java.io.Serializable;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.bean.ProxyBean;
import net.ion.craken.node.convert.bean.TypeStrategy;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestPrimitiveProperty extends TestBaseCrud{

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/primitive").property("bo", true).property("by", ((byte)95)).property("ch", 'a').property("dou", 3.0d)
					.property("fl", 3.0f).property("in", 3).property("lo", 3L).property("sh", (short)30) ;
				return null;
			}
		}).get() ;
	}
	
	public void testPrimitiveType() throws Exception {
		
		ReadNode node = session.pathBy("/primitive");
		PrimitiveBean bean = node.toBean(PrimitiveBean.class) ;
		
		assertEquals(true, bean.isBo()) ;
		assertEquals((byte)95, bean.getBy()) ;
		assertEquals('a', bean.getCh()) ;
		assertEquals(3.0d, bean.getDou()) ;
		assertEquals(3.0f, bean.getFl()) ;
		assertEquals(3, bean.getIn()) ;
		assertEquals(3L, bean.getLo()) ;
		assertEquals((short)30, bean.getSh()) ;
	}
	
	public void testPrimitiveWrapperType() throws Exception {
		long start = System.currentTimeMillis() ;
		ReadNode node = session.pathBy("/primitive");
		PrimitiveWrapperBean bean = node.toBean(PrimitiveWrapperBean.class) ;
		
		
		Debug.line(System.currentTimeMillis() - start) ;
	}

	public void testPrimitiveWrapperArrayType() throws Exception {
		ReadNode node = session.pathBy("/primitive");
		PrimitiveArrayWrapperBean bean = ProxyBean.create(TypeStrategy.DEFAULT, node, PrimitiveArrayWrapperBean.class) ;
		
		assertEquals(Boolean.TRUE, bean.isBo()[0]) ;
		assertEquals((byte)95, (byte)(bean.getBy()[0])) ;
		assertEquals(new Character('a'), bean.getCh()[0]) ;
		assertEquals(new Double(3.0d), bean.getDou()[0]) ;
		assertEquals(new Float(3.0f), bean.getFl()[0]) ;
		assertEquals(new Integer(3), bean.getIn()[0]) ;
		assertEquals(new Long(3L), bean.getLo()[0]) ;
		assertEquals(new Short((short)30), bean.getSh()[0]) ;
	}

	public void testPrimitiveArrayType() throws Exception {
		ReadNode node = session.pathBy("/primitive");
		PrimitiveArrayBean bean = node.toBean(PrimitiveArrayBean.class) ;
		
		assertEquals(true, bean.isBo()[0]) ;
		assertEquals((byte)95, bean.getBy()[0]) ;
		assertEquals('a', bean.getCh()[0]) ;
		assertEquals(3.0d, bean.getDou()[0]) ;
		assertEquals(3.0f, bean.getFl()[0]) ;
		assertEquals(3, bean.getIn()[0]) ;
		assertEquals(3L, bean.getLo()[0]) ;
		assertEquals((short)30, bean.getSh()[0]) ;
	}
	
	public void testNullPrimitiveArray() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/null") ;
				return null;
			}
		}).get() ;
		ReadNode node = session.pathBy("/null");
		PrimitiveArrayBean bean = node.toBean(PrimitiveArrayBean.class) ;
		
		assertEquals(true, bean.isBo() == null) ;
		assertEquals(true, bean.getBy() == null) ;
		assertEquals(true, bean.getCh() == null) ;
		assertEquals(true, bean.getDou() == null) ;
		assertEquals(true, bean.getFl() == null) ;
		assertEquals(true, bean.getIn() == null) ;
		assertEquals(true, bean.getLo() == null) ;
		assertEquals(true, bean.getSh() == null) ;

	}

	public void testNullPrimitive() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/null") ;
				return null;
			}
		}).get() ;
		ReadNode node = session.pathBy("/null");
		PrimitiveBean bean = node.toBean(PrimitiveBean.class) ;
		
		assertEquals(true, bean.isBo() == false) ;
		assertEquals(true, bean.getBy() == (byte)0) ;
		assertEquals(true, bean.getDou() == 0.0d) ;
		assertEquals(true, bean.getFl() == 0.0f) ;
		assertEquals(true, bean.getIn() == 0) ;
		assertEquals(true, bean.getLo() == 0L) ;
		assertEquals(true, bean.getSh() == (short)0) ;
//		assertEquals(true, bean.getCh() == '') ;

	}

}

class PrimitiveArrayBean implements Serializable{
	private boolean[] bo ;
	private byte[] by ;
	private char[] ch ;
	private double[] dou ;
	private float[] fl ;
	private int[] in ;
	private long[] lo ;
	private short[] sh ;
	
	public boolean[] isBo() {
		return bo;
	}
	public byte[] getBy() {
		return by;
	}
	public char[] getCh() {
		return ch;
	}
	public double[] getDou() {
		return dou;
	}
	public float[] getFl() {
		return fl;
	}
	public int[] getIn() {
		return in;
	}
	public long[] getLo() {
		return lo;
	}
	public short[] getSh() {
		return sh;
	}
	
}

class PrimitiveArrayWrapperBean implements Serializable{
	private Boolean[] bo ;
	private Byte[] by ;
	private Character[] ch ;
	private Double[] dou ;
	private Float[] fl ;
	private Integer[] in ;
	private Long[] lo ;
	private Short[] sh ;
	
	public Boolean[] isBo() {
		return bo;
	}
	public Byte[] getBy() {
		return by;
	}
	public Character[] getCh() {
		return ch;
	}
	public Double[] getDou() {
		return dou;
	}
	public Float[] getFl() {
		return fl;
	}
	public Integer[] getIn() {
		return in;
	}
	public Long[] getLo() {
		return lo;
	}
	public Short[] getSh() {
		return sh;
	}
	
}

class PrimitiveWrapperBean implements Serializable{
	private Boolean bo ;
	private Byte by ;
	private Character ch ;
	private Double dou ;
	private Float fl ;
	private Integer in ;
	private Long lo ;
	private Short sh ;
	
	public Boolean isBo() {
		return bo;
	}
	public Byte getBy() {
		return by;
	}
	public Character getCh() {
		return ch;
	}
	public Double getDou() {
		return dou;
	}
	public Float getFl() {
		return fl;
	}
	public Integer getIn() {
		return in;
	}
	public Long getLo() {
		return lo;
	}
	public Short getSh() {
		return sh;
	}
}

class PrimitiveBean implements Serializable{
	private boolean bo ;
	private byte by ;
	private char ch ;
	private double dou ;
	private float fl ;
	private int in ;
	private long lo ;
	private short sh ;
	
	public boolean isBo() {
		return bo;
	}
	public byte getBy() {
		return by;
	}
	public char getCh() {
		return ch;
	}
	public double getDou() {
		return dou;
	}
	public float getFl() {
		return fl;
	}
	public int getIn() {
		return in;
	}
	public long getLo() {
		return lo;
	}
	public short getSh() {
		return sh;
	}
	
}