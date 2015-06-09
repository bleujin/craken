package net.ion.craken.node.crud.tree.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Set;

import net.ion.craken.node.crud.tree.Fqn;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;

/**
 * A class that represents the key to a node
 * 
 * @author Manik Surtani
 * @since 4.0
 */
public class TreeNodeKey implements Serializable {
	private static final long serialVersionUID = 809910293671104885L;
	
	final Fqn fqn;
	final Type contents;
	private Action action = Action.MERGE;

	public static enum Type {
		DATA {
			public String prefix() {
				return "";
			}
		}, STRUCTURE {
			public String prefix() {
				return "@";
			}
		};

		public boolean isStructure() {
			return this == STRUCTURE;
		}
		public boolean isData(){
			return this == DATA ;
		}
		public abstract String prefix()  ;
	}
	
	public static enum Action {
		MERGE, RESET, CREATE
	}


	public TreeNodeKey(Fqn fqn, Type contents) {
		this.contents = contents;
		this.fqn = fqn;
	}

	public Fqn getFqn() {
		return fqn;
	}

	public Type getContents() {
		return contents;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TreeNodeKey key = (TreeNodeKey) o;

		if (contents != key.contents)
			return false;
		if (!Util.safeEquals(fqn, key.fqn))
			return false;

		return true;
	}

	public int hashCode() {
		int h = fqn != null ? fqn.hashCode() : 1;
		h += ~(h << 9);
		h ^= (h >>> 14);
		h += (h << 4);
		h ^= (h >>> 10);
		return h;
	}

	public String toString() {
		return "TreeNodeKey{" + contents + ", fqn=" + fqn + '}';
	}

	public static class Externalizer extends AbstractExternalizer<TreeNodeKey> {
		private static final byte DATA_BYTE = 1;
		private static final byte STRUCTURE_BYTE = 2;

		@Override
		public void writeObject(ObjectOutput output, TreeNodeKey key) throws IOException {
			output.writeObject(key.fqn);
			byte type = 0;
			switch (key.contents) {
			case DATA:
				type = DATA_BYTE;
				break;
			case STRUCTURE:
				type = STRUCTURE_BYTE;
				break;
			}
			output.write(type);
		}

		@Override
		public TreeNodeKey readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			Fqn fqn = (Fqn) input.readObject();
			int typeb = input.readUnsignedByte();
			TreeNodeKey.Type type = null;
			switch (typeb) {
			case DATA_BYTE:
				type = TreeNodeKey.Type.DATA;
				break;
			case STRUCTURE_BYTE:
				type = TreeNodeKey.Type.STRUCTURE;
				break;
			}
			return new TreeNodeKey(fqn, type);
		}

		@Override
		public Set<Class<? extends TreeNodeKey>> getTypeClasses() {
			return Util.<Class<? extends TreeNodeKey>> asSet(TreeNodeKey.class);
		}
	}


	public String idString(){
		return contents.prefix() + fqn.toString() ;
	}
	
	public String fqnString() {
		return fqn.toString();
	}

	public Type getType() {
		return contents;
	}


	public TreeNodeKey resetAction(){
		this.action = Action.RESET ;
		return this ;
	}
	
	public TreeNodeKey createAction(){
		this.action = Action.CREATE ;
		return this ;
	}
	
	public Action action(){
		return action ;
	}
	
	public TreeNodeKey createKey(Action action){
		final TreeNodeKey result = new TreeNodeKey(this.fqn, this.contents);
		result.action = action ;
		return result ;
	}

	public static TreeNodeKey fromString(String idString) {
		if (idString.startsWith(Type.STRUCTURE.prefix())) {
			return Fqn.fromString(idString.substring(1)).struKey() ; // new TreeNodeKey(Fqn.fromString(idString.substring(1)), Type.STRUCTURE) ;
		} else {
			return Fqn.fromString(idString).dataKey() ; //  new TreeNodeKey(Fqn.fromString(idString), Type.DATA) ;
		}
//		return new TreeNodeKey(Fqn.fromString(idString), idString.startsWith("@") ? Type.STRUCTURE : Type.DATA);
	}
}
