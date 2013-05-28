package net.ion.craken.expression;

import junit.framework.TestCase;

public class TestValueObject extends TestCase {

	
	public void testEqual() throws Exception {
		assertEquals(new Animal("m", 20), new Animal("m", 20)) ;
		assertEquals(new Animal("m", 20).hashCode(), new Animal("m", 20).hashCode()) ;
		
		assertEquals(true, new Mammal("m", 20).equals(new Mammal("m", 20))) ;
		assertEquals(false, new Animal("m", 20).equals(new Mammal("m", 20))) ;
		
	}
	
	static class Animal extends ValueObject {
		final String sex;
		final int age;

		Animal(String sex, int age) {
			this.sex = sex;
			this.age = age;
		}
	}

	static class Mammal extends Animal {
		Mammal(String sex, int age) {
			super(sex, age);
		}
	}

	static class Dog extends Mammal {
		final String name;

		public Dog(String sex, int age, final String name) {
			super(sex, age);
			this.name = name;
		}
	}

	static class Person extends ValueObject {
		final String name;

		Person(String name) {
			this.name = name;
		}
	}

}
