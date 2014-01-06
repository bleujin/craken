package net.ion.bleujin;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Basically the same answer as noah's.
 */
public class TestGeneric<E> {

	@SuppressWarnings("unchecked")
	public Class<E> getTypeParameterClass() {
		Type type = getClass().getGenericSuperclass();
		ParameterizedType paramType = (ParameterizedType) type;
		return (Class<E>) paramType.getActualTypeArguments()[0];
	}

	private static class StringHome extends TestGeneric<String> {
	}

	private static class StringBuilderHome extends TestGeneric<StringBuilder> {
	}

	private static class StringBufferHome extends TestGeneric<StringBuffer> {
	}

	/**
	 * This prints "String", "StringBuilder" and "StringBuffer"
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException {
		String object0 = new StringHome().getTypeParameterClass().newInstance();
		StringBuilder object1 = new StringBuilderHome().getTypeParameterClass().newInstance();
		StringBuffer object2 = new StringBufferHome().getTypeParameterClass().newInstance();
		System.out.println(object0.getClass().getSimpleName());
		System.out.println(object1.getClass().getSimpleName());
		System.out.println(object2.getClass().getSimpleName());
	}

}
