package net.ion.craken.db;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.framework.db.Rows;
import net.ion.framework.db.procedure.UserProcedure;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.lucene.index.CorruptIndexException;

public class CrakenFnManager extends CrakenManager {

	private Repository repository;
	private String wname ;

	public CrakenFnManager(Repository repository, String wname) {
		super() ;
		this.repository = repository;
		this.wname = wname ;
	}


	private Map<String, QueryPackage> packages = MapUtil.newMap();

	public int updateWith(CrakenUserProcedureBatch batch) throws Exception{

		Object result = callFunction(batch);

		if (result == null)
			return 0;
		if (result instanceof Integer)
			return ((Integer) result).intValue();
		return -1;
	}

	public int updateWith(CrakenUserProcedure cupt) throws Exception {

		Object result = callFunction(cupt);

		if (result == null)
			return 0;
		if (result instanceof Integer)
			return ((Integer) result).intValue();
		return -1;
	}

	private Object callFunction(UserProcedure cupt) throws SecurityException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, IllegalArgumentException, CorruptIndexException, IOException {
		String packageName = StringUtil.substringBefore(cupt.getProcName(), "@");
		String functionName = StringUtil.substringAfter(cupt.getProcName(), "@");

		QueryPackage targetPackage = packages.get(packageName);
		if (targetPackage == null)
			throw new IllegalArgumentException("not found package : " + packageName);

		final Field field = targetPackage.getClass().getSuperclass().getDeclaredField("session");
		field.setAccessible(true);
		field.set(targetPackage, session());
		// BeanUtils.setProperty(targetPackage, "session", session()) ;

		Method[] mts = targetPackage.getClass().getMethods();

		final Object[] params = cupt.getParams().toArray();
		for (Method method : mts) {
			if (method.isAnnotationPresent(Function.class) && functionName.equalsIgnoreCase(method.getAnnotation(Function.class).value())) {
				final Class<?>[] paramTypes = method.getParameterTypes();
				return MethodUtils.invokeMethod(targetPackage, method.getName(), handlePrimitive(params, paramTypes), paramTypes);
			}
			;
		}

		for (Method method : mts) { // if not found annotation
			if (method.getName().equalsIgnoreCase(functionName)) {
				final Class<?>[] paramTypes = method.getParameterTypes();
				return MethodUtils.invokeMethod(targetPackage, method.getName(), handlePrimitive(params, paramTypes), paramTypes);
			}
		}

		throw new IllegalArgumentException("not found function : " + functionName);
	}

	private Object[] handlePrimitive(Object[] params, Class<?>[] paramTypes) {
		
		List<Object> result = ListUtil.newList() ;
		for (int i = 0 ; i < params.length ; i++) {
			Object param = params[i] ;
			Class paramType = paramTypes[i] ;
			if (paramType.isArray() && paramType.getComponentType().isPrimitive()){
				result.add(getPrimitiveArray(param)) ;
			} else {
				result.add(param) ;
			}
		}
		
		return result.toArray() ;
	}

	private Object getPrimitiveArray(Object val) {
		if (val instanceof Integer[]) return ArrayUtil.toPrimitive((Integer[])val) ;
		if (val instanceof Boolean[]) return ArrayUtil.toPrimitive((Boolean[])val) ;
		if (val instanceof Long[]) return ArrayUtil.toPrimitive((Long[])val) ;
		return (Object[]) val;
	}

	public Rows queryBy(CrakenUserProcedure cupt) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, CorruptIndexException, IOException {
		Object result = callFunction(cupt);
		if (result == null || (!Rows.class.isInstance(result)))
			throw new IllegalStateException("returnType must be rows");
		return (Rows) result;
	}
	
	public ReadSession session() throws IOException {
		return repository.login(wname);
	}

	public CrakenFnManager register(String packageName, QueryPackage queryPackage) {
		packages.put(packageName, queryPackage);
		
		return this;
	}

}

class ConnectionMock implements MethodInterceptor {

	@Override
	public Object intercept(Object proxy, Method method, Object[] args, MethodProxy arg3) throws Throwable {
		return null;
		// throw new IllegalStateException("this is fake object") ;
	}

}