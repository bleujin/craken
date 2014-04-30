package net.ion.ics6.core;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

public class TestDataMigrator {

    private ReadSession session;

    public static TestDataMigrator create(ReadSession session) {
        TestDataMigrator migrator = new TestDataMigrator();
        migrator.session = session;

        return migrator;
    }

    private class Node {
        Node parent;
        List<Node> children = ListUtil.newList();

        String id;
        int orderNo = 1;
    }

    public int insertAFieldRels() throws Exception {
        File file = new File("./resource/ics6/data/afield_rels.txt");

        FileReader fr = new FileReader(file);
        final BufferedReader br = new BufferedReader(fr);

        return session.tranSync(new TransactionJob<Integer>() {
            @Override
            public Integer handle(WriteSession wsession) throws Exception {
                String line = null;
                int count = 0;

                Map<String, Node> nodes = MapUtil.newMap();
                Node root = new Node();
                root.id = "ROOT";
                root.orderNo = 1;

                while((line = br.readLine()) != null) {
                    String[] rows = StringUtil.split(line, "\t");

                    String upperId = rows[0];
                    String lowerId = rows[1];
                    int orderNo = Integer.parseInt(rows[2]);

                    Node n = new Node();
                    n.id = lowerId;
                    n.orderNo = orderNo;

                    if(nodes.containsKey(upperId)) {
                        Node p = nodes.get(upperId);
                        n.parent = p;
                    } else {
                        Node p = new Node();
                        p.id = upperId;
                    }

                    String path = String.format("/afield_rels/%s/%s", upperId, lowerId);




//                    wsession.pathBy(path).property("upperid", upperId).property("lowerid", lowerId).property("orderno", orderNo);
                    count++;
                }

                return Integer.valueOf(count);
            }
        });
    }

}

