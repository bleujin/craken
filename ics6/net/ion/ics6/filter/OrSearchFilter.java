package net.ion.ics6.filter;

import net.ion.craken.node.crud.Filters;
import net.ion.nsearcher.search.filter.FilterUtil;

import org.apache.lucene.search.Filter;

public class OrSearchFilter {

	public static Filter wildcard(String value, String[] cols) {
		Filter[] filters = new Filter[cols.length];

		for(int i = 0; i < cols.length; i++) {
			filters[i] = Filters.wildcard(cols[i], "*" + value + "*");
		}

		return FilterUtil.or(filters);
	}

}
