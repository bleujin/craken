package net.ion.rosetta.query;

public class And extends BinOp {
	public And(Filter a, Filter b) {
		super(a, b);
	}

	public boolean check(HasMetadata t) {
		return a.check(t) && b.check(t);
	}

	public String toString() {
		return "(" + a + " + " + b + ")";
	}
}
