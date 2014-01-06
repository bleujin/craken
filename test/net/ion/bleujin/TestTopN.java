package net.ion.bleujin;

import java.util.Comparator;
import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.node.crud.util.SortUtil;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

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
