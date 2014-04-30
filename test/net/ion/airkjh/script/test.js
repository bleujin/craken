new function() {

	this.iter = function(iterator) {
		var result = {};
		
		while(iterator.hasNext()) {
			var key = iterator.next();
			result[key] = key.toUpperCase();
		}
		
		return result;
	}
	
	this.fn = function(iterator) {
		var result = this.iter(iterator);
		
		return result['a'];
		
	}

}