package net.ion.craken.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class QualifiedArratyName extends ValueObject implements Iterable<String> {
	public final List<String> names;

	public QualifiedArratyName(List<String> names) {
		this.names = Collections.unmodifiableList(names);
	}

	public static QualifiedArratyName of(String... names) {
		return new QualifiedArratyName(Arrays.asList(names));
	}

	public Iterator<String> iterator() {
		return names.iterator();
	}
	
	public String last(){
		return names.get(names.size()-1) ;
	}
}
