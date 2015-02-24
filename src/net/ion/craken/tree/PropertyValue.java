package net.ion.craken.tree;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.exception.NodeIOException;
import net.ion.craken.node.exception.NodeNotValidException;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.PropertyValue.ReplaceValue;
import net.ion.craken.tree.PropertyValue.VType;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.parse.gson.JsonSyntaxException;
import net.ion.framework.util.DateUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;

import org.infinispan.io.GridFilesystem;

public class PropertyValue implements Serializable, Comparable<PropertyValue> {

	private final static long serialVersionUID = 4614113174797214253L;
	public final static PropertyValue NotFound = new PropertyValue(Values.newBlank());

	private final Values values;
	private transient GridFilesystem gfs;

	public enum VType implements Serializable {
		BOOL {
			public Class supportedClass() {
				return Boolean.class;
			}

			public Object read(JsonElement ele) {
				return ele.getAsBoolean();
			}
		},
		INT {
			public Class supportedClass() {
				return Integer.class;
			}

			public Object read(JsonElement ele) {
				return ele.getAsInt();
			}
		},
		LONG {
			public Class supportedClass() {
				return Long.class;
			}

			public Object read(JsonElement ele) {
				return ele.getAsLong();
			}
		},
		DOUB {
			public Class supportedClass() {
				return Double.class;
			}

			public Object read(JsonElement ele) {
				return ele.getAsDouble();
			}
		},
		STR {
			public Class supportedClass() {
				return CharSequence.class;
			}

			public Object read(JsonElement ele) {
				return ele.getAsString();
			}
		},
		BLOB {
			public Class supportedClass() {
				return GridBlob.class;
			}

			public Object read(JsonElement ele) {
				return GridBlob.read(ele.getAsString()) ; // JsonObject.fromString(ele.getAsString()).getAsObject(Metadata.class);
			}
		},
		REPLACE {
			public Class supportedClass() {
				return ReplaceValue.class;
			}

			public Object read(JsonElement ele) {
				return ele.getAsString();
			}
		},
		UNKNOWN {
			public Class supportedClass() {
				return Object.class;
			}

			public Object read(JsonElement ele) {
				return ele.getAsString();
			}
		};

		public abstract Class supportedClass();

		public abstract Object read(JsonElement jele);

		public static VType findType(Object value) {
			for (VType vtype : VType.values()) {
				if (vtype.supportedClass().isInstance(value)) {
					return vtype;
				}
			}
			return VType.UNKNOWN;
		}
	}

	private PropertyValue(Values values) {
		this.values = values;
	}

	public interface ReplaceValue<T> {
		public <T> T replaceValue();

		public VType vtype();
	}

	public static PropertyValue createBlank() {
		return new PropertyValue(Values.newBlank());
	}

	public static PropertyValue createPrimitive(Object value) {
		return new PropertyValue(Values.create(value));
	}

	public static PropertyValue loadFrom(TreeNodeKey nodeKey, PropertyId propId, JsonElement pvalue) {
		if (propId.type() == PType.REFER) {
			return PropertyValue.createPrimitive(pvalue.getAsString()) ;
		} else {
			PropertyValue propValue = new PropertyValue(Values.fromJson(pvalue.getAsJsonObject()));
//			if (propValue.isBlob()) {
//				((GridBlob) propValue.value()).path(nodeKey.idString() + "/" + propId.idString());
//			}
			
			return propValue;
		}
	}

	public PropertyValue gfs(GridFilesystem gfs) {
		this.gfs = gfs;
		return this;
	}

	public String toString() {
		return "propertyValue:" + this.values.toString() + "";
	}

	public JsonArray asJsonArray() {
		JsonArray result = new JsonArray();
		for (Object value : values) {
			if (value instanceof GridBlob){
				result.add(((GridBlob)value).toJsonPrimitive()) ;
			} else {
				result.add(JsonPrimitive.create(value));
			}
		}
		return result;
	}

	public JsonObject json() {
		return values.toJson();
	}

	public Object value() {
		Iterator iter = values.iterator();
		return iter.hasNext() ? iter.next() : null;
	}

	public Set asSet() {
		return values.asSet();
	}

	public PropertyValue append(Object... vals) {
		for (Object val : vals) {
			values.append(val);
		}
		return this;
	}

	public PropertyValue remove(Object... vals) {
		for (Object val : vals) {
			values.remove(val);
		}
		return this;
	}

	public String stringValue() {
		return ObjectUtil.toString(value());
	}

	public <T> T defaultValue(T defaultValue) {
		return (T) ObjectUtil.coalesce(value(), defaultValue);
	}

	public int intValue(int dftValue) {
		try {
			return (int) Double.parseDouble(stringValue());
		} catch (NumberFormatException e) {
			return dftValue;
		}
	}

	public long longValue(long dftValue) {
		try {
			return (long) Double.parseDouble(stringValue());
		} catch (NumberFormatException e) {
			return dftValue;
		}
	}

	public GridBlob asBlob() {
		final Object value = value();
		if (value == null)
			throw new NodeIOException("this value not accessable");
		if (gfs == null)
			throw new NodeIOException("this value not accessable[gfs is null]");
		if (this.type() == VType.BLOB) {
			return  ((GridBlob)value).gfs(this.gfs) ; 
		}
		throw new NodeIOException("this value is not blob type : " + value);
	}

	public boolean isBlob() {
		return VType.BLOB == type();
	}

	public int size() {
		return values.size();
	}

	public <T> T value(T replaceValue) {
		final Object value = value();
		if (value == null) {
			return replaceValue;
		}
		return (T) value;
	}

	@Override
	public int compareTo(PropertyValue that) {
		if (this.value() instanceof Comparable && that.value() instanceof Comparable) {
			return ((Comparable) this.value()).compareTo(that.value());
		}
		return 0;
	}

	public int hashCode() {
		return values.hashCode();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof PropertyValue))
			return false;
		PropertyValue that = (PropertyValue) obj;
		return this.values.equals(that.values);
	}

	public VType type() {
		return values.type();
	}

	public int asInt() {
		return intValue(0);
	}

	public String asString() {
		return stringValue();
	}

	public Object asObject() {
		return value();
	}

	public Boolean asBoolean() {
		return Boolean.valueOf(asString());
	}

	public long asLong(int dftValue) {
		return longValue(dftValue);
	}

	public float asFloat(float dftValue) {
		try {
			return (float) Float.parseFloat(stringValue());
		} catch (NumberFormatException e) {
			return dftValue;
		}
	}

	public double asDouble(double dftValue) {
		try {
			return (double) Double.parseDouble(stringValue());
		} catch (NumberFormatException e) {
			return dftValue;
		}
	}

	public String asDateFmt(String fmt) {
		return DateUtil.dateToString(new Date(asLong(0)), fmt);
	}

	public String[] asStrings() {
		List<String> result = ListUtil.newList();
		for (Object o : asSet()) {
			result.add(ObjectUtil.toString(o));
		}
		return result.toArray(new String[0]);
	}

	public boolean isBlank() {
		return StringUtil.isBlank(asString());
	}

}

class Values implements Serializable, Iterable {

	private final Set values;
	private VType selfType = VType.UNKNOWN;

	private Values(Set values) {
		this.values = SetUtil.orderedSet(values);
	}

	static Values newBlank() {
		return new Values(SetUtil.newSet());
	}

	static Values create(Object value) {
		if (value == null) {
			return newBlank();
		} else if (Collection.class.isInstance(value)) {
			Values created = newBlank();
			for (Object val : (Collection) value) {
				created.append(val);
			}
			return created;
		} else if (value.getClass().isArray()) {
			Values created = newBlank();
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				created.append(Array.get(value, i));
			}
			return created;
		} else if (ReplaceValue.class.isInstance(value)) {
			ReplaceValue rvalue = (ReplaceValue) value;
			Values created = create(rvalue.replaceValue());
			created.selfType = rvalue.vtype();
			return created;
		} else if (GridBlob.class.isInstance(value)) {
			Values created = new Values(SetUtil.create(value));
			created.selfType = VType.BLOB;
			return created;
		} else {
			Values created = new Values(SetUtil.create(value));
			created.selfType = VType.findType(value);

			return created;
		}
	}

	static Values fromJson(JsonObject json) {
		Values created = newBlank();
		created.selfType = VType.valueOf(json.asString("vtype"));

		JsonArray jarray = json.asJsonArray("vals");
		for (JsonElement jele : jarray) {
			created.append(created.selfType.read(jele));
		}

		return created;
	}

	JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.addProperty("vtype", type().toString());
		JsonArray jarray = new JsonArray();
		result.add("vals", jarray);
		for (Object value : values) {
			if (value instanceof GridBlob){
				jarray.add(((GridBlob)value).toJsonPrimitive());
			} else {
				jarray.add(JsonPrimitive.create(value));
			}
		}

		return result;
	}

	VType type() {
		return selfType;
	}

	int size() {
		return values.size();
	}

	boolean remove(Object val) {
		return values.remove(val);
	}

	void append(Object val) {
		if (size() == 0) {
			this.selfType = VType.findType(val);
		} else {
			if (this.selfType != VType.findType(val))
				throw new NodeNotValidException("disallow different type in same property vlaue");
		}

		values.add(val);
	}

	Set asSet() {
		return Collections.unmodifiableSet(values);
	}

	@Override
	public Iterator iterator() {
		return values.iterator();
	}

	public int hashCode() {
		return values.hashCode();
	}

	public boolean equals(Values obj) {
		if (!(obj instanceof Values))
			return false;
		Values that = (Values) obj;
		return this.values.equals(that.values);
	}

	public String toString() {
		return values.toString();
	}
}
