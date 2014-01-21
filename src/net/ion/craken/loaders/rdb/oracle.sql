
--SEP 
drop table craken_tblc ; 

--SEP
drop table craken_tblt ;

--SEP
CREATE TABLE craken_tblc
(
    fqn  VARCHAR2(400),
    props CLOB, 
    parentFqn varchar2(400)
) ;

--SEP
create unique index craken_pk on craken_tblc(parentFqn, fqn) ;
--SEP
create unique index craken_parent_idx on craken_tblc(fqn, parentFqn) ;


--SEP
create global temporary table craken_tblt
(seqno number, fqn varchar2(400), action varchar2(15), props clob, parentFqn varchar2(400)) on commit delete rows ;






--SEP
CREATE OR REPLACE PACKAGE TYPES
as type cursorType
is ref cursor ;
	FUNCTION  dummy return number ;
END ;


--SEP
CREATE OR REPLACE PACKAGE BODY TYPES
as FUNCTION dummy return Number
is
	BEGIN
		return 1 ;
	End dummy ;
END ;


--SEP
CREATE OR REPLACE PACKAGE Craken
is 
	FUNCTION  storeWith return Number; 
    FUNCTION  nodeBy(v_fqn IN varchar2) return Types.cursorType ;
    FUNCTION  childrenBy(v_parentFqn IN varchar2) return Types.cursorType ;
    FUNCTION  nodeAllBy(v_limit IN number) return Types.cursorType ;
    FUNCTION  nodeAllFqnBy return Types.cursorType ;
 
END ;


--SEP
CREATE OR REPLACE PACKAGE BODY Craken
is 
	FUNCTION  storeWith
	return Number 
	is 
	BEGIN 

        merge into craken_tblc c
        using (select fqn, props, parentFqn from craken_tblt  where action = 'MODIFY') t
        on (t.fqn = c.fqn)
        when matched then
            update set c.props = t.props
        when not matched then
            insert (c.fqn, c.props, c.parentFqn) values(t.fqn, t.props, t.parentFqn) ;
            
        
        delete from craken_tblc where fqn in (select fqn from craken_tblt where action = 'REMOVE') ;
        
        delete from craken_tblc where parentFqn in (select fqn from craken_tblt where action = 'REMOVECHILDREN')
                and fqn not in 
                    (select t2.fqn from craken_tblt t1, craken_tblt t2 
                     where t1.action = 'REMOVECHILDREN' and t1.fqn = t2.parentFqn and t1.seqNo < t2.seqNo) ;
        return 1; 
	END ; 
    
    Function nodeBy(v_fqn IN varchar2)
	return types.cursorType is rtn_cursor types.cursorType ;
	BEGIN
		Open rtn_cursor For
		Select * From craken_tblc where fqn = v_fqn ;
        
		return rtn_cursor ;
	END  ;
    

    Function childrenBy(v_parentFqn IN varchar2)
	return types.cursorType is rtn_cursor types.cursorType ;
	BEGIN
		Open rtn_cursor For
		Select fqn From craken_tblc where parentFqn = v_parentFqn ;
        
		return rtn_cursor ;
	END  ;
    

    FUNCTION  nodeAllBy(v_limit IN number) 
	return types.cursorType is rtn_cursor types.cursorType ;
	BEGIN
		Open rtn_cursor For
		Select fqn, props From craken_tblc where rownum <= v_limit ;
        
		return rtn_cursor ;
	END   ;

    FUNCTION  nodeAllFqnBy
	return types.cursorType is rtn_cursor types.cursorType ;
	BEGIN
		Open rtn_cursor For
		Select fqn, props From craken_tblc where rownum <= 1000;
        
		return rtn_cursor ;
	END   ;
    
END  ;