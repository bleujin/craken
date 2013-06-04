package net.ion.craken.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class RelationName extends ValueObject implements Iterable<String> {
	public final List<String> names;

	public RelationName(List<String> names) {
		this.names = Collections.unmodifiableList(names);
	}

	public static RelationName of(String... names) {
		return new RelationName(Arrays.asList(names));
	}

	public Iterator<String> iterator() {
		return names.iterator();
	}
}
