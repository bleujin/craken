package net.ion.craken.tree;

import static net.ion.craken.tree.TreeNodeKey.Type.DATA;
import static net.ion.craken.tree.TreeNodeKey.Type.STRUCTURE;
import static net.ion.craken.tree.TreeNodeKey.Type.SYSTEM;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Set;

import org.infinispan.marshall.AbstractExternalizer;
import org.infinispan.util.Util;

public class TreeNodeKey implements Serializable {

	private static final long serialVersionUID = 809910293671104885L;
	private final Fqn fqn;
	private final Type contents;
	private Action action = Action.MERGE ;

	public static enum Type {
		DATA {
			public String prefix(){
				return "" ;
			}
		}, STRUCTURE{
			public String prefix(){
				return "@" ;
			}
		}, SYSTEM {
			public String prefix(){
				return "#" ;
			}
		};
		
		public boolean isStructure(){
			return this == STRUCTURE ;
		}
		
		public boolean isData(){
			return this == DATA ;
		}
		
		public boolean isSystem(){
			return this == SYSTEM ;
		}
		
		public abstract String prefix() ;
	}
	
		public TreeNodeKey(Fqn fqn, Type contents) {
		this.contents = contents;
		this.fqn = fqn;
	}
		
	public static enum Action {
		MERGE, RESET, CREATE
	}

	public Fqn getFqn() {
		return fqn;
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
	
	
	


	public String idString(){
		return contents.prefix() + fqn.toString() ;
	}
	
	public String fqnString(){
		return fqn.toString() ;
	}
	
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TreeNodeKey key = (TreeNodeKey) o;

		if (contents != key.contents) return false;
		if (!Util.safeEquals(fqn, key.fqn)) return false;

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
		return "TreeNodeKey{" + "contents=" + contents + ", fqn=" + fqn + '}';
	}

	public static class Externalizer extends AbstractExternalizer<TreeNodeKey> {
		private static final long serialVersionUID = 8630641407515513659L;
		private static final byte DATA_BYTE = 1;
		private static final byte STRUCTURE_BYTE = 2;
		private static final byte SYSTEM_BYTE = 3 ;

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
			case SYSTEM:
				type = SYSTEM_BYTE ;
				break ;
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
				type = DATA;
				break;
			case STRUCTURE_BYTE:
				type = STRUCTURE;
				break;
			case SYSTEM_BYTE :
				type = SYSTEM ;
			}
			return new TreeNodeKey(fqn, type);
		}

		@Override
		public Set<Class<? extends TreeNodeKey>> getTypeClasses() {
			return Util.<Class<? extends TreeNodeKey>> asSet(TreeNodeKey.class);
		}
	}

	public static TreeNodeKey fromString(String idString) {
		if (idString.startsWith(STRUCTURE.prefix())) {
			return new TreeNodeKey(Fqn.fromString(idString.substring(1)), Type.STRUCTURE) ;
		} else if (idString.startsWith(SYSTEM.prefix())) {
			return new TreeNodeKey(Fqn.fromString(idString.substring(1)), Type.SYSTEM) ;
		} else {
			return new TreeNodeKey(Fqn.fromString(idString), Type.DATA) ;
		}
//		return new TreeNodeKey(Fqn.fromString(idString), idString.startsWith("@") ? Type.STRUCTURE : Type.DATA);
	}
}
