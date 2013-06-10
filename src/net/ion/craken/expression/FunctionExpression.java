package net.ion.craken.expression;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.NumberUtil;

import com.google.common.collect.Iterables;

public final class FunctionExpression extends ValueObject implements Expression {
	public final QualifiedName function;
	public final List<Expression> args;

	public FunctionExpression(QualifiedName function, List<Expression> args) {
		this.function = function;
		this.args = Collections.unmodifiableList(args);
	}

	public static FunctionExpression of(QualifiedName function, Expression... args) {
		return new FunctionExpression(function, Arrays.asList(args));
	}

	@Override
	public Comparable value(NodeCommon node) {
		if (args.size() < 1)
			return null; // sysdate ?
		if (function.names.size() > 1)
			return null; // static

		List<Comparable> margs = ListUtil.newList();
		for (Expression arg : args) {
			margs.add(arg.value(node));
		}

		String fnName = function.names.get(0);
		Comparable target = margs.get(0);

		try {

			if (margs.size() == 1)
				return (Comparable) target.getClass().getMethod(fnName).invoke(target);

			Iterator argIter = Iterables.skip(margs, 1).iterator();
			Class[] argClz = new Class[margs.size() - 1];
			Object[] argObj = new Object[margs.size() - 1];
			int i = 0;
			while (argIter.hasNext()) {
				Object aobj = argIter.next();
				argClz[i] = (aobj.getClass().equals(BigDecimal.class)) ? int.class : aobj.getClass();
				argObj[i++] = (aobj instanceof BigDecimal) ? ((BigDecimal)aobj).intValue() : aobj;
			}
			
			final Object result = target.getClass().getMethod(fnName, argClz).invoke(target, argObj);
			
			
			if ((result instanceof Integer) || result instanceof Long || result instanceof Float || result instanceof Double) return NumberUtil.createBigDecimal(result.toString()) ;
			return (Comparable) result;
		} catch (Throwable ex) {
			throw new IllegalArgumentException(ex);
		}

	}
}