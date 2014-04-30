package net.ion.craken.node.crud;

import net.ion.craken.node.ReadSession;

public class WalkReadNode extends ReadNodeImpl {

	private static final long serialVersionUID = -4810786417922545471L;
	private int level;

	private WalkReadNode(ReadSession session, TreeNode tnode, int level) {
		super(session, tnode);
		this.level = level;
	}

	static WalkReadNode create(ReadSession session, TreeNode tnode, int level) {
		return new WalkReadNode(session, tnode, level);
	}

	public int level() {
		return level;
	}

}
