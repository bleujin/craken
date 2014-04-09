new function(){

	this.createWith = function(afieldId, afieldNm) {
		session.tranSync(function(wsession){
			wsession.pathBy('/afields/' + afieldId).property('afieldId', afieldId).property('afieldNm', afieldNm) ;
		}) ;
		return 1 ;
	}, 
	
	this.listBy = function(skip, offset) {
		return session.pathBy('/afields').children().skip(skip).offset(offset).toAdRows('afieldId, afieldNm') ;
	}, 
	
	this.batchWith = function(ids, names){
		session.tranSync(function(wsession){
			for(var i in ids){
				wsession.pathBy('/afields/' + ids[i]).property('afieldId', ids[i]).property('afieldNm', names[i]) ;
			}
		}) ;
		
		return ids.length ;
	}, 
	
	this.jsonBy = function(path, props) {
		var found = session.pathBy(path) ;
		return jbuilder.newInner().property(found, props).property("extra", "extravalue").buildRows("afieldId, afieldNm, extra") ; 
	}
} ; 

