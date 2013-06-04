package net.ion.craken.expression;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import javax.sql.RowSetMetaData;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.AdNodeRows;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.db.rowset.RowSetMetaDataImpl;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;

public class SelectProjection extends ValueObject {

	private final List<Projection> projections;
	private static Map<Class, Integer> TypeMappingMap = makeMapping();

	private static Map makeMapping() {
		Map<Class, Integer> result = MapUtil.newMap();

		result.put(String.class, Types.LONGVARCHAR);
		result.put(Integer.class, Types.INTEGER);
		result.put(Long.class, Types.BIGINT);
		result.put(BigDecimal.class, Types.NUMERIC);
		result.put(Double.class, Types.DOUBLE);
		result.put(Boolean.class, Types.BOOLEAN);
		result.put(Date.class, Types.DATE);
		result.put(java.util.Date.class, Types.DATE);

		return result;
	}

	public SelectProjection(List<Projection> projections) {
		this.projections = projections;
	}

	public RowSetMetaData getMetaType(ReadNode node, int columnCount) throws SQLException {
		RowSetMetaData meta = new RowSetMetaDataImpl();
		meta.setColumnCount(columnCount);

		if (node == null) {
			int i = 1;
			for (Projection pro : projections) {
				meta.setColumnName(i, pro.label()) ;
				meta.setColumnLabel(i, pro.label()) ;
				meta.setColumnType(i++, Types.OTHER);
			}
			return meta;
		} else {
			int i = 1;
			for (Projection pro : projections) {
				final Object value = pro.value(node);
				if (value == null){
					meta.setColumnName(i, pro.label()) ;
					meta.setColumnLabel(i, pro.label()) ;
					meta.setColumnType(i++, Types.OTHER);
					continue ;
				}
				
				Integer type = ObjectUtil.coalesce(TypeMappingMap.get(value.getClass()), Types.OTHER);
				meta.setColumnName(i, pro.label()) ;
				meta.setColumnLabel(i, pro.label()) ;
				meta.setColumnType(i++, type);
			}
		}

		return meta;
	}

	public AdNodeRows updateObject(AdNodeRows adNodeRows, ReadNode rnode) throws SQLException {
		for (int i = 1; i <= projections.size(); i++) {
			final Projection projection = projections.get(i - 1);
			
			adNodeRows.updateObject(i, projection.value(rnode));
		}
		return adNodeRows ;
	}

	public int size() {
		return projections.size();
	}

	
	
	

}
