new function () {

    var prefix = "/afields/";
    
    var nodeName = function(path) {
    	var names = path.split('/');

    	if(names.length === 0) return path;
    	else names[names.length - 1];
    }

    this.createWith = function (afieldId, afieldNm, grpCd, afieldExp, typeCd, aFieldLen, isMndt, indexOption, aFieldMaxLen, aFieldVLen, fileTypeCd, examId, defaultValue) {
        session.tran(function (wsession) {
            wsession.pathBy(prefix + afieldId)
                .property("afieldid", afieldId)
                .property("afieldnm", afieldNm)
                .property("grpcd", grpCd)
                .property("afieldexp", afieldExp)
                .property("typecd", typeCd)
                .property("afieldlen", aFieldLen)
                .property("ismndt", func.nvl(isMndt, 'F'))
                .property("indexoption", indexOption)
                .property("afieldmaxlen", aFieldMaxLen)
                .property("afieldvlen", aFieldVLen)
                .property("filetypecd", fileTypeCd)
                .property("examid", examId)
                .property("defaultvalue", defaultValue);
        });

        return 1;
    }

    this.updateWith = function (afieldId, afieldNm, grpCd, afieldExp, typeCd, aFieldLen, isMndt, indexOption, aFieldMaxLen, aFieldVLen, fileTypeCd, examId, defaultValue) {

        session.tran(function (wsession) {
            wsession.pathBy(prefix + afieldId)
                .property("afieldid", afieldId)
                .property("afieldnm", afieldNm)
                .property("grpcd", grpCd)
                .property("afieldexp", afieldExp)
                .property("typecd", typeCd)
                .property("afieldlen", aFieldLen)
                .property("ismndt", func.nvl(isMndt, 'F'))
                .property("indexption", indexOption)
                .property("afieldmaxlen", aFieldMaxLen)
                .property("afieldvlen", aFieldVLen)
                .property("filetypecd", fileTypeCd)
                .property("examid", examId)
                .property("defaultvalue", defaultValue);
        });

        return 1;
    }

    this.delLowerAfieldWith = function(upperId) {
		var iterator = session.pathBy("/afield_rels").childQuery("").eq("upperid", upperId).find().iterator();
		var cnt = 0;

		session.tranSync(function(wsession) {
			while(iterator.hasNext()) {
				var node = iterator.next();
				wsession.pathBy(node.fqn()).removeSelf();
				cnt++;
			}
		});
		
		return cnt;
    }

    this.addLowerAFieldWith = function(upperId, lowerId, orderNo) {
		return session.tranSync(function(wsession) {
			var path = "/afield_rels/" + ("ROOT" ===  upperId ? "" : upperId + "/") + lowerId;
			wsession.pathBy(path).property("upperid", upperId).property("lowerid", lowerId).property("orderno", orderNo);
			return 1;
		});
    }

    this.selLowerAfieldBy = function(upperId) {
		var iterator = session.pathBy("/afield_rels").childQuery("", true).eq("upperid", upperId).ascending("orderno").find().iterator();
		var builder = jbuilder.newEmptyInlist();
		
		while(iterator.hasNext()) {
			var node = iterator.next();
			
			var afieldId = node.property("lowerid").asString();
			var afieldNm = '';
			
			try {
				afieldNm = session.pathBy("/afields/" + afieldId).property("afieldnm").asString();
			} catch(exception) {}
			
			builder.next().property("lowerId", afieldId).property("afieldNm", afieldNm);
		}
		
		return builder.buildRows();
    }

    this.retrieveBy = function (afieldId) {

        var node = session.pathBy(prefix + afieldId);
        return jbuilder.newInner()
            .property(node, 'afieldid, afieldnm, grpcd, regdate, afieldexp, typecd, afieldlen, afieldvlen, afieldmaxlen, filetypecd, examid, defaultvalue, ismndt')
            .property("filetypecd", node.property("filetypecd").asString().toLowerCase())
            .property("afieldvlen", func.asInt(node.property("afieldvlen")))
            .property("afieldlen", func.asInt(node.property("afieldlen")))
            .property("afieldmaxlen", func.asInt(node.property("afieldmaxlen")))
            .property("examid", func.asInt(node.property("examid")))
            .buildRows("afieldid, afieldnm, grpcd, regdate, afieldexp, typecd, afieldlen, afieldvlen, afieldmaxlen, filetypecd, examid, defaultvalue, ismndt");
    }

    this.removeWith = function (afieldId) {

        var exist = false,
            category_afield_tblc = null,
            afield_rel_row = null,
            rowCount = 0;


        try {
            var category_afield_tblc = session.pathBy('/category_afields/' + afieldId);
        } catch (e) {

        }

        if (category_afield_tblc === null) {
            var afield_rel_row = session.pathBy("/afield_rels").childQuery("").eq("lowerid", afieldId).ne("upperid", "ROOT").findOne();
            exist = (afield_rel_row !== null);
        } else {
            exist = true;
        }

        if (!exist) {
            session.tran(function (wsession) {
                wsession.pathBy(prefix + afieldId).removeSelf();
                rowCount++;

                var iterator = wsession.pathBy('/afield_rels').childQuery("", true).where("upperid == '" + afieldId + "' || lowerid == '" + afieldId + "'").find().iterator();

                while (iterator.hasNext()) {
                    wsession.pathBy(iterator.next().fqn()).removeSelf();
                    rowCount++;
                }
            });
        }

        return rowCount;
    }

    this.unloadWith = function (catId, srcCatId) {
        if (catId !== srcCatId) {
            var iterator = session.pathBy("/category_afields").childQuery("", true).eq("catid", catId).find().iterator();

            session.tranSync(function (wsession) {
                while (iterator.hasNext()) {
                    var node = iterator.next();
                    wsession.pathBy(node.fqn()).removeSelf();
                }
            });
        }
    }

    this.loadWith = function (catId, srcCatId) {
        var rowCount = 0;
        var children = session.pathBy("/category_afields/" + srcCatId + "/rels").children();

        session.tranSync(function (wsession) {
                var iterator = children.iterator();
                while (iterator.hasNext()) {
                    var node = iterator.next();
                    wsession.pathBy("/category_afields/" + catId + "/rels/" + node.property("afieldid").asString())
                        .property("afieldid", node.property("afieldid").asString())
                        .property("catid", catId)
                        .property("orderlnno", func.asInt(node.property("orderlnno")));

                    rowCount++;
                }
            }
        );
        return rowCount;
    }

    this.listBy = function(grpCd, typeCd, listNum, pageNo) {

        var afieldGroups = session.pathBy("/codes/afield_grp").childQuery("").eq("codeid", grpCd).find().iterator();
        var afieldGrpMap = {};

        while(afieldGroups.hasNext()) {
            var afieldGroup = afieldGroups.next();
            var codeId = afieldGroup.property("codeid").asString();

            afieldGrpMap[codeId] = afieldGroup;
        }

        var afieldTypeQueryRequest = session.pathBy("/codes/afield").childQuery("");

        if(typeCd && typeCd !== '') {
            afieldTypeQueryRequest.wildcard("codeid", typeCd + "*");
        }

        var afieldTypes = afieldTypeQueryRequest.find().iterator();
        var afieldTypeMap = {};

        while(afieldTypes.hasNext()) {
            var afieldType = afieldTypes.next();
            var codeId = afieldType.property("codeid").asString();

            afieldTypeMap[codeId] = afieldType;
        }

        var skip = (pageNo - 1) * listNum;

        var queryRequest = session.pathBy("/afields").childQuery("").offset(listNum).skip(skip).ascending("grpcd").ascending("typecd");

        if(typeCd && typeCd !== '') {
            queryRequest.eq("typecd", typeCd);
        }

        if(grpCd && grpCd !== '') {
            queryRequest.eq("grpcd", grpCd);
        }

        var afields = queryRequest.find().iterator();
        var builder = jbuilder.newInlist();

        while(afields.hasNext()) {
            var afield = afields.next();

            var afieldId = afield.property("afieldid").asString();
            var typeCd = afield.property("typecd").asString();

            var isUsedCategory = "F";
            var isUsedAfield = "F";

            try {
                session.pathBy("/category_afields").childQuery("").eq("afieldid", afieldId).findOne();
                isUsedCategory = 'T';
            } catch(exception) {}

            try {
                session.pathBy("/afield_rels").childQuery("").eq("lowerid", afieldId).ne("upperid", "ROOT").findOne();
                isUsedAfield = 'T';
            } catch(exception) {}

            builder.property("afieldId", afieldId)
                .property("afieldNm", afield.property("afieldnm").asString())
                .property("typeCd", typeCd)
                .property("afieldExp", func.nvl(afield.property("afieldexp").asString(), ""))
                .property("grpCd", afield.property("grpcd").asString())
                .property("grpNm", afieldTypeMap[typeCd].property("codenm").asString())
                .property("isUsedCategory", isUsedCategory)
                .property("isUsedAfield", isUsedAfield);

            if(afields.hasNext()) {
                builder.next();
            }
        }

        return builder.buildRows();
    }

    // TODO
    this.ableLowerListBy = function(selfId, groupCd, typeCd, searchKey, listNum, pageNo) {
    	
    }

    // TODO
    this.lowerlistBy = function(afieldId) {
    	
    }
    
    var codeFunc = function(iterator) {
    	var codeMap = {};
    	
    	while(iterator.hasNext()) {
    		var node = iterator.next();
    		var codeId = node.property('codeid').asString();
    		var codeName = node.property('codenm').asString();
    		
    		codeMap[codeId] = codeName;
    	}
    	
    	return codeMap;
    }
    
    this.searchListBy = function(grpCd, typeCd, searchKey, listNum, pageNo) {
    	var javaFilters = net.ion.craken.node.crud.Filters;
    	
    	var afieldGrpMap = session.pathBy("/codes/afield_grp").children().transform(codeFunc);
		var afieldTypeMap = session.pathBy("/codes/afield").children().transform(codeFunc);
		var skip = (pageNo - 1) * listNum;
		
		var filters = [];

		if (typeCd && typeCd !== '') {
			filters.push(net.ion.craken.node.crud.Filters.eq("typecd", typeCd));
		}

		if (grpCd && grpCd !== '') {
			filters.push(net.ion.craken.node.crud.Filters.eq("grpcd", grpCd));
		}
		
		var searchFilter = net.ion.ics6.filter.OrSearchFilter.wildcard(searchKey, ['afieldid', 'afieldnm', 'afieldexp']);
		filters.push(searchFilter);
		
		var result = session.pathBy("/afields").childQuery("")
			.filter(net.ion.craken.node.crud.Filters.and(filters))
			.offset(listNum)
			.skip(skip)
			.ascending("grpcd")
			.ascending("typecd")
			.descending("regdate")
			.find();		
		
    	var builder = jbuilder.newInlist();
		var afields = result.iterator();

		while (afields.hasNext()) {
			var afield = afields.next();

			var afieldId = afield.property("afieldid").asString();
			var typeCd = afield.property("typecd").asString();

			var isUsedCategory = "F";
			var isUsedAfield = "F";

			var catAFieldRow = session.pathBy("/category_afields").childQuery("").eq("afieldid", afieldId).findOne();
			var afieldRels = session.pathBy("/afield_rels").childQuery("").eq("lowerid", afieldId).ne("upperid", "ROOT").findOne();

			isUsedCategory = (catAFieldRow == null ? "F" : "T");
			isUsedAfield = (afieldRels == null ? "F" : "T");

			builder.property("afieldId", afieldId)
				.property("afieldNm", afield.property("afieldnm").asString())
				.property("typeCd", typeCd)
				.property("afieldExp", func.nvl(afield.property("afieldexp").asString(), ""))
				.property("grpCd", afield.property("grpcd").asString())
				.property("grpNm", afieldTypeMap[typeCd])
				.property("isUsedCategory", isUsedCategory)
				.property("isUsedAfield", isUsedAfield);

			if (afields.hasNext()) {
				builder.next();
			}
		}
    	
    	return builder.buildRows();
    }
    
    this.allGroupAndTypeBy = function() {
		var afieldGroups = session.pathBy("/codes/afield_grp").children().iterator();
		var afieldTypes = session.pathBy("/codes/afield").children().iterator();
		
		var builder = jbuilder.newEmptyInlist();
		
		while(afieldGroups.hasNext()) {
			var afieldGrp = afieldGroups.next();
			var codeId = afieldGrp.property("codeid").asString();
			var cdnm = afieldGrp.property("cdnm").asString();
			
			builder.next().property("upperCdId", "afield_grp")
				.property("codeId", codeId)
				.property("groupNm", cdnm)
				.property("typeId", codeId)
				.property("typeNm", cdnm);
		}
		
		while(afieldTypes.hasNext()) {
			var afieldType = afieldTypes.next();
			var codeId = afieldType.property("codeid").asString();
			var cdnm = afieldType.property("cdnm").asString();
			
			builder.next().property("upperCdId", "afield")
				.property("codeId", codeId)
				.property("groupNm", cdnm)
				.property("typeId", codeId)
				.property("typeNm", cdnm);				
		}		
		
		return builder.buildRows();
    }
    
    this.allGroupBy = function() {
		var afieldGroups = session.pathBy("/codes/afield_grp").children().ascending('codeid').iterator();
		var builder = jbuilder.newEmptyInlist();
		
		while(afieldGroups.hasNext()) {
			var afieldGroup = afieldGroups.next();
			var codeId = afieldGroup.property("codeid").asString();
			var codeNm = afieldGroup.property("cdnm").asString();
			
			var usedAfield = session.pathBy("/afields").childQuery("").eq("grpcd", codeId).findOne();
			var isUsedAfield = (usedAfield === null ? "F" : "T");
			
			builder.next()
				.property("groupId", codeId)
				.property("groupNm", codeNm)
				.property("codeId", codeId)
				.property("codeNm", codeNm)
				.property("isUsedAfield", isUsedAfield);
		}
		
		return builder.buildRows();
    }
    
    this.allTypeBy = function() {
    	return session.pathBy("/codes/afield").children().ascending("codeid").toAdRows("codeid typeid, cdnm typnm");
    }
    
    this.mappedListBy = function(catId) {
		var rowExprs = '';
		rowExprs += "this.afieldid, ";
		rowExprs += "this.mapping.afieldnm, ";
		rowExprs += "this.mapping.afieldexp, ";
		rowExprs += "this.mapping.typecd, ";
		rowExprs += "this.mapping.grpcd, ";
		rowExprs += "this.mapping.afieldlen, ";
		rowExprs += "this.mapping.afieldvlen, ";
		rowExprs += "this.mapping.afieldmaxlen, ";
		rowExprs += "this.mapping.ismndt, ";
		rowExprs += "this.mapping.filetypecd, ";
		rowExprs += "this.mapping.example.examid, ";
		rowExprs += "this.mapping.example.examnm, ";
		rowExprs += "this.mapping.example.expression ";
		
		return session.pathBy("/category_afields/" + catId + "/rels").children().ascending("orderlnno").toAdRows(rowExprs);
    }
    
	// TODO
    this.mappedTreeBy = function(catId) {
    	
    }

	// TODO
    this.mappedFullListBy = function(catId) {
    	
    }

    this.addMappingWith = function(catId, afieldId) {
    	var out = java.lang.System.out;
    	var isExistAField = false;
    	try {
    		var afieldNode = session.pathBy("/afields/" + afieldId);
    		isExistAField = true;
    	} catch(exception) {}
		
		
		if(isExistAField) {
			
			var maxOrderNo = 1;
			
			try {
				maxOrderNode = session.pathBy("/category_afields/" + catId + "/rels").children().descending("orderlnno").firstNode();
				maxOrderNo = maxOrderNode.property("orderlnno").asInt() + 1;
			} catch(exception) {} 
			
			return session.tranSync(function(wsession) {
						wsession.pathBy("/category_afields/" + catId + "/rels/" + afieldId)
						.property("afieldid", afieldId)
						.property("catid", catId)
						.property("orderlnno", maxOrderNo)
						.refTo("mapping", "/afields/" + afieldId);

						return 1;
					});
		}
		
		return 0;
    }

    this.delMappingWith = function(catId) {
    	return session.tranSync(function(wsession) {
    		wsession.pathBy("/category_afields/" + catId).removeSelf();
    		return 1;
    	});
    }
    
	// FIXME - Check whether surely working or not
    this.delNotExistExamWith = function(catId) {
		return session.tranSync(function(wsession) {
			var nodes = session.pathBy("/category_afield_exams/" + catId).children().iterator();
			var count = 0;

			while(nodes.hasNext()) {
				var node = nodes.next();
				var refs = session.pathBy("/category_afields/" + catId + "/rels/").children().iterator();

				while(refs.hasNext()) {
					var ref = refs.next();
					var lowerId = ref.property("afieldid").asString();
					var afieldRels = session.pathBy("/afield_rels").childQuery("", true).eq("upperid", "ROOT").eq("lowerid", lowerId).findOne();

					if (afieldRels === null) {
						wsession.pathBy(node.fqn()).removeSelf();
						count++;
						break;
					}
				}
			}
			return count;
		});
    	
    }

    // TODO
    this.usedCategoryBy = function(afieldId) {
    	
    }
    
    this.examListBy = function() {
		return session.pathBy("/examples").children().toAdRows('examid, examnm');
    }
    
    this.searchExamListBy = function(examNm, listNum, pageNo) {
    	var skip = (pageNo - 1) * listNum;
    	
		return session.pathBy("/examples").childQuery("")
			.gt("examid", 0)
			.wildcard("examnm", "*" + searchKey + "*")
			.offset(listNum)
			.skip(skip)
			.find()
			.toRows("examid, examnm, (case when this.afield.afieldid is null then 'F' else 'T' end) as isUsedAfield, (case when this.mapping.examid is null then 'F' else 'T' end) as isUsedCatExam");
    }
    
    this.addGroupWith = function(groupId, groupNm, groupExp) {
		session.tranSync(function(wsession) {
			var upperCodeId = 'afield_grp';
			var node = session.pathBy("/codes/afield_grp").childQuery("").eq("uppercdid", upperCodeId).ascending("cdlvl").findOne();
			var maxLvl = 1;
			
			if(node !== null) {
				maxLvl = node.property("cdlvl").asInt() + 1;
			}
			
			wsession.pathBy("/codes/afield_grp/" + groupId)
				.property("codeid", groupId)
				.property("cdnm", groupNm)
				.property("cdExp", groupExp)
				.property("cdlvl", maxLvl)
				.property("uppercdid", upperCodeId);
		});
    	return 1;
    }
    
    this.delGroupWith = function(groupId) {
		var node = session.pathBy("/afields").childQuery("").eq("grpcd", groupId).findOne();
		
		if(node !== null) {
			return -1;
		} else {
			return session.tranSync(function(wsession) {
				wsession.pathBy("/codes/afield_grp/" + groupId).removeSelf();
				return 1;
			});
		}
    }
    
    this.modGroupWith = function(groupId, groupNm, groupExp) {
		return session.tranSync(function(wsession) {
			wsession.pathBy("/codes/afield_grp/" + groupId).property("cdnm", groupNm).property("cdexp", groupExp);
			return 1;
		});
    }
    
    this.selGroupBy = function(groupId) {
    	return session.pathBy("/codes/afield_grp/" + groupId).toRows("codeid groupId, cdnm groupNm, cdexp groupExp");    	
    } 
    
    this.addExamWith = function(examId, examNm, expression) {
    	return session.tranSync(function(wsession) {
    		wsession.pathBy("/examples/" + examId).property("examid", examId).property("examnm", examNm).property("expression", exp);
    		return 1;
    	});
    }
    
    this.addExamDetailWith = function(examId, serNo, exp) {
		session.tranSync(function(wsession) {
			wsession.pathBy("/examples/" + examId + "/" + serNo).property("serNo", 1).property("examexp", exp);
			return 1;
		});
    }
    
    this.delExamWith = function(examId) {
    	return session.tranSync(function(wsession) {
    		wsession.pathBy('/examples/' + examId).removeSelf();
    		return 1;
    	});
    }
    
    this.selExamBy = function(examId) {
    	return session.pathBy("/examples/" + examId).children().toAdRows("parent.examid examId, parent.examnm, serno serNo, examexp examExp");
    }
    
    this.useExamBy = function(afieldId) {
    	return session.pathBy("/afields/" + afieldId).property("examid").asInt();
    }
    
    var lpad = function(str, length, padString) {
        while (str.length < length)
            str = padString + str;
        return str;
    }    
    
    this.isCompare = function(artId, modSerNo, afieldId, typeCd, value, op) {
		var afieldCont = session.pathBy('/afield_content/' + artId + '/' + afieldId + '/' + modSerNo);
		
		var dvalue = '' + afieldCont.property("dvalue").asString();
		var leftValue = '';
		var rightValue = '';
		
		if('Currency' === typeCd || 'Float' === typeCd || 'Number' === typeCd) {
			leftValue = lpad(dvalue, 20, '0');
		} else if("Date" === type) {
			if(value.length === 8) {
				leftValue = substring(0, 8);
			}
		} else {
			leftValue = dvalue;
		}
		
		if('Currency' === typeCd || 'Float' === typeCd || 'Number' === typeCd) {
			rightValue = lpad('' + value, 20, '0');
		}
		
		var compare = false;
		
		if('=' === op) {
			compare = leftValue === rightValue; 
		} else if('like' === op) {
			compare = leftValue.indexOf(rightValue) > -1;
		} else if(">" === op) {
			compare = leftValue > rightValue;
		} else if("<" === op) {
			compare = leftValue < rightValue;
		} else if(">=" === op) {
			compare = leftValue >= rightValue;
		} else if("<=" === op) {
			compare = leftValue <= rightValue;
		} else if("<>" === op) {
			compare = leftValue !== rightValue;
		} else {
			// 0
		}

		return compare ? 1 : 0;
    }
    
    this.getConvertedValue = function(artId, afieldId, modSerNo) {
    	try {
    		var afieldCont = session.pathBy('/afield_content/' + artId + '/' + afieldId + '/' + modSerNo);
    	} catch(exception) {
    		// if path not found, return ' '
    		return ' ';
    	}
		
		
		var dvalue = '' + afieldCont.property("dvalue").asString();
		var result = '';
		
		if('Currency' === typeCd || 'Float' === typeCd || 'Number' === typeCd) {
			result = lpad(dvalue, 20, '0');
		} else {
			result = dvalue;
		}
		
		return result;
    }
    
    this.getBooleanTypedValue = function(value) {
    	var upperValue = value.toUpperCase();
    	
    	if(upperValue === 'YES' || upperValue === 'TRUE' || upperValue === 'OK' || upperValue === 'T') {
    		return 'T';
    	} else {
    		return 'F';
    	}
    }
    
    this.getValue = function(artId, afieldId, modSerNo) {
    	try {
    		var afieldCont = session.pathBy('/afield_content/' + artId + '/' + afieldId + '/' + modSerNo);
    	} catch(exception) {
    		// if path not found, return ' '
    		return ' ';
    	}
		
		var dvalue = '' + afieldCont.property("dvalue").asString();
		
		if('Date' === typeCd) {
			return dvalue.substr(0, 8);
		} else {
			return dvalue;
		}
    }
    
    // TODO
    this.isExist = function(catId, afieldId) {
    	
    }
    
    this.afieldListBy = function() {
    	return session.pathBy('/afields').children().toAdRows("afieldid");
    }
    
    this.afieldTypeBy = function() {
    	return session.pathBy('/afields').children().toAdRows("afieldid, typecd");
    }
    
    // TODO
    this.makeTemporaryCategoryAfield = function(catId) {
    	
    }
    
    
}