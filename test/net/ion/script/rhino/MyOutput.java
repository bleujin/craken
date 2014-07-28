package net.ion.script.rhino;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class MyOutput extends PrintStream {

	private StringBuilder builder = new StringBuilder();

	public MyOutput() throws IOException {
		super(File.createTempFile("out", "osuffix"));
	}

	public void write(byte b[]) {
		String s = new String(b);
		append(s.trim(), false);
	}

	public String readOut() {
		String result = builder.toString();
		builder = new StringBuilder();
		return result;
	}

	public void write(byte b[], int off, int len) {
		String s = new String(b, off, len);
		append(s.trim(), false);
	}

	public void write(int b) {
		Integer i = new Integer(b);
		append(i.toString(), false);
	}

	public void println(String s) {
		append(s, true);
	}

	public void print(String s) {
		append(s, false);
	}

	public void print(Object obj) {
		if (obj != null)
			append(obj.toString(), false);
		else
			append("null", false);
	}

	public void println(Object obj) {
		if (obj != null)
			append(obj.toString(), true);
		else
			append("null", true);
	}

	private synchronized void append(String x, boolean newline) {
		builder.append(x);
		if (newline)
			builder.append("\r\n");
	}

}
