package net.ion.craken.node.crud.impl;

import java.io.IOException;
import java.util.Map;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyId.PType;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.PropertyValue.VType;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

public class WorkspaceIndexUtil {

	public static IndexJob<Void> makeIndexJob(final WriteNode targetNode, final boolean includeSub, final IndexWriteConfig iwconfig) {
		IndexJob<Void> indexJob = new IndexJob<Void>() {
			@Override
			public Void handle(final IndexSession isession) throws Exception {

				indexNode(targetNode.toReadNode(), iwconfig, targetNode.fqn(), isession);
				if (includeSub) {
					targetNode.toReadNode().walkChildren().eachNode(new ReadChildrenEach<Void>() {
						@Override
						public Void handle(ReadChildrenIterator riter) {
							try {
								while (riter.hasNext()) {
									ReadNode next = riter.next();
									indexNode(next, iwconfig, next.fqn(), isession);
								}
							} catch (IOException ex) {
								ex.printStackTrace();
							}
							return null;
						}
					});
				}

				return null;
			}

			private void indexNode(final ReadNode wnode, final IndexWriteConfig iwconfig, final Fqn fqn, IndexSession isession) throws IOException {
				WriteDocument wdoc = isession.newDocument(fqn.toString());
				wdoc.keyword(EntryKey.PARENT, fqn.getParent().toString());
				wdoc.number(EntryKey.LASTMODIFIED, System.currentTimeMillis());

				Map<PropertyId, PropertyValue> valueMap = wnode.toMap();

				for (PropertyId pid : valueMap.keySet()) {
					PropertyValue pvalue = valueMap.get(pid);
					JsonArray jarray = pvalue.asJsonArray();
					final String propId = pid.getString();

					if (pid.type() == PType.NORMAL) {
						VType vtype = pvalue.type();
						for (JsonElement e : jarray.toArray()) {
							if (e == null)
								continue;
							FieldIndex fieldIndex = iwconfig.fieldIndex(propId);
							fieldIndex.index(wdoc, propId, vtype, e.isJsonObject() ? e.toString() : e.getAsString());
						}
					} else { // refer
						for (JsonElement e : jarray.toArray()) {
							if (e == null)
								continue;
							FieldIndex.KEYWORD.index(wdoc, '@' + propId, e.getAsString());
						}
					}
				}

				wdoc.update();
			}
		};
		return indexJob;
	}

}
