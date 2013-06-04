package net.ion.craken.node.convert.rows;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.function.NvlFunction;
import net.ion.craken.node.convert.rows.function.SingleColumn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;

public class ColumnParser {

	public ColumnParser(){
		
	}
	private static List<String> FunctionName = ListUtil.toList("nvl", "tochar", "decode", "substr", "sign", "length", "power", "divide", "floor", "mod", "tonumber", "append", "lpad", "minus", "min", "max");

	public NodeColumns parse(String... columns) {
		List<IColumn> result = ListUtil.newList() ;
		for (int i = 0; i < columns.length; i++) {
			result.add(this.parse(columns[i]));
		}
		return NodeColumns.create(result.toArray(new IColumn[0]));
	}
	
	
	public IColumn nvl(String... cols) {
		List<String> list = ListUtil.toList(cols);
		return new NvlFunction(this, (String[]) (list.subList(0, list.size() - 1).toArray(new String[0])), cols[cols.length - 1]);
	}

	public IColumn constant(Object con, String label) {
		return new ConstantColumn(con, label);
	}

	private String[] getParams(String expression) {
		List<String> list = ListUtil.newList();
		String[] params = StringUtil.split(expression, ",");

		String value = "";
		for (int i = 0; i < params.length; i++) {
			if (StringUtil.isEmpty(value))
				value = params[i];
			else {
				value = value + "," + params[i];
			}
			if (isIncludeCountMatches(value, "(", ")")) {
				list.add(value);
				value = "";
			}
		}
		return list.toArray(new String[0]);
	}

	private boolean isIncludeCountMatches(String value, String startKey, String endKey) {
		return StringUtil.countMatches(value, startKey) == StringUtil.countMatches(value, endKey);
	}

	public IColumn parse(String expression) {
		// decode(a, b) c
		// decode(r.a, r.b) c
		// decode(r.a, 'constant') c
		// String expression = _expression.toLowerCase();
		try {
			if (isFunctionExpression(expression)) {
				String fnName = StringUtil.lowerCase(StringUtil.substringBefore(expression, "("));
				String paramString = StringUtil.substringAfter(expression, "(");

				paramString = StringUtil.substringBeforeLast(paramString, ")");
				String[] params = getParams(paramString);

				String alias = StringUtil.defaultIfEmpty(StringUtil.substringAfterLast(expression, ")").trim(), expression);
				
				Class clz = Class.forName("net.ion.craken.node.convert.rows.function." + StringUtil.capitalize(fnName) + "Function");
				Object[] passed = {this, params, alias };
				IColumn col = (IColumn) clz.getConstructor(ColumnParser.class, String[].class, String.class).newInstance(passed);
				return col;
			} else if (isConstantExpression(expression)) {
				String[] cols = StringUtil.split(expression, " "); // @TODO : not sufficiency
				Object value = NumberUtil.isNumber(cols[0]) ? Integer.parseInt(cols[0]) : StringUtil.substringBetween(cols[0], "'", "'");
				String alias = (cols.length == 1 ? ObjectUtil.toString(value) : cols[1]);

				return new ConstantColumn(value, alias);
			} else {
				String[] exps = StringUtil.split(expression, " ");
				if (exps.length == 1) {
					return new PropertyColumn(exps[0], "");
				} else if (exps.length == 2) {
					return new PropertyColumn(exps[0], exps[1]);
					// } else if (exps.length == 2 && expression.contains(".")) { // a.b
					// return new ReferenceColumn(expression, exps[1]);
					// } else if (exps.length == 2 && expression.contains(" ")) { // a b
					// return new NormalColumn(exps[0], exps[1]);
					// } else if (exps.length == 3 && expression.contains(".")) { // a.b c
					// return new ReferenceColumn(exps[0] + "." + exps[1], exps[2]);
				} else {
					throw new IllegalArgumentException(expression + " is illegal expression");
				}
			}
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private boolean isConstantExpression(String expression) {
		return expression.startsWith("'") || NumberUtil.isNumber(expression);
	}

	private boolean isFunctionExpression(String expression) {
		String fnName = StringUtil.lowerCase(StringUtil.substringBefore(expression, "("));
		return FunctionName.contains(fnName);
	}
}

class PropertyColumn extends SingleColumn {

	private String targetColumn;
	private String label;

	PropertyColumn(String targetColumn, String label) {
		this.targetColumn = targetColumn ;
		this.label = label ;
	}

	public String getLabel() {
		if (StringUtil.isBlank(label)){
			String[] names = StringUtil.split(targetColumn, "/@") ;
			return names[names.length-1] ;
		}
		
		return label ;
	}

	@Override
	public String toString() {
		return label + " ( " + targetColumn + " ) ";
	}

	public Object getValue(ReadNode node) {
		
		StringBuilder prefix = new StringBuilder() ;
		for(char c : targetColumn.toCharArray()){
			if (c == '/'){
				final String afterName = StringUtil.substringAfter(targetColumn, prefix.toString() + "/");
				if ("..".equals(prefix.toString())){
					return new PropertyColumn(afterName,  afterName).getValue(node.parent()) ;
				} else if (node.hasChild(prefix.toString())){
					return new PropertyColumn(afterName,  afterName).getValue(node.child(prefix.toString())) ;
				}
			} else if (c == '@' && node.hasRef(prefix.toString())) {
				final String afterName = StringUtil.substringAfter(targetColumn, prefix.toString() + "@");
				return new PropertyColumn(afterName,  afterName).getValue(node.ref(prefix.toString())) ;
			}
			prefix.append(c) ;
		}

		return node.property(targetColumn).value();
	}

}