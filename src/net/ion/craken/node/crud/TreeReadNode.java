package net.ion.craken.node.crud;

import net.ion.craken.node.ReadSession;

public class TreeReadNode extends ReadNodeImpl {

	private static final long serialVersionUID = -4810786417922545471L;
	private int level;

	private TreeReadNode(ReadSession session, TreeNode tnode, int level) {
		super(session, tnode);
		this.level = level;
	}

	static TreeReadNode create(ReadSession session, TreeNode tnode, int level) {
		return new TreeReadNode(session, tnode, level);
	}

	public int level() {
		return level;
	}

}
