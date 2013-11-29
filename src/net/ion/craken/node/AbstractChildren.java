package net.ion.craken.node;

import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.convert.Predicates;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.ListUtil;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public abstract class AbstractChildren<T extends NodeCommon<T>, C extends AbstractChildren> extends IteratorList<T>{


	public abstract C filter(Predicate<T> equalValue)  ;
	
	public C all(String propId, Object value) {
		return filter(Predicates.<T>allValue(propId, value));
	}

	public C gt(String propId, Object value) {
		return filter(Predicates.<T>gtValue(propId, value));
	}


	public C gte(String propId, Object value) {
		return filter(Predicates.<T>gteValue(propId, value));
	}


	public C lt(String propId, Object value) {
		return filter(Predicates.<T>ltValue(propId, value));
	}


	public C lte(String propId, Object value) {
		return filter(Predicates.<T>lteValue(propId, value));
	}


	public C ne(String propId, Object value) {
		return filter(Predicates.<T>neValue(propId, value));
	}


	public C nin(String propId, Object value) {
		return filter(Predicates.<T>ninValue(propId, value));
	}

	public C eq(String propId, Object value) {
		return filter(Predicates.<T>equalValue(propId, value));
	}

	public C contains(String propId, String value) {
		return filter(Predicates.<T>containsValue(propId, value));
	}

	public C hasRef(String refName, Fqn target) {
		return filter(Predicates.<T>hasRelation(refName, target)) ;
	}

	public C in(String propId, Object value) {
		return filter(Predicates.<T>inValue(propId, value));
	}


	
	
	
	// Element
	public C exists(String propId) {
		return filter(Predicates.<T>exists(propId));
	}

	public C type(String propId, Class clz) {
		return filter(Predicates.<T>type(propId, clz));
	}

	public C size(String propId, int size){
		return filter(Predicates.<T>size(propId, size));
	}

	public C where(String expression) {
		return filter(Predicates.<T>where(expression)) ;
	}

	
	

	
	// Logical

	public C and(Predicate<T>... components) {
		return filter(Predicates.<T>and(components));
	}
	public C or(Predicate<T>... components) {
		return filter(Predicates.<T>or(components));
	}
	public C nor(Predicate<T> left, Predicate<T> right) {
		return filter(Predicates.<T>nor(left, right));
	}
	public C not(Predicate<T> components) {
		return filter(Predicates.<T>not(components));
	}

	

	

	public Iterator<T> iterator(){
		return toList().iterator() ;
	}
	
	public List<T> toList(){
		List<T> result = ListUtil.newList() ;
		while(hasNext()){
			result.add(next()) ;
		}
		return result ;
	}
	
	public <F> F transform(Function<Iterator<T>, F> fn){
		return fn.apply(iterator()) ;
	}

	public void debugPrint() {
		while(hasNext()){
			T node = next();
			node.session().credential().tracer().println(node) ;
		}
	}
	
}






