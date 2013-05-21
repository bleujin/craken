package net.ion.craken.db;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang.ClassUtils;

import com.google.common.base.CharMatcher;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.convert.bean.ProxyBean;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.manager.DBManager;
import net.ion.framework.db.procedure.RepositoryService;
import net.ion.framework.db.procedure.UserProcedure;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class CrakenManager extends DBManager {

	private CrakenRepositoryService cservice;
	private RepositoryImpl repository;
	private Connection fake;

	public CrakenManager(RepositoryImpl repository) {
		this.repository = repository;
		this.cservice = new CrakenRepositoryService(this);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return this.fake;
	}

	@Override
	public int getDBManagerType() {
		return 77;
	}

	@Override
	public String getDBType() {
		return "craken";
	}

	public ReadSession session() {
		return repository.testLogin("test");
		// return repository.testLogin("test") ;
	}

	@Override
	public RepositoryService getRepositoryService() {
		return cservice;
	}

	@Override
	protected void myDestroyPool() throws Exception {

	}

	protected void heartbeatQuery(IDBController dc) throws SQLException {
		// no action
	}

	@Override
	protected void myInitPool() throws SQLException {
		Enhancer e = new Enhancer();
		e.setSuperclass(Connection.class);
		e.setCallback(new ConnectionMock());

		this.fake = (Connection) e.create();
	}

	private Map<String, QueryPackage> packages = MapUtil.newMap();

	public CrakenManager register(String packageName, QueryPackage queryPackage) {
		packages.put(packageName, queryPackage);

		return this;
	}

	public int updateWith(CrakenUserProcedureBatch batch) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException, ExecutionException {

		Object result = callFunction(batch);

		if (result == null)
			return 0;
		if (result instanceof Integer)
			return ((Integer) result).intValue();
		return -1;
	}

	public int updateWith(CrakenUserProcedure cupt) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {

		Object result = callFunction(cupt);

		if (result == null)
			return 0;
		if (result instanceof Integer)
			return ((Integer) result).intValue();
		return -1;
	}

	private Object callFunction(UserProcedure cupt) throws SecurityException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
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
			if (method.getName().equals(functionName)) {
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

	public Rows queryBy(CrakenUserProcedure cupt) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		Object result = callFunction(cupt);
		if (result == null || (!Rows.class.isInstance(result)))
			throw new IllegalStateException("returnType must be rows");
		return (Rows) result;
	}

}

class ConnectionMock implements MethodInterceptor {

	@Override
	public Object intercept(Object proxy, Method method, Object[] args, MethodProxy arg3) throws Throwable {
		return null;
		// throw new IllegalStateException("this is fake object") ;
	}

}