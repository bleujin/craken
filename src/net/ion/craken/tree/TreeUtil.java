package net.ion.craken.tree;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;

public class TreeUtil {

	public static String printTree(ReadSession session, boolean details) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n\n");

		// walk tree
		sb.append("+ ").append(Fqn.SEPARATOR);
		if (details)
			sb.append("  ").append(session.root().toMap());
		sb.append("\n");
		addChildren(session.root(), 1, sb, details);
		return sb.toString();
	}

	private static void addChildren(ReadNode node, int depth, StringBuilder sb, boolean details) {
		for (ReadNode child : node.children().toList()) {
			for (int i = 0; i < depth; i++)
				sb.append("  "); // indentations
			sb.append("+ ");
			sb.append(child.fqn().getLastElementAsString()).append(Fqn.SEPARATOR);
			if (details)
				sb.append("  ").append(child.toMap());
			sb.append("\n");
			addChildren(child, depth + 1, sb, details);
		}
	}
}
