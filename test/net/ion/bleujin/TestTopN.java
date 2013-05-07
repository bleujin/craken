package net.ion.bleujin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.ecs.xhtml.s;

import net.ion.craken.node.search.util.SortUtil;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

import junit.framework.TestCase;

public class TestTopN extends TestCase {

	public void testTopN() throws Exception {
		List<Integer> list = ListUtil.toList(3, 4, 6, 1, 2, 5, 2, 6, 9, 10, 9) ;
		List<Integer> sorted = SortUtil.selectTopN(list.iterator(), new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				return o1 - o2;
			}
		}, 3) ;

		Debug.line(sorted) ;
	}
	
	

}
