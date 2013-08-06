package net.ion.craken.tree;

public class TreeUtil {

	public static String printTree(TreeCache cache, boolean details) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n\n");

		// walk tree
		sb.append("+ ").append(Fqn.SEPARATOR);
		if (details)
			sb.append("  ").append(cache.getRoot().getData());
		sb.append("\n");
		addChildren(cache.getRoot(), 1, sb, details);
		return sb.toString();
	}

	private static void addChildren(TreeNode node, int depth, StringBuilder sb, boolean details) {
		for (TreeNode child : node.getChildren()) {
			for (int i = 0; i < depth; i++)
				sb.append("  "); // indentations
			sb.append("+ ");
			sb.append(child.getFqn().getLastElementAsString()).append(Fqn.SEPARATOR);
			if (details)
				sb.append("  ").append(child.getData());
			sb.append("\n");
			addChildren(child, depth + 1, sb, details);
		}
	}
}
