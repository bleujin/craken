package net.ion.craken.node.search.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class SortUtil {

	public static <T> List<T> selectTopN(Iterator<T> values, Comparator<T> comparator, int top) {
		// Holding n+2 entries, to compute n+1 top items first
		Queue<T> topN = new PriorityQueue(top + 2, comparator);
		while (values.hasNext()) {
			T value = values.next();
			if (value != null) {
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
}
