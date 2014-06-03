package net.ion.ics6.core;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.craken.node.crud.Filters;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.node.crud.WalkReadChildren;
import net.ion.craken.node.exception.NotFoundPath;
import net.ion.craken.script.DBFunction;
import net.ion.craken.script.JsonBuilder;
import net.ion.craken.script.ListBuilder;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Filter;

import com.google.common.base.Function;

public class TestAfieldPackage extends TestBasePackage {

	private TestDataMigrator migrator;

	public void setUp() throws Exception {
		super.setUp();
		migrator = TestDataMigrator.create(session);
		prepareAFieldData();
	}

	private int addTestAField(String afieldId, String afieldNm) throws SQLException {
		return addAField(afieldId, afieldNm, "dftgroup", "User Home Address", "String", 0, "F", 0, 0, 0, "ALL", 0, "seoul");
	}

	private int addAField(String afieldId, String afieldNm, String grpCd, String afieldExp, String typeCd, int aFieldLen, String isMndt, int indexOption, int aFieldMaxLen, int aFieldVLen, String fileTypeCd, int examId, String defaultValue) throws SQLException {
		int count = cs.execUpdate("afield@createWith", afieldId, afieldNm, grpCd, afieldExp, typeCd, aFieldLen, isMndt, indexOption, aFieldMaxLen, aFieldVLen, fileTypeCd, examId, defaultValue);
		return count;
	}

	public void testCreateWith() throws Exception {
		// given
		String afieldId = "address";
		String afieldNm = "HomeAddress";
		String grpCd = "dftgroup";
		String afieldExp = "User Home Address";
		String typeCd = "String";
		String isMndt = "F";

		int aFieldLen = 0;
		int indexOption = 0;
		int aFieldMaxLen = 0;
		int aFieldVLen = 0;
		String fileTypeCd = "ALL";
		int examId = 0;
		String defaultValue = "seoul";

		// when
		// int count = cs.execUpdate("afield@createWith", afieldId, afieldNm, grpCd, afieldExp, typeCd, aFieldLen, isMndt, indexOption, aFieldMaxLen, aFieldVLen, fileTypeCd, examId, defaultValue);
		int count = addAField(afieldId, afieldNm, grpCd, afieldExp, typeCd, aFieldLen, isMndt, indexOption, aFieldMaxLen, aFieldVLen, fileTypeCd, examId, defaultValue);

		// then
		assertEquals(1, count);
		assertEquals("HomeAddress", session.pathBy("/afields/address").property("afieldnm").asString());
	}

	public void testUpdateWith() throws SQLException {
		// given
		String afieldId = "address";
		String afieldNm = "NewAddress";
		String grpCd = "dftgroup";
		String afieldExp = "User New Address";
		String typeCd = "String";
		String isMndt = null;

		int aFieldLen = 80;
		int indexOption = 0;
		int aFieldMaxLen = 400;
		int aFieldVLen = 1;
		String fileTypeCd = "ALL";
		int examId = 0;
		String defaultValue = "Default";

		// when
		int rowCnt = cs.execUpdate("afield@updateWith", afieldId, afieldNm, grpCd, afieldExp, typeCd, aFieldLen, isMndt, indexOption, aFieldMaxLen, aFieldVLen, fileTypeCd, examId, defaultValue);

		// then
		assertEquals(1, rowCnt);
		ReadNode node = session.pathBy("/afields/address");

		assertEquals("NewAddress", node.property("afieldnm").stringValue());
		assertEquals("F", node.property("ismndt").asString());
	}

	public void testDelLowerAfieldWith() throws Exception {
		String upperId = "ch_set";
		cs.execUpdate("afield@delLowerAfieldWith", upperId);

		int remain = session.pathBy("/afield_rels").childQuery("").eq("upperid", upperId).find().size();
		assertEquals(0, remain);
	}

	public void testAddLowerAFieldWith() throws Exception {
		String upperId = "ROOT";
		String lowerId = "rel_test";
		int orderNo = 1;

		cs.execUpdate("afield@addLowerAFieldWith", upperId, lowerId, orderNo);

		session.pathBy("/afield_rels/rel_test").debugPrint();

		String secondUpperId = "rel_test";
		String secondLowerId = "child";
		int secondOrderNo = 1;

		cs.execUpdate("afield@addLowerAFieldWith", secondUpperId, secondLowerId, secondOrderNo);

		session.pathBy("/afield_rels/rel_test/child").debugPrint();
	}

	public void testSelLowerAFieldBy() throws IOException, ParseException, SQLException {
		String upperId = "ch_set";
		// List<ReadNode> nodes = session.pathBy("/afield_rels").childQuery("", true).eq("upperid", upperId).ascending("orderno").find().toList();
		//
		// ListBuilder builder = JsonBuilder.instance().newEmptyInlist();
		//
		// for (ReadNode node : nodes) {
		// String afieldId = node.property("lowerid").asString();
		// String afieldNm = session.ghostBy("/afields/" + afieldId).property("afieldnm").asString();
		//
		// builder.next().property("lowerId", afieldId).property("afieldNm", afieldNm);
		// }
		//
		// builder.buildRows().debugPrint();

		Rows rows = cs.execQuery("afield@selLowerAfieldBy", upperId);

		rows.debugPrint();
	}

	public void testSelLowerAFieldBy2() throws IOException, ParseException, SQLException {
		String upperId = "ch_set";
		session.pathBy("/afield_rels").childQuery("", true).eq("upperid", upperId).ascending("orderno").find().toRows("lowerid, upperid, ").debugPrint(); 

//		for (ReadNode node : nodes) {
//			String afieldId = node.property("lowerid").asString();
//			String afieldNm = session.ghostBy("/afields/" + afieldId).property("afieldnm").asString();
//
//			builder.next().property("lowerId", afieldId).property("afieldNm", afieldNm);
//		}
//		builder.buildRows().debugPrint();
	}

	public void testRetrieveWith() throws SQLException {
		// given
		addTestAField("address", "Address");

		// when
		Rows rows = cs.execQuery("afield@retrieveBy", "address");

		assertEquals(1, rows.getRowCount());
		Debug.line(rows.firstRow());
	}

	public void testSelectFilter() throws Exception {
		// afield_rels, RELSEQNO, RELKINDCD, UPPERCATID, UPPERARTID, LOWERCATID, LOWERARTID
		// given
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/afields/afielda");
				wsession.pathBy("/afields/afieldb");
				wsession.pathBy("/afields/afieldc");
				wsession.pathBy("/afields/afieldd");

				wsession.pathBy("/afield_rels/100/rels/afielda").property("lowerid", "afielda").property("upperid", "ROOT");
				wsession.pathBy("/afield_rels/100/rels/afieldb").property("lowerid", "afieldb").property("upperid", "ROOT");
				wsession.pathBy("/afield_rels/100/rels/afieldc").property("lowerid", "afieldc").property("upperid", "afielda");
				wsession.pathBy("/afield_rels/100/rels/afieldd").property("lowerid", "afieldd").property("upperid", "afieldc");

				return null;
			}
		});

		// when
		cs.execUpdate("afield@removeWith", "afieldc");

		// then
		assertTrue(session.ghostBy("/afields/afieldc").isGhost());
		assertTrue(session.ghostBy("/afield_rels/100/rels/afieldc").isGhost());
	}

	public void testUnloadWith() throws Exception {
		// given
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/category_afields/categoryA/rels/afieldA").property("afieldid", "afieldA").property("catid", "categoryA");
				wsession.pathBy("/category_afields/categoryA/rels/afieldB").property("afieldid", "afieldB").property("catid", "categoryA");
				wsession.pathBy("/category_afields/categoryA/rels/afieldC").property("afieldid", "afieldC").property("catid", "categoryA");
				wsession.pathBy("/category_afields/categoryB/rels/afieldB").property("afieldid", "afieldB").property("catid", "categoryB");
				wsession.pathBy("/category_afields/categoryC/rels/afieldA").property("afieldid", "afieldA").property("catid", "categoryC");
				wsession.pathBy("/category_afields/categoryC/rels/afieldC").property("afieldid", "afieldC").property("catid", "categoryC");

				return null;
			}
		});

		// when
		String v_catId = "categoryC";
		String v_srcCatId = "categoryB";

		cs.execUpdate("afield@unloadWith", v_catId, v_srcCatId);

		// then
		assertEquals(0, session.pathBy("/category_afields/categoryC/rels").children().count());
	}

	public void testLoadWith() throws Exception {
		// given
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/category_afields/categoryA/rels/afieldA").property("afieldid", "afieldA").property("catid", "categoryA").property("orderlnno", 1);
				wsession.pathBy("/category_afields/categoryA/rels/afieldB").property("afieldid", "afieldB").property("catid", "categoryA").property("orderlnno", 2);
				wsession.pathBy("/category_afields/categoryA/rels/afieldC").property("afieldid", "afieldC").property("catid", "categoryA").property("orderlnno", 3);
				wsession.pathBy("/category_afields/categoryB/rels/afieldB").property("afieldid", "afieldB").property("catid", "categoryB").property("orderlnno", 1);
				wsession.pathBy("/category_afields/categoryC/rels/afieldA").property("afieldid", "afieldA").property("catid", "categoryC").property("orderlnno", 1);
				wsession.pathBy("/category_afields/categoryC/rels/afieldC").property("afieldid", "afieldC").property("catid", "categoryC").property("orderlnno", 2);
				wsession.pathBy("/category_afields/categoryD/rels/afieldC").property("afieldid", "afieldC").property("catid", "categoryD").property("orderlnno", 1);

				return null;
			}
		});

		// when
		String v_catId = "categoryD";
		String v_srcCatId = "categoryA";

		int count = cs.execUpdate("afield@loadWith", v_catId, v_srcCatId);

		// then
		session.pathBy("/category_afields/categoryD/rels").children().debugPrint();
		assertEquals(3, count);
	}

	public void testListBy() throws Exception {
		// given
		String v_grpCd = "afg_mq";
		String v_typeCd = "";
		int v_listNum = 10;
		int v_pageNo = 1;

		// when
		Rows rows = cs.execQuery("afield@listBy", v_grpCd, v_typeCd, v_listNum, v_pageNo);

		rows.debugPrint();

		// then
		assertEquals(10, rows.getRowCount());
	}

	public void testAndOrSearchFilter() throws Exception {
		// given

		String v_grpCd = "afg_ishop";
		String v_typeCd = "Number";
		String v_searchKey = "상품";
		int v_listNum = 10;
		int v_pageNum = 1;
		int skip = (v_pageNum - 1) * v_listNum;

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/afields/number").property("afieldid", "number").property("afieldnm", "상품번호").property("afieldexp", "상품번호입력").property("typecd", "Number").property("grpcd", "afg_ishop").property("regdate", "20140422-134701");
				wsession.pathBy("/afields/point").property("afieldid", "point").property("afieldnm", "포인트").property("afieldexp", "").property("typecd", "Number").property("grpcd", "afg_ishop").property("regdate", "20140422-134702");
				wsession.pathBy("/afields/price").property("afieldid", "price").property("afieldnm", "가격").property("afieldexp", "").property("typecd", "Currency").property("grpcd", "afg_ishop").property("regdate", "20140422-134703");
				wsession.pathBy("/afields/release_date").property("afieldid", "release_date").property("afieldnm", "출시일").property("afieldexp", "").property("typecd", "Date").property("grpcd", "afg_ishop").property("regdate", "20140422-134704");
				wsession.pathBy("/afields/seller").property("afieldid", "seller").property("afieldnm", "판매자").property("afieldexp", "상품판매자이름").property("typecd", "String").property("grpcd", "afg_ishop").property("regdate", "20140422-134705");
				wsession.pathBy("/afields/spec").property("afieldid", "spec").property("afieldnm", "사양").property("afieldexp", "").property("typecd", "Summary").property("grpcd", "afg_ishop").property("regdate", "20140422-134706");

				return null;
			}
		});

		// when
		List<Filter> filters = ListUtil.newList();

		if (v_typeCd != null && !"".equals(v_typeCd)) {
			filters.add(Filters.eq("typecd", v_typeCd));
		}

		if (v_grpCd != null && !"".equals(v_grpCd)) {
			filters.add(Filters.eq("grpcd", v_grpCd));
		}

		filters.add(Filters.or(Filters.wildcard("afieldid", "*" + v_searchKey + "*"), Filters.wildcard("afieldnm", "*" + v_searchKey + "*"), Filters.wildcard("afieldexp", "*" + v_searchKey + "*")));

		ChildQueryResponse result = session.pathBy("/afields").childQuery("").filter(Filters.and(filters.toArray(new Filter[0]))).offset(v_listNum).skip(skip).ascending("grpcd").ascending("typecd").descending("regdate").find();
		// then
		result.debugPrint();
	}

	public void testSearchListBy() throws Exception {
		// given
		migrator.insertAFieldRels();

		// String v_grpCd = "afg_ishop";
		// String v_typeCd = "";
		// String v_searchKey = "price";
		int v_listNum = 10;
		int v_pageNum = 1;
		int skip = (v_pageNum - 1) * v_listNum;

		String v_grpCd = "afg_ishop";
		String v_typeCd = "Editor";
		String v_searchKey = "상품";

		Function<Iterator<ReadNode>, Map<String, String>> codeFunc = new Function<Iterator<ReadNode>, Map<String, String>>() {
			@Override
			public Map<String, String> apply(Iterator<ReadNode> nodes) {
				Map<String, String> codes = MapUtil.newMap();
				while (nodes.hasNext()) {
					ReadNode node = nodes.next();
					String codeId = node.property("codeid").asString();
					String cdnm = node.property("cdnm").asString();
					codes.put(codeId, cdnm);
				}
				return codes;
			}
		};

		Map<String, String> afieldTypeMap = session.pathBy("/codes/afield").children().transform(codeFunc);

		DBFunction func = new DBFunction();

		// when
		List<Filter> filters = ListUtil.newList();

		if (v_typeCd != null && !"".equals(v_typeCd)) {
			filters.add(Filters.eq("typecd", v_typeCd));
		}

		if (v_grpCd != null && !"".equals(v_grpCd)) {
			filters.add(Filters.eq("grpcd", v_grpCd));
		}

		filters.add(Filters.or(Filters.wildcard("afieldid", "*" + v_searchKey + "*"), Filters.wildcard("afieldnm", "*" + v_searchKey + "*"), Filters.wildcard("afieldexp", "*" + v_searchKey + "*")));

		ChildQueryResponse result = session.pathBy("/afields").childQuery("").filter(Filters.and(filters.toArray(new Filter[0]))).offset(v_listNum).skip(skip).ascending("grpcd").ascending("typecd").descending("regdate").find();

		ListBuilder builder = JsonBuilder.instance().newInlist();

		if (result.size() == 0) {
			// No nodes returned!
			builder.buildRows("empty").debugPrint();
			return;
		}

		IteratorList<ReadNode> afields = result.iterator();

		while (afields.hasNext()) {
			ReadNode afield = afields.next();

			String afieldId = afield.property("afieldid").asString();
			String typeCd = afield.property("typecd").asString();

			String isUsedCategory = "F";
			String isUsedAfield = "F";

			ReadNode catAFieldRow = session.pathBy("/category_afields").childQuery("").eq("afieldid", afieldId).findOne();
			ReadNode afieldRels = session.pathBy("/afield_rels").childQuery("").eq("lowerid", afieldId).ne("upperid", "ROOT").findOne();

			isUsedCategory = (catAFieldRow == null ? "F" : "T");
			isUsedAfield = (afieldRels == null ? "F" : "T");

			builder.property("afieldId", afieldId).property("afieldNm", afield.property("afieldnm").asString()).property("typeCd", typeCd).property("afieldExp", func.nvl(afield.property("afieldexp").asString(), "")).property("grpCd", afield.property("grpcd").asString())
					.property("grpNm", afieldTypeMap.get(typeCd)).property("isUsedCategory", isUsedCategory).property("isUsedAfield", isUsedAfield);

			if (afields.hasNext()) {
				builder.next();
			}
		}

		// then
		builder.buildRows().debugPrint();
	}

	public void testScriptSearchListBy() throws Exception {
		int v_listNum = 10;
		int v_pageNum = 1;

		String v_grpCd = "afg_ishop";
		String v_typeCd = "";
		String v_searchKey = "상품";

		Rows rows = cs.execQuery("afield@searchListBy", v_grpCd, v_typeCd, v_searchKey, v_listNum, v_pageNum);
		rows.debugPrint();
	}

	public void testAllGroupAndTypeBy() throws Exception {
		// given

		// when
		Rows rows = cs.execQuery("afield@allGroupAndTypeBy", new Object[0]);

		// then
		rows.debugPrint();
	}

	public void testAllGroupBy() throws Exception {
		Rows rows = session.pathBy("/codes/afield_grp").children().ascending("codeid").toAdRows("(case when this.afield_grp_member.afieldid is null then 'F' else 'T' end) as isUsing , codeid groupId, cdnm gropuNm, codeid codeId, cdnm codeNm");

		rows.debugPrint();
	}

	public void testAllTypeBy() throws SQLException {
		Rows rows = session.pathBy("/codes/afield").children().ascending("codeid").toAdRows("codeid typeid, cdnm typnm");
		rows.debugPrint();
	}

	public void testMappedListBy() throws SQLException {
		// given
		String catId = "categoryA";
		// when
		Rows rows = cs.execQuery("afield@mappedListBy", catId);

		// then
		rows.debugPrint();
	}

	public void testAddMappingWith() throws Exception {
		final String catId = "categoryA";
		final String afieldId = "mq_ev_pdate";

		int count = cs.execUpdate("afield@addMappingWith", catId, afieldId);

		Debug.line(count);
		session.pathBy("/category_afields/categoryA/rels/mq_ev_pdate").debugPrint();
	}

	public void testDelMappingWith() throws SQLException {
		cs.execUpdate("afield@delMappingWith", "categoryA");

		try {
			session.pathBy("/category_afields/categoryA");
			fail();
		} catch (NotFoundPath e) {

		}
	}

	public void testDelNotExistExamWith() throws Exception {
		// given
		final String catId = "16942";
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/category_afields/16942/rels/year").property("afieldid", "year").property("catid", "16942").property("orderlnno", 2);
				wsession.pathBy("/category_afields/16942/rels/month").property("afieldid", "month").property("catid", "16942").property("orderlnno", 2);
				return null;
			}
		});

		// when
		int count = session.tranSync(new TransactionJob<Integer>() {
			@Override
			public Integer handle(WriteSession wsession) throws Exception {
				List<ReadNode> nodes = session.pathBy("/category_afield_exams/" + catId).children().toList();
				int count = 0;

				for (ReadNode node : nodes) {
					List<ReadNode> refs = session.pathBy("/category_afields/" + catId + "/rels/").children().toList();

					for (ReadNode ref : refs) {
						String lowerId = ref.property("afieldid").asString();
						ReadNode afieldRels = session.pathBy("/afield_rels").childQuery("", true).eq("upperid", "ROOT").eq("lowerid", lowerId).findOne();

						if (afieldRels == null) {
							Debug.line(node.fqn());
							wsession.pathBy(node.fqn()).removeSelf();
							count++;
						}
					}
				}
				return count;
			}
		});
		// int count = cs.execUpdate("afield@delNotExistExamWith", catId);
		Debug.line(count);
	}

	private String pathByCatNmWithTrash(ReadNode node, String div) {
		List<String> names = ListUtil.newList();
		do {
			names.add(node.property("catnm").asString());
			node = node.parent();

		} while (!"/category/scat".equals(node.fqn().toString()) && !"/category/acat".equals(node.fqn().toString()) && !"/category/pcat".equals(node.fqn().toString()));

		Collections.reverse(names);
		return StringUtil.join(names.toArray(new String[0]), div);
	}

	public void testUsedCategoryBy() throws IOException, ParseException, SQLException {
		String lowerId = "ch_set";
		// IteratorList<ReadNode> iterator = session.pathBy("/afield_rels").childQuery("", true).eq("lowerid", lowerId).find().iterator();
		//
		// List<ReadNode> afieldRelsView = ListUtil.newList();
		//
		// while(iterator.hasNext()) {
		// ReadNode node = iterator.next();
		// do {
		// afieldRelsView.add(node);
		// node = node.parent();
		// } while(!node.fqn().toString().equals("/afield_rels"));
		// }
		//
		// ListBuilder builder = JsonBuilder.instance().newEmptyInlist();
		//
		// for(ReadNode node : afieldRelsView) {
		// IteratorList<ReadNode> categoryAfields = node.refs("category_mapping");
		// while(categoryAfields.hasNext()) {
		// ReadNode children = categoryAfields.next();
		// ReadNode category = children.ref("category");
		// String catId = children.property("catid").asString();
		// String catNm = category.property("catnm").asString();
		// boolean isSiteCat = category.property("isscat").asBoolean();
		// String fullPath = pathByCatNmWithTrash(category, ">");
		//
		// builder.next().property("catId", catId).property("catNm", catNm).property("isContentCategory", !isSiteCat).property("fullPath", fullPath);
		// }
		// }
		// Debug.line(builder.buildRows());
		Rows rows = cs.execQuery("afield@usedCategoryBy", lowerId);
		Debug.debug(rows);
	}

	public void testIsExist() throws ParseException, IOException, SQLException {
		String afieldId = "ch_set";
		String catId = "categoryC";

		// IteratorList<ReadNode> iterator = session.pathBy("/afield_rels").childQuery("", true).eq("lowerid", afieldId).find().iterator();
		// while (iterator.hasNext()) {
		// ReadNode node = iterator.next();
		// do {
		// IteratorList<ReadNode> refs = node.refs("category_mapping");
		// while(refs.hasNext()) {
		// if(catId.equals(refs.next().property("catid").asString())) {
		// Debug.line("Found!!");
		// return;
		// }
		// }
		// node = node.parent();
		// } while (!node.fqn().toString().equals("/afield_rels"));
		// }
		// Debug.line("False");

		Object result = cs.callFn("afield@isExist", catId, afieldId);

		Debug.line(result);
	}

	public void testUpperListBy() throws IOException, ParseException, SQLException {
		String afieldId = "ch_summary";
		Rows rows = cs.execQuery("afield@upperListBy", afieldId);

		rows.debugPrint();
		// IteratorList<ReadNode> iterator = session.pathBy("/afield_rels").childQuery("", true).eq("lowerid", afieldId).find().iterator();
		//
		// List<ReadNode> afieldRelsView = ListUtil.newList();
		//
		// while(iterator.hasNext()) {
		// ReadNode node = iterator.next();
		// do {
		// afieldRelsView.add(node);
		// node = node.parent();
		// } while(!node.fqn().toString().equals("/afield_rels"));
		// }
		//
		// ListBuilder builder = JsonBuilder.instance().newEmptyInlist();
		//
		// int rnum = 0;
		//
		// ReadNode afield = session.pathBy("/afields/" + afieldId);
		// String afieldNm = afield.property("afieldnm").asString();
		// String grpCd = afield.property("grpcd").asString();
		// String typeCd = afield.property("typecd").asString();
		//
		// builder.next()
		// .property("rnum", 0)
		// .property("afieldId", afieldId)
		// .property("lvl", 0)
		// .property("afieldIdExpr", afieldId)
		// .property("upperId", -1)
		// .property("loopCheck", " ")
		// .property("afieldNm", afieldNm)
		// .property("typeCd", typeCd)
		// .property("grpCd", grpCd);
		//
		//
		// for(ReadNode child: afieldRelsView) {
		//
		// String lowerId = child.property("lowerid").asString();
		// Debug.line(child.fqn(), lowerId);
		// String upperId = child.property("upperid").asString();
		//
		// int level = StringUtil.split(child.fqn().toString(), "/").length - 2;
		// String afieldIdExpr = StringUtil.leftPad("--", level * 2, "--");
		// String loopCheck = "<font color=red><b>[Afield Relation Loop Error]</b></font>";
		//
		// if(!upperId.equals(afieldId)) {
		// loopCheck = " ";
		// }
		//
		// if(!"ROOT".equals(upperId)) {
		// afield = session.pathBy("/afields/" + upperId);
		// afieldNm = afield.property("afieldnm").asString();
		// grpCd = afield.property("grpcd").asString();
		// typeCd = afield.property("typecd").asString();
		//
		// builder.next()
		// .property("rnum", rnum++)
		// .property("afieldId", lowerId)
		// .property("lvl", level)
		// .property("afieldIdExpr", afieldIdExpr + upperId)
		// .property("upperId", child.parent().property("afieldid").asString())
		// .property("loopCheck", loopCheck)
		// .property("afieldNm", afieldNm)
		// .property("typeCd", typeCd)
		// .property("grpCd", grpCd);
		// }
		// }

		// builder.buildRows().debugPrint();
	}

	public void testLowerListBy() throws SQLException {
		String afieldId = "ch_set";
		Rows rows = cs.execQuery("afield@lowerListBy", afieldId);
		rows.debugPrint();
		// WalkReadChildren walkChildren = session.pathBy("/afield_rels/" + afieldId).walkChildren();
		//
		// ListBuilder builder = JsonBuilder.instance().newEmptyInlist();
		//
		// IteratorList<ReadNode> iterator = walkChildren.iterator();
		// int rnum = 0;
		//
		// ReadNode afield = session.pathBy("/afields/" + afieldId);
		// String afieldNm = afield.property("afieldnm").asString();
		// String grpCd = afield.property("grpcd").asString();
		// String typeCd = afield.property("typecd").asString();
		//
		// builder.next()
		// .property("rnum", 0)
		// .property("afieldId", afieldId)
		// .property("lvl", 0)
		// .property("afieldIdExpr", afieldId)
		// .property("upperId", -1)
		// .property("loopCheck", " ")
		// .property("afieldNm", afieldNm)
		// .property("typeCd", typeCd)
		// .property("grpCd", grpCd);
		//
		//
		// while(iterator.hasNext()) {
		// ReadNode child = iterator.next();
		//
		// Debug.line(child.fqn());
		//
		// String lowerId = child.property("lowerid").asString();
		// int level = StringUtil.split(child.fqn().toString(), "/").length - 2;
		// String afieldIdExpr = StringUtil.leftPad("--", level * 2, "--");
		// String loopCheck = "<font color=red><b>[Afield Relation Loop Error]</b></font>";
		//
		// if(!lowerId.equals(afieldId)) {
		// loopCheck = " ";
		// }
		//
		// afield = session.pathBy("/afields/" + lowerId);
		// afieldNm = afield.property("afieldnm").asString();
		// grpCd = afield.property("grpcd").asString();
		// typeCd = afield.property("typecd").asString();
		//
		// builder.next()
		// .property("rnum", rnum++)
		// .property("afieldId", lowerId)
		// .property("lvl", level)
		// .property("afieldIdExpr", afieldIdExpr + lowerId)
		// .property("upperId", child.parent().property("afieldid").asString())
		// .property("loopCheck", loopCheck)
		// .property("afieldNm", afieldNm)
		// .property("typeCd", typeCd)
		// .property("grpCd", grpCd);
		//
		//
		// // Debug.line(iterator.next().fqn().toString());
		// }
		// Debug.line(builder.buildJson());

	}

	public void testAddGroupWith() throws Exception {
		// given
		String v_groupId = "ngroup";
		String v_groupNm = "New AField Group";
		String v_groupExp = "Explain";

		// when
		int rowCnt = cs.execUpdate("afield@addGroupWith", v_groupId, v_groupNm, v_groupExp);

		// then
		Debug.line(rowCnt);
		Debug.line(session.pathBy("/codes/afield_grp/ngroup").property("cdlvl").asInt());
	}

	public void testDelGroupWith() throws Exception {
		// given
		final String v_groupId = "at_t";

		// when
		int result = cs.execUpdate("afield@delGroupWith", v_groupId);

		// then
		assertEquals(1, result);
		assertTrue(session.ghostBy("/codes/afield_grp/at_t").isGhost());
	}

	public void testModGroupWith() throws Exception {
		// given
		final String groupId = "at_t";
		final String groupName = "newName";
		final String groupExp = "newExp";

		// when
		cs.execUpdate("afield@modGroupWith", groupId, groupName, groupExp);

		// then
		ReadNode node = session.pathBy("/codes/afield_grp/at_t");
		assertEquals(groupName, node.property("cdnm").asString());
		assertEquals(groupExp, node.property("cdexp").asString());
	}

	public void testSelGroupBy() throws IOException, ParseException, SQLException {
		String groupId = "afg_ishop";

		Rows rows = cs.execQuery("afield@selGroupBy", groupId);

		rows.debugPrint();
	}

	public void testAddExamWith() throws Exception {
		// given
		final int examId = 100;
		final String examNm = "airkjh";
		final String exp = "<>";

		// when
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/examples/" + examId).property("examid", examId).property("examnm", examNm).property("expression", exp);
				return null;
			}
		});

		// then
		session.pathBy("/examples/100").debugPrint();
	}

	public void testAddExamDetailWith() throws Exception {
		final int examId = 100;
		final int serNo = 1;
		final String exp = "<hahahaha>";

		session.tranSync(new TransactionJob<Integer>() {
			@Override
			public Integer handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/examples/" + examId + "/" + serNo).property("serNo", 1).property("examexp", exp);
				return 1;
			}
		});

		session.pathBy("/examples/100/1").debugPrint();
	}

	public void testSelExamBy() throws SQLException {
		int examId = 151;

		Rows rows = cs.execQuery("afield@selExamBy", examId);

		rows.debugPrint();
	}

	public void testUseExamBy() throws IOException, ParseException, SQLException {
		String afieldId = "price";
		int examId = session.pathBy("/afields/" + afieldId).property("examid").asInt();

		assertEquals(92, examId);
	}

	public void testIsCompare() throws Exception {
		// given
		String type = "Number";
		String value = "999";
		String op = "<";

		int result = cs.execUpdate("afield@isCompare", 100, 1, "testfield", type, value, op);

		Debug.line(result);
	}

	public void testSearchExamListBy() throws SQLException, IOException, ParseException {
		String searchKey = "";
		Rows rows = session.pathBy("/examples").childQuery("").gt("examid", 0).wildcard("examnm", "*" + searchKey + "*").offset(10).skip(10).find()
				.toRows("examid, examnm, (case when this.afield.afieldid is null then 'F' else 'T' end) as isUsedAfield, (case when this.mapping.examid is null then 'F' else 'T' end) as isUsedCatExam");

		rows.debugPrint();
	}

	public void testTransformer() throws SQLException {
		cs.execQuery("afield@transformer", "categoryA");
	}

	public void testMakeTemporaryCategoryAfield() throws ParseException, IOException {
		String catId = "categoryA";

		String[] afields = session.pathBy("/category_afields/" + catId + "/rels").children().transform(new Function<Iterator<ReadNode>, String[]>() {
			@Override
			public String[] apply(Iterator<ReadNode> iterator) {
				List<String> afields = ListUtil.newList();
				while (iterator.hasNext()) {
					String afieldId = iterator.next().property("afieldid").asString();
					afields.add(afieldId);
				}
				return afields.toArray(new String[0]);
			}
		});

		IteratorList<ReadNode> iterator = session.pathBy("/afield_rels").childQuery("", true).in("lowerid", afields).eq("upperid", "ROOT").find().iterator();
		int rownum = 1;

		while (iterator.hasNext()) {
			ReadNode child = iterator.next();
			WalkReadChildren walkChildren = (WalkReadChildren) session.pathBy("/afield_rels/" + child.property("").asString()).walkChildren().ascending("orderno");

			for (ReadNode walkChild : walkChildren) {
				String fqn = walkChild.fqn().toString();
				String topId = StringUtil.split(fqn, "/")[1];
				String upperId = walkChild.property("upperid").asString();
				String lowerId = walkChild.property("lowerid").asString();
				int level = StringUtil.split(fqn, "/").length - 2;
				rownum++;
			}
		}

	}

	private void prepareAFieldData() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				// category
				wsession.pathBy("/category/scat/categoryA").property("catid", "categoryA").property("catnm", "Category A").property("isscat", true);
				wsession.pathBy("/category/scat/categoryA/categoryB").property("catid", "categoryB").property("catnm", "Category B").property("isscat", true);
				wsession.pathBy("/category/scat/categoryA/categoryB/categoryC").property("catid", "categoryC").property("catnm", "Category C").property("isscat", true);

				// afield_rels
				wsession.pathBy("/afield_rels");
				wsession.pathBy("/afield_rels/mq_ev_pdate").property("upperid", "ROOT").property("lowerid", "mq_ev_pdate").property("orderno", 1);
				wsession.pathBy("/afield_rels/lyn_editor").property("upperid", "ROOT").property("lowerid", "lyn_editor").property("orderno", 1);
				wsession.pathBy("/afield_rels/sortaf").property("upperid", "ROOT").property("lowerid", "sortaf").property("orderno", 1);
				wsession.pathBy("/afield_rels/lyn_date").property("upperid", "ROOT").property("lowerid", "lyn_date").property("orderno", 1);
				wsession.pathBy("/afield_rels/lyn_image").property("upperid", "ROOT").property("lowerid", "lyn_image").property("orderno", 1);
				wsession.pathBy("/afield_rels/ch_set").property("upperid", "ROOT").property("lowerid", "ch_set").property("orderno", 1);
				wsession.pathBy("/afield_rels/ch_set/ch_string").property("upperid", "ch_set").property("lowerid", "ch_string").property("orderno", 1);
				wsession.pathBy("/afield_rels/ch_set/ch_boolean").property("upperid", "ch_set").property("lowerid", "ch_boolean").property("orderno", 2);
				wsession.pathBy("/afield_rels/ch_set/ch_file").property("upperid", "ch_set").property("lowerid", "ch_file").property("orderno", 3);
				wsession.pathBy("/afield_rels/ch_set/ch_image").property("upperid", "ch_set").property("lowerid", "ch_image").property("orderno", 4);
				wsession.pathBy("/afield_rels/ch_set/ch_summary").property("upperid", "ch_set").property("lowerid", "ch_summary").property("orderno", 5);
				wsession.pathBy("/afield_rels/ch_summary").property("upperid", "ROOT").property("lowerid", "ch_summary").property("orderno", 1);

				// category_article_tblc
				wsession.pathBy("/category_afields/categoryA/rels/number").property("afieldid", "number").property("catid", "categoryA").property("orderlnno", 1).refTo("mapping", "/afields/number").refTo("category", "/category/scat/categoryA");
				wsession.pathBy("/category_afields/categoryA/rels/point").property("afieldid", "point").property("catid", "categoryA").property("orderlnno", 2).refTo("mapping", "/afields/point").refTo("category", "/category/scat/categoryA");
				wsession.pathBy("/category_afields/categoryA/rels/price").property("afieldid", "price").property("catid", "categoryA").property("orderlnno", 3).refTo("mapping", "/afields/price").refTo("category", "/category/scat/categoryA");
				wsession.pathBy("/category_afields/categoryB/rels/release_date").property("afieldid", "release_date").property("catid", "categoryB").property("orderlnno", 1).refTo("mapping", "/afields/release_date").refTo("category", "/category/scat/categoryA/categoryB");
				wsession.pathBy("/category_afields/categoryC/rels/seller").property("afieldid", "seller").property("catid", "categoryC").property("orderlnno", 1).refTo("mapping", "/afields/seller").refTo("category", "/category/scat/categoryA/categoryB/categoryC");
				wsession.pathBy("/category_afields/16942/rels/year").property("afieldid", "year").property("catid", "16942").property("orderlnno", 1).refTo("mapping", "/afields/year");
				wsession.pathBy("/category_afields/16942/rels/month").property("afieldid", "month").property("catid", "16942").property("orderlnno", 2).refTo("mapping", "/afields/month");
				wsession.pathBy("/category_afields/categoryA/rels/ch_set").property("afieldid", "ch_set").property("catid", "categoryA").property("orderlnno", 1).refTo("mapping", "/afields/ch_set").refTo("category", "/category/scat/categoryA");
				wsession.pathBy("/category_afields/categoryA/rels/ch_set/ch_summary").property("afieldid", "ch_summary").property("catid", "categoryA").property("orderlnno", 1).refTo("mapping", "/afields/ch_summary").refTo("category", "/category/scat/categoryA");
				wsession.pathBy("/category_afields/categoryB/rels/ch_summary").property("afieldid", "ch_summary").property("catid", "categoryB").property("orderlnno", 1).refTo("mapping", "/afields/ch_summary").refTo("category", "/category/scat/categoryA/categoryB");
				wsession.pathBy("/category_afields/categoryC/rels/ch_set").property("afieldid", "ch_set").property("catid", "categoryC").property("orderlnno", 1).refTo("mapping", "/afields/ch_set").refTo("category", "/category/scat/categoryA/categoryB/categoryC");

				wsession.pathBy("/afield_rels/ch_set").refTos("category_mapping", "/category_afields/categoryA/rels/ch_set");
				wsession.pathBy("/afield_rels/ch_set").refTos("category_mapping", "/category_afields/categoryC/rels/ch_set");
				wsession.pathBy("/afield_rels/ch_set/ch_summary").refTo("category_mapping", "/category_afields/categoryA/rels/ch_set/ch_summary");
				wsession.pathBy("/afield_rels/ch_summary").refTo("category_mapping", "/category_afields/categoryB/rels/ch_summary");

				// example_tblc
				wsession.pathBy("/examples/1").property("examid", 1).property("examnm", "AB").property("expression", "expression1");
				wsession.pathBy("/examples/2").property("examid", 2).property("examnm", "BC").property("expression", "expression1");
				wsession.pathBy("/examples/3").property("examid", 3).property("examnm", "CD").property("expression", "expression1");
				wsession.pathBy("/examples/4").property("examid", 4).property("examnm", "DE").property("expression", "expression1");
				wsession.pathBy("/examples/5").property("examid", 5).property("examnm", "EF").property("expression", "expression1");
				wsession.pathBy("/examples/6").property("examid", 6).property("examnm", "FG").property("expression", "expression1");
				wsession.pathBy("/examples/7").property("examid", 7).property("examnm", "GH").property("expression", "expression1");
				wsession.pathBy("/examples/8").property("examid", 8).property("examnm", "HI").property("expression", "expression1");
				wsession.pathBy("/examples/9").property("examid", 9).property("examnm", "IJ").property("expression", "expression1");
				wsession.pathBy("/examples/10").property("examid", 10).property("examnm", "JK").property("expression", "expression1");
				wsession.pathBy("/examples/151").property("examid", 151).property("examnm", "HAHAHA").property("expression", "expression1");
				wsession.pathBy("/examples/151/1").property("serno", 1).property("examexp", "Exp1");
				wsession.pathBy("/examples/151/2").property("serno", 2).property("examexp", "Exp2");
				wsession.pathBy("/examples/151/3").property("serno", 3).property("examexp", "Exp3");

				wsession.pathBy("/examples/91").property("examid", 91).property("examnm", "ion").property("expression", "expression2");
				wsession.pathBy("/examples/93").property("examid", 93).property("examnm", "month").property("expression", "expression3");
				wsession.pathBy("/examples/92").property("examid", 92).property("examnm", "year").property("expression", "expression4");

				wsession.pathBy("/examples/151").refTo("afield", "/afields/number").refTo("mapping", "/category_afield_exams/c_ww11/year").refTo("mapping", "/category_afield_exams/c_us_c_over/year").refTo("mapping", "/category_afield_exams/cjinik11/year");
				wsession.pathBy("/examples/91").refTo("afield", "/afields/point").refTo("mapping", "/category_afield_exams/16942/month");
				wsession.pathBy("/examples/92").refTo("afield", "/afields/price").refTo("mapping", "/category_afield_exams/16942/year");

				// afield_group_vw
				wsession.pathBy("/codes/afield_grp/afg_ishop").property("codeid", "afg_ishop").property("cdnm", "AFG_ISHOP").property("uppercdid", "afield_grp").property("cdlvl", 3); // cdlvl_1 = /codes, cdlvl_2 = /codes/afield_grps
				wsession.pathBy("/codes/afield_grp/afg_mq").property("codeid", "afg_mq").property("cdnm", "AFG_MQ").property("uppercdid", "afield_grp").property("cdlvl", 3);
				wsession.pathBy("/codes/afield_grp/at_t").property("codeid", "at_t").property("cdnm", "AT_T").property("uppercdid", "afield_grp").property("cdlvl", 3);
				wsession.pathBy("/codes/afield_grp/afg_ishop/custom").property("codeid", "custom").property("cdnm", "Custom Group").property("uppercdid", "afg_ishop").property("cdlvl", 4);

				// afield_type_vw
				wsession.pathBy("/codes/afield/LongString").property("codeid", "LongString").property("cdnm", "LongString Type");
				wsession.pathBy("/codes/afield/Editor").property("codeid", "Editor").property("cdnm", "Editor Type");
				wsession.pathBy("/codes/afield/Set").property("codeid", "Set").property("cdnm", "Set Type");
				wsession.pathBy("/codes/afield/String").property("codeid", "String").property("cdnm", "String Type");
				wsession.pathBy("/codes/afield/Summary").property("codeid", "Summary").property("cdnm", "Summary Type");
				wsession.pathBy("/codes/afield/Number").property("codeid", "Number").property("cdnm", "Number Type");
				wsession.pathBy("/codes/afield/Date").property("codeid", "Date").property("cdnm", "Date Type");
				wsession.pathBy("/codes/afield/Image").property("codeid", "Image").property("cdnm", "Image Type");
				wsession.pathBy("/codes/afield/File").property("codeid", "File").property("cdnm", "File Type");
				wsession.pathBy("/codes/afield/Currency").property("codeid", "Currency").property("cdnm", "Currency Type");
				wsession.pathBy("/codes/afield/Boolean").property("codeid", "Boolean").property("cdnm", "Boolean Type");

				// category_afieldexam_tblc
				wsession.pathBy("/category_afield_exams/16942/month").property("examid", 91);
				wsession.pathBy("/category_afield_exams/c_ww11/year").property("examid", 151);
				wsession.pathBy("/category_afield_exams/16942/year").property("examid", 92);
				wsession.pathBy("/category_afield_exams/c_us_c_over/year").property("examid", 151);
				wsession.pathBy("/category_afield_exams/cjinik11/year").property("examid", 151);

				// afield
				wsession.pathBy("/afields/number").property("afieldid", "number").property("afieldnm", "상품번호").property("afieldexp", "상품번호입력").property("typecd", "Number").property("grpcd", "afg_ishop").property("regdate", "20140422-134701").property("examid", 151).refTo("example", "/examples/151");
				wsession.pathBy("/afields/point").property("afieldid", "point").property("afieldnm", "포인트").property("afieldexp", "").property("typecd", "Number").property("grpcd", "afg_ishop").property("regdate", "20140422-134702").property("examid", 91).refTo("example", "/examples/91");
				wsession.pathBy("/afields/price").property("afieldid", "price").property("afieldnm", "가격").property("afieldexp", "").property("typecd", "Currency").property("grpcd", "afg_ishop").property("regdate", "20140422-134703").property("examid", 92).refTo("example", "/examples/92");
				wsession.pathBy("/afields/release_date").property("afieldid", "release_date").property("afieldnm", "출시일").property("afieldexp", "").property("typecd", "Date").property("grpcd", "afg_ishop").property("regdate", "20140422-134704").property("examid", 93)
						.refTo("example", "/examples/93");
				wsession.pathBy("/afields/seller").property("afieldid", "seller").property("afieldnm", "판매자").property("afieldexp", "상품판매자이름").property("typecd", "String").property("grpcd", "afg_ishop").property("regdate", "20140422-134705").property("examid", 0);
				wsession.pathBy("/afields/spec").property("afieldid", "spec").property("afieldnm", "사양").property("afieldexp", "").property("typecd", "Summary").property("grpcd", "afg_ishop").property("regdate", "20140422-134706");
				wsession.pathBy("/afields/mq_ev_pdate").property("afieldid", "mq_ev_pdate").property("afieldnm", "날짜").property("afieldexp", "").property("typecd", "String").property("grpcd", "afg_ishop").property("regdate", "20140422-134706");
				wsession.pathBy("/afields/year").property("afieldid", "year").property("afieldnm", "년도").property("afieldexp", "").property("typecd", "String").property("grpcd", "afg_ishop").property("regdate", "20140422-134706");
				wsession.pathBy("/afields/month").property("afieldid", "month").property("afieldnm", "월").property("afieldexp", "").property("typecd", "String").property("grpcd", "afg_ishop").property("regdate", "20140422-134706");
				wsession.pathBy("/afields/ch_set").property("afieldid", "ch_set").property("afieldnm", "ContentHube_Set").property("afieldexp", "").property("typecd", "Set").property("grpcd", "ch_set").property("regdate", "20140422-134706");
				wsession.pathBy("/afields/ch_string").property("afieldid", "ch_string").property("afieldnm", "StringType").property("afieldexp", "").property("typecd", "String").property("grpcd", "ch_set").property("regdate", "20140422-134706");
				wsession.pathBy("/afields/ch_image").property("afieldid", "ch_image").property("afieldnm", "ImageType").property("afieldexp", "").property("typecd", "Image").property("grpcd", "ch_set").property("regdate", "20140422-134706");
				wsession.pathBy("/afields/ch_boolean").property("afieldid", "ch_boolean").property("afieldnm", "BooleanType").property("afieldexp", "").property("typecd", "Boolean").property("grpcd", "ch_set").property("regdate", "20140422-134706");
				wsession.pathBy("/afields/ch_file").property("afieldid", "ch_file").property("afieldnm", "FileType").property("afieldexp", "").property("typecd", "File").property("grpcd", "ch_set").property("regdate", "20140422-134706");
				wsession.pathBy("/afields/ch_summary").property("afieldid", "ch_summary").property("afieldnm", "SummaryType").property("afieldexp", "").property("typecd", "Summary").property("grpcd", "ch_set").property("regdate", "20140422-134706");

				wsession.pathBy("/codes/afield_grp/afg_ishop").refTo("afield_grp_member", "/afields/number");
				wsession.pathBy("/codes/afield_grp/afg_ishop").refTo("afield_grp_member", "/afields/point");
				wsession.pathBy("/codes/afield_grp/afg_ishop").refTo("afield_grp_member", "/afields/price");
				wsession.pathBy("/codes/afield_grp/afg_ishop").refTo("afield_grp_member", "/afields/release_date");
				wsession.pathBy("/codes/afield_grp/afg_ishop").refTo("afield_grp_member", "/afields/seller");
				wsession.pathBy("/codes/afield_grp/afg_ishop").refTo("afield_grp_member", "/afields/spec");

				for (int i = 0; i < 30; i++) {
					String afieldId = "afield" + i;
					String fqn = "/afields/" + afieldId;

					wsession.pathBy(fqn).property("afieldid", afieldId).property("afieldnm", afieldId.toUpperCase()).property("typecd", "String").property("grpcd", "afg_mq");
					wsession.pathBy("/codes/afield_grp/afg_mq").refTo("afield_grp_member", fqn);
				}

				wsession.pathBy("/afield_content/100/testfield/1").property("dvalue", "111").property("typecd", "Number");
				wsession.pathBy("/afield_content/100/testfield/2").property("dvalue", "222").property("typecd", "Number");
				wsession.pathBy("/afield_content/100/testfield/3").property("dvalue", "222").property("typecd", "Number");
				wsession.pathBy("/afield_content/100/testfield/4").property("dvalue", "333").property("typecd", "Number");
				wsession.pathBy("/afield_content/100/testfield/5").property("dvalue", "444").property("typecd", "Number");
				wsession.pathBy("/afield_content/100/testfield/6").property("dvalue", "555").property("typecd", "Number");

				return null;
			}
		});
	}

}
