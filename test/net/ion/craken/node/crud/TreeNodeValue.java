package net.ion.craken.node.crud;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import net.ion.craken.node.Workspace;
import net.ion.craken.tree.Fqn;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.util.Util;

@SerializeWith(TreeNodeValue.Externalizer.class)
public class TreeNodeValue {

	private transient TreeNode tnode;
	private TreeNodeValue(TreeNode tnode){
		this.tnode = tnode ;
	}
	
	public final static TreeNodeValue create(TreeNode tnode){
		return new TreeNodeValue(tnode) ;
	}
	
	
	public TreeNode toTreeNode(Workspace workspace){
		return TreeNode.create(workspace, tnode.fqn()) ;
	}
	

	public static class Externalizer extends AbstractExternalizer<TreeNodeValue> {
		@Override
		public void writeObject(ObjectOutput output, TreeNodeValue tvalue) throws IOException {
			TreeNode tnode = tvalue.tnode ;
			output.writeUTF(tnode.fqn().toString());
			output.writeUTF(tnode.toValueJson().toString()) ;
			
			
		}

		@Override
		public TreeNodeValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			Fqn fqn = Fqn.fromString(input.readUTF());
			
			
			return null ;
		}

		@Override
		public Set<Class<? extends TreeNodeValue>> getTypeClasses() {
			return Util.<Class<? extends TreeNodeValue>> asSet(TreeNodeValue.class);
		}
		
		
		
	}
}
