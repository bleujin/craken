package net.ion.craken.node.crud;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.ecs.xhtml.th;

import net.ion.craken.node.crud.util.SortUtil;
import net.ion.framework.util.SetUtil;
import junit.framework.TestCase;

public class TestRespositoryListener extends TestCase{

	
	public void testMyStatus() throws Exception {
		MyStatus status = new MyStatus().started(100L).lastModified(100L).memeberName("bleujin");
		
		MyStatus read = MyStatus.fromJson(status.toJsonString());
		assertEquals(true, read.equals(status)) ;
	}
	
	public void testCompare() throws Exception {
		MyStatus first = new MyStatus().started(100L).lastModified(100L).memeberName("bleujin");
		MyStatus third = new MyStatus().started(100L).lastModified(300L).memeberName("bleujin");
		MyStatus second = new MyStatus().started(100L).lastModified(200L).memeberName("bleujin");
		
		ArrayList<MyStatus> list = new ArrayList(SetUtil.create(first, third, second));
		Collections.sort(list) ;
		
		assertEquals(first, list.get(0)) ;
		assertEquals(third, list.get(list.size() - 1)) ;
		
	}
}
