new function(){

	this.createWith = function(v_afieldId, v_afieldNm, v_grpCd, v_afieldExp, v_typeCd, 
	                     v_aFieldLen, v_isMndt, v_indexOption, v_aFieldMaxLen, v_aFieldVLen, v_fileTypeCd, v_examId, v_defaultValue){
		session.tran(function(wsession){
			wsession.pathBy("/afields/" + v_afieldId).property("afieldnm", v_afieldNm) ;
		}) ;
		return 1 ;	
	}


}