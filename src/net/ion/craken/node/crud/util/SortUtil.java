package net.ion.craken.node.crud.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import com.google.common.base.Predicate;

public class SortUtil {

	public static <T> List<T> selectTopN(Iterator<T> values, Predicate<T> filter, Comparator<T> comparator, int top) {
		Queue<T> topN = new PriorityQueue(100, comparator);
		while (values.hasNext()) {
			T value = values.next();
			if (value != null && filter != null && filter.apply(value)) {
				if (topN.size() <= top) {
					topN.add(value);
				} else {
					if (comparator.compare(value, topN.peek()) > 0) {
						topN.poll();
						topN.add(value);
					}
				}
			}
		}

		if (topN.isEmpty()) {
			return Collections.emptyList();
		}
		while (topN.size() > top) {
			topN.poll();
		}

		List<T> result = new ArrayList(topN);
		Collections.sort(result, Collections.reverseOrder(comparator));
		return result;
	}
	
	public static <T> List<T> selectTopN(Iterator<T> values, Comparator<T> comparator, int top) {
		return selectTopN(values, (Predicate)null, comparator, top) ;
	}
}
