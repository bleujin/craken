package net.ion.nsearcher.search.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.OpenBitSetDISI;

public class BooleanFilter extends Filter implements Iterable<FilterClause> {

	private final List<FilterClause> clauses = new ArrayList<FilterClause>();

	/**
	 * Returns the a DocIdSetIterator representing the Boolean composition of the filters that have been added.
	 */
	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		FixedBitSet res = null;

		boolean hasShouldClauses = false;
		for (final FilterClause fc : clauses) {
			if (fc.getOccur() == Occur.SHOULD) {
				hasShouldClauses = true;
				final DocIdSetIterator disi = getDISI(fc.getFilter(), reader);
				if (disi == null)
					continue;
				if (res == null) {
					res = new FixedBitSet(reader.maxDoc());
				}
				res.or(disi);
			}
		}
		if (hasShouldClauses && res == null)
			return DocIdSet.EMPTY_DOCIDSET;

		for (final FilterClause fc : clauses) {
			if (fc.getOccur() == Occur.MUST_NOT) {
				if (res == null) {
					assert !hasShouldClauses;
					res = new FixedBitSet(reader.maxDoc());
					res.set(0, reader.maxDoc()); // NOTE: may set bits on deleted docs
				}
				final DocIdSetIterator disi = getDISI(fc.getFilter(), reader);
				if (disi != null) {
					res.andNot(disi);
				}
			}
		}

		for (final FilterClause fc : clauses) {
			if (fc.getOccur() == Occur.MUST) {
				final DocIdSetIterator disi = getDISI(fc.getFilter(), reader);
				if (disi == null) {
					return DocIdSet.EMPTY_DOCIDSET; // no documents can match
				}
				if (res == null) {
					res = new FixedBitSet(reader.maxDoc());
					res.or(disi);
				} else {
					res.and(disi);
				}
			}
		}

		return res != null ? res : DocIdSet.EMPTY_DOCIDSET;
	}

	private static DocIdSetIterator getDISI(Filter filter, IndexReader reader) throws IOException {
		final DocIdSet set = filter.getDocIdSet(reader);
		return (set == null || set == DocIdSet.EMPTY_DOCIDSET) ? null : set.iterator();
	}

	/**
	 * Adds a new FilterClause to the Boolean Filter container
	 * 
	 * @param filterClause
	 *            A FilterClause object containing a Filter and an Occur parameter
	 */
	public void add(FilterClause filterClause) {
		clauses.add(filterClause);
	}

	public final void add(Filter filter, Occur occur) {
		add(new FilterClause(filter, occur));
	}

	/**
	 * Returns the list of clauses
	 */
	public List<FilterClause> clauses() {
		return clauses;
	}

	/**
	 * Returns an iterator on the clauses in this query. It implements the {@link Iterable} interface to make it possible to do:
	 * 
	 * <pre>
	 * for (FilterClause clause : booleanFilter) {
	 * }
	 * </pre>
	 */
	public final Iterator<FilterClause> iterator() {
		return clauses().iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if ((obj == null) || (obj.getClass() != this.getClass())) {
			return false;
		}

		final BooleanFilter other = (BooleanFilter) obj;
		return clauses.equals(other.clauses);
	}

	@Override
	public int hashCode() {
		return 657153718 ^ clauses.hashCode();
	}

	/** Prints a user-readable version of this Filter. */
	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder("BooleanFilter(");
		final int minLen = buffer.length();
		for (final FilterClause c : clauses) {
			if (buffer.length() > minLen) {
				buffer.append(' ');
			}
			buffer.append(c);
		}
		return buffer.append(')').toString();
	}
}