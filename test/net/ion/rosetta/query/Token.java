package net.ion.rosetta.query;

import java.math.BigInteger;
import java.util.regex.Pattern;


public class Token implements Filter {

	public final boolean falseify;
	public final String name;
	public final Constants property;
	public final String value;
	public final Match match;

	public Token(String t) {
		System.out.println("token: " + t);
		String p;

		int i = t.indexOf(':');
		if (i > 0) {
			p = t.substring(0, i);
			value = t.substring(i + 1);
		} else {
			p = t;
			value = "";
		}

		if (p.charAt(p.length() - 1) == '!') {
			falseify = true;
			name = p.substring(0, p.length() - 1);
		} else {
			falseify = false;
			name = p;
		}

		Constants code = Constants.get(name);

		if (code != null) {

			this.property = code;

			int ae00 = 0x61650000;
			if ((0xFFFF0000 & code.code) == ae00) {
				// TODO we ignore apple codes because mt-daapd doesn't implement them
				// this should change?
				match = new AlwaysMatch(value);
				return;
			}

			Object matchValue = null;

			switch (code.type) {
			case 1:
				matchValue = Byte.parseByte(value);
				break;
			case 5:
				matchValue = Integer.parseInt(value);
				break;
			// using big integer ensures unsigned longs are parsed correctly
			case 7:
				matchValue = new BigInteger(value).longValue();
				break;
			case 9:
				match = new MatchString(value);
				return;
			default:
				throw new IllegalArgumentException("unknown or unimplemented type: " + code.longName + " for " + property);
			}

			match = new MatchValue(matchValue);
		} else {
			throw new RuntimeException("unknown property encountered: " + name);
		}
	}

	public boolean check(HasMetadata t) {

		if (property == null)
			throw new RuntimeException("unknown property: " + name);
		return this.match.matches(t.get(property)) ^ falseify;

	}

	public String toString() {
		return "'" + property.longName + (falseify ? "!" : "") + ":" + match + "'";
	}

	private interface Match {
		public boolean matches(Object lval);
	}

	private class MatchValue implements Match {
		private Object value;

		public MatchValue(Object value) {
			this.value = value;
		}

		public boolean matches(Object lval) {
			return this.value.equals(lval);
		}

		public String toString() {
			return value.toString();
		}
	}

	private class AlwaysMatch implements Match {
		private Object value;

		public AlwaysMatch(Object value) {
			this.value = value;
		}

		public boolean matches(Object lval) {
			return true;
		}

		public String toString() {
			return value.toString();
		}
	}

	private class MatchString implements Match {
		private String value;
		private Pattern pattern;

		public MatchString(String value) {
			this.value = value.replace("*", ".*");
			this.pattern = Pattern.compile(this.value, Pattern.CASE_INSENSITIVE);
		}

		public boolean matches(Object lval) {
			if (lval == null)
				return value == null;
			if (lval instanceof String) {
				return pattern.matcher((String) lval).matches();
			}
			return false;
		}

		public String toString() {
			return value;
		}
	}
}