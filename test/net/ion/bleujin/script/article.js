new function(){

	this.listBy = function() {
		return session.ghostBy('/articles').children().toAdRows('artId, artSubject') ;
	} 
	
} ;

