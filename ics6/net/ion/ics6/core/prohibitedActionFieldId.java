package net.ion.ics6.core;

import net.ion.framework.util.ListUtil;

import java.util.List;

public class ProhibitedActionFieldId {

    private static List<String> list = ListUtil.newList();
    static {
        list.add("keyword");
        list.add("id");
        list.add("reportid");
        list.add("parentid");
        list.add("writedate");
        list.add("writetime");
        list.add("startdate");
        list.add("starttime");
        list.add("subject");
        list.add("priority");
        list.add("enddate");
        list.add("endtime");
        list.add("_status");
        list.add("_relation");
        list.add("afield");
        list.add("catid");
        list.add("partid");
        list.add("doc-id");
        list.add("doc-body");
        list.add("orderno");
        list.add("operday");
        list.add("expireday");
        list.add("useflg");
        list.add("credate");
        list.add("moddate");
        list.add("status");
        list.add("artfilenm");
        list.add("keyword");
        list.add("gourlloc");
        list.add("artid");
        list.add("modserno");
        list.add("reguserid");
        list.add("regday");
        list.add("modday");
        list.add("artsubject");
        list.add("artcont");
        list.add("upperartid");
        list.add("statuscd");
        list.add("image");
    }

    public static boolean isAllowed(String afieldId) {
        return list.contains(afieldId);
    }
}
