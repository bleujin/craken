package net.ion.framework.db.rowset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.sql.RowSet;
import javax.sql.RowSetInternal;
import javax.sql.RowSetMetaData;
import javax.sql.RowSetReader;
import javax.sql.RowSetWriter;

// Referenced classes of package sun.jdbc.rowset:
//            BaseRowSet, BaseRow, InsertRow, Row, 
//            RowSetMetaDataImpl, RowSetReaderImpl, RowSetWriterImpl, SQLInputImpl, 
//            SerialArray, SerialBlob, SerialClob, SerialStruct

public class CachedRowSet extends BaseRowSet implements RowSet, RowSetInternal, Serializable, Cloneable {

	private transient RowSetReader rowSetReader;
	private transient RowSetWriter rowSetWriter;
	private transient Connection conn;
	private transient ResultSetMetaData RSMD;
	private RowSetMetaData RowSetMD;
	private int keyCols[];
	private String tableName;
	private Vector<Row> cachedRows;
	private int cursorPos;
	private int absolutePos;
	private int numDeleted;
	private int numRows;
	private InsertRow insertRow;
	private boolean onInsertRow;
	private int currentRow;
	private boolean lastValueNull;

	public CachedRowSet() throws SQLException {
		setReader(new RowSetReaderImpl());
		setWriter(new RowSetWriterImpl());
		initParams();
		initContainer();
		initProperties();
		onInsertRow = false;
		insertRow = null;
	}

	protected Vector<Row> getCachedRows() {
		return cachedRows;
	}

	protected BaseRow getRow(int rowindex) {
		if (onInsertRow)
			return insertRow;
		else
			return (Row) cachedRows.get(rowindex);
	}

	public boolean absolute(int i) throws SQLException {
		if (i == 0 || getType() == 1003)
			throw new SQLException("absolute: Invalid cursor operation.");
		if (i > 0) {
			if (i > numRows)
				afterLast();
			else if (absolutePos <= 0)
				internalFirst();
		} else if (cursorPos + i < 0)
			beforeFirst();
		else if (absolutePos >= 0)
			internalLast();
		while (absolutePos != i)
			if (absolutePos >= i ? !internalPrevious() : !internalNext())
				break;
		notifyCursorMoved();
		return !isAfterLast() && !isBeforeFirst();
	}

	public void acceptChanges() throws SQLException {
		if (onInsertRow)
			throw new SQLException("Invalid operation while on insert row");
		RowSetWriter rowsetwriter = getWriter();
		int i = cursorPos;
		boolean flag = true;
		if (rowsetwriter != null) {
			int j = cursorPos;
			flag = rowsetwriter.writeData(this);
			cursorPos = j;
		}
		if (flag)
			setOriginal();
		else
			throw new SQLException("acceptChanges Failed");
	}

	public void acceptChanges(Connection connection) throws SQLException {
		setConnection(connection);
		acceptChanges();
	}

	public void afterLast() throws SQLException {
		if (numRows > 0) {
			cursorPos = numRows + 1;
			absolutePos = 0;
			notifyCursorMoved();
		}
	}

	public void beforeFirst() throws SQLException {
		if (getType() == 1003) {
			throw new SQLException("beforeFirst: Invalid cursor operation.");
		} else {
			cursorPos = 0;
			absolutePos = 0;
			notifyCursorMoved();
			return;
		}
	}

	public void cancelRowDelete() throws SQLException {
		if (!getShowDeleted())
			return;
		checkCursor();
		if (onInsertRow)
			throw new SQLException("Invalid cursor position.");
		Row row = (Row) getCurrentRow();
		if (row.getDeleted()) {
			row.clearDeleted();
			numDeleted--;
			notifyRowChanged();
		}
	}

	public void cancelRowInsert() throws SQLException {
		checkCursor();
		if (onInsertRow)
			throw new SQLException("Invalid cursor position.");
		Row row = (Row) getCurrentRow();
		if (row.getInserted()) {
			cachedRows.remove(cursorPos);
			numRows--;
			notifyRowChanged();
		} else {
			throw new SQLException("Illegal operation on non-inserted row");
		}
	}

	public void cancelRowUpdates() throws SQLException {
		checkCursor();
		if (onInsertRow)
			throw new SQLException("Invalid cursor position.");
		Row row = (Row) getCurrentRow();
		if (row.getUpdated()) {
			row.clearUpdated();
			notifyRowChanged();
		}
	}

	private void checkCursor() throws SQLException {
		if (isAfterLast() || isBeforeFirst())
			throw new SQLException("Invalid cursor position");
		else
			return;
	}

	private void checkIndex(int i) throws SQLException {
		if (i < 1 || i > getMetaData().getColumnCount())
			throw new SQLException("Invalid column index");
		else
			return;
	}

	public void clearWarnings() {
		throw new UnsupportedOperationException();
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void close() throws SQLException {
		release();
	}

	public boolean columnUpdated(int i) throws SQLException {
		checkCursor();
		if (onInsertRow)
			throw new SQLException("Operation invalid on insert row");
		else
			return ((Row) getCurrentRow()).getColUpdated(i - 1);
	}

	private Object convertNumeric(Object obj, int i, int j) throws SQLException {
		if (i == j)
			return obj;
		if (!isNumeric(j) && !isString(j))
			throw new SQLException("1.Datatype Mismatch: " + j);
		try {
			switch (j) {
			case -7:
				Integer integer = new Integer(obj.toString().trim());
				return integer.equals(new Integer(0)) ? new Boolean(false) : new Boolean(true);

			case -6:
				return new Byte(obj.toString().trim());

			case 5: // '\005'
				return new Short(obj.toString().trim());

			case 4: // '\004'
				return new Integer(obj.toString().trim());

			case -5:
				return new Long(obj.toString().trim());

			case 2: // '\002'
			case 3: // '\003'
				return new BigDecimal(obj.toString().trim());

			case 6: // '\006'
			case 7: // '\007'
				return new Float(obj.toString().trim());

			case 8: // '\b'
				return new Double(obj.toString().trim());

			case -1:
			case 1: // '\001'
			case 12: // '\f'
				return new String(obj.toString());

			case -4:
			case -3:
			case -2:
			case 0: // '\0'
			case 9: // '\t'
			case 10: // '\n'
			case 11: // '\013'
			default:
				throw new SQLException("2.Data Type Mismatch: " + j);
			}
		} catch (NumberFormatException _ex) {
			throw new SQLException("3.Data Type Mismatch: " + j);
		}
	}

	private Object convertTemporal(Object obj, int i, int j) throws SQLException {
		if (i == j)
			return obj;
		if (isNumeric(j) || !isString(j) && !isTemporal(j))
			throw new SQLException("Datatype Mismatch");
		try {
			switch (j) {
			case 91: // '['
				if (i == 93)
					return new Date(((Timestamp) obj).getTime());
				else
					throw new SQLException("Data Type Mismatch");

			case 93: // ']'
				if (i == 92)
					return new Timestamp(((Time) obj).getTime());
				else
					return new Timestamp(((Date) obj).getTime());

			case 92: // '\\'
				if (i == 93)
					return new Time(((Timestamp) obj).getTime());
				else
					throw new SQLException("Data Type Mismatch");

			case -1:
			case 1: // '\001'
			case 12: // '\f'
				return new String(obj.toString());
			}
			throw new SQLException("Data Type Mismatch");
		} catch (NumberFormatException _ex) {
			throw new SQLException("Data Type Mismatch");
		}
	}

	public RowSet createCopy() throws SQLException {
		ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream objectoutputstream = new ObjectOutputStream(bytearrayoutputstream);
			objectoutputstream.writeObject(this);
		} catch (IOException ioexception) {
			throw new SQLException("Clone failed: " + ioexception.getMessage());
		}
		ObjectInputStream objectinputstream;
		try {
			ByteArrayInputStream bytearrayinputstream = new ByteArrayInputStream(bytearrayoutputstream.toByteArray());
			objectinputstream = new ObjectInputStream(bytearrayinputstream);
		} catch (StreamCorruptedException streamcorruptedexception) {
			throw new SQLException("Clone failed: " + streamcorruptedexception.getMessage());
		} catch (IOException ioexception1) {
			throw new SQLException("Clone failed: " + ioexception1.getMessage());
		}
		try {
			return (RowSet) objectinputstream.readObject();
		} catch (ClassNotFoundException classnotfoundexception) {
			throw new SQLException("Clone failed: " + classnotfoundexception.getMessage());
		} catch (OptionalDataException optionaldataexception) {
			throw new SQLException("Clone failed: " + optionaldataexception.getMessage());
		} catch (IOException ioexception2) {
			throw new SQLException("Clone failed; " + ioexception2.getMessage());
		}
	}

	public RowSet createShared() throws SQLException {
		RowSet rowset;
		try {
			rowset = (RowSet) clone();
		} catch (CloneNotSupportedException clonenotsupportedexception) {
			throw new SQLException(clonenotsupportedexception.getMessage());
		}
		return rowset;
	}

	public void deleteRow() throws SQLException {
		checkCursor();
		((Row) getCurrentRow()).setDeleted();
		numDeleted++;
		notifyRowChanged();
	}

	public void execute() throws SQLException {
		execute(null);
	}

	public void execute(Connection connection) throws SQLException {
		setConnection(connection);
		getReader().readData(this);
	}

	public int findColumn(String s) throws SQLException {
		return getColIdxByName(s);
	}

	public boolean first() throws SQLException {
		if (getType() == 1003) {
			throw new SQLException("First: Invalid cursor operation.");
		} else {
			boolean flag = internalFirst();
			notifyCursorMoved();
			return flag;
		}
	}

	protected Object getColumnObject(BaseRow row, int i) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object result = row.getColumnObject(i);
		setLastValueNull(result == null);
		return result;
	}

	public Array getArray(int i) throws SQLException {
		return getArray(getCurrentRow(), i);
	}

	protected Array getArray(int rowindex, int colindex) throws SQLException {
		return getArray(getRow(rowindex), colindex);
	}

	private Array getArray(BaseRow row, int i) throws SQLException {
		if (getMetaData().getColumnType(i) != 2003)
			throw new SQLException("Datatype Mismatch");
		Array array = (Array) getColumnObject(row, i);
		if (array == null) {
			return null;
		} else {
			return array;
		}
	}

	public Array getArray(String s) throws SQLException {
		return getArray(getColIdxByName(s));
	}

	public InputStream getAsciiStream(int i) throws SQLException {
		super.asciiStream = null;
		return getAsciiStream(getCurrentRow(), i);
	}

	protected InputStream getAsciiStream(int rowindex, int colindex) throws SQLException {
		return getAsciiStream(getRow(rowindex), colindex);
	}

	private InputStream getAsciiStream(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		}
		try {
			if (isString(getMetaData().getColumnType(i)))
				super.asciiStream = new ByteArrayInputStream(((String) obj).getBytes("ASCII"));
			else
				throw new SQLException("Data type mismatch");
		} catch (UnsupportedEncodingException unsupportedencodingexception) {
			throw new SQLException(unsupportedencodingexception.getMessage());
		}
		return super.asciiStream;
	}

	public InputStream getAsciiStream(String s) throws SQLException {
		return getAsciiStream(getColIdxByName(s));
	}

	public BigDecimal getBigDecimal(int i) throws SQLException {
		return getBigDecimal(getCurrentRow(), i);
	}

	protected BigDecimal getBigDecimal(int rowindex, int colindex, int scale) throws SQLException {
		return getBigDecimal(getRow(rowindex), colindex).setScale(scale);
	}

	private BigDecimal getBigDecimal(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		}
		try {
			return new BigDecimal(obj.toString().trim());
		} catch (NumberFormatException _ex) {
			throw new SQLException("getDouble Failed on value (" + obj.toString().trim() + ") in column " + i);
		}
	}

	/**
	 * @deprecated Method getBigDecimal is deprecated
	 */

	public BigDecimal getBigDecimal(int i, int j) throws SQLException {
		return getBigDecimal(i).setScale(j);
	}

	public BigDecimal getBigDecimal(String s) throws SQLException {
		return getBigDecimal(getColIdxByName(s));
	}

	/**
	 * @deprecated Method getBigDecimal is deprecated
	 */

	public BigDecimal getBigDecimal(String s, int i) throws SQLException {
		return getBigDecimal(getColIdxByName(s), i);
	}

	public InputStream getBinaryStream(int i) throws SQLException {
		super.binaryStream = null;
		return getBinaryStream(getCurrentRow(), i);
	}

	protected InputStream getBinaryStream(int rowindex, int colindex) throws SQLException {
		return getBinaryStream(getRow(rowindex), colindex);
	}

	private InputStream getBinaryStream(BaseRow row, int i) throws SQLException {
		if (getMetaData().getColumnType(i) == 2004) { // in oracle..
			Object obj = getColumnObject(row, i);
			return ((SerialBlob) obj).getBinaryStream();
		}
		if (!isBinary(getMetaData().getColumnType(i)))
			throw new SQLException("Data Type Mismatch");
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		} else {
			super.binaryStream = new ByteArrayInputStream((byte[]) obj);
			return super.binaryStream;
		}
	}

	public InputStream getBinaryStream(String s) throws SQLException {
		return getBinaryStream(getColIdxByName(s));
	}

	public Blob getBlob(int i) throws SQLException {
		return getBlob(getCurrentRow(), i);
	}

	protected Blob getBlob(int rowindex, int colindex) throws SQLException {
		return getBlob(getRow(rowindex), colindex);
	}

	private Blob getBlob(BaseRow row, int i) throws SQLException {
		if (getMetaData().getColumnType(i) != 2004)
			throw new SQLException("Datatype Mismatch");
		Blob blob = (Blob) getColumnObject(row, i);
		if (blob == null) {
			setLastValueNull(true);
			return null;
		} else {
			return blob;
		}
	}

	public Blob getBlob(String s) throws SQLException {
		return getBlob(getColIdxByName(s));
	}

	public boolean getBoolean(int i) throws SQLException {
		return getBoolean(getCurrentRow(), i);
	}

	protected boolean getBoolean(int rowindex, int colindex) throws SQLException {
		return getBoolean(getRow(rowindex), colindex);
	}

	private boolean getBoolean(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return false;
		}
		if (obj instanceof Boolean)
			return ((Boolean) obj).booleanValue();
		try {
			Double double1 = new Double(obj.toString());
			return double1.compareTo(new Double(0.0D)) != 0;
		} catch (NumberFormatException _ex) {
			throw new SQLException("getBoolen Failed on value (" + obj.toString().trim() + ") in column " + i);
		}
	}

	public boolean getBoolean(String s) throws SQLException {
		return getBoolean(getColIdxByName(s));
	}

	public byte getByte(int i) throws SQLException {
		return getByte(getCurrentRow(), i);
	}

	protected byte getByte(int rowindex, int colindex) throws SQLException {
		return getByte(getRow(rowindex), colindex);
	}

	public byte getByte(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return 0;
		}
		try {
			return (new Byte(obj.toString())).byteValue();
		} catch (NumberFormatException _ex) {
			throw new SQLException("getByte Failed on value (" + obj.toString() + ") in column " + i);
		}
	}

	public byte getByte(String s) throws SQLException {
		return getByte(getColIdxByName(s));
	}

	public byte[] getBytes(int i) throws SQLException {
		return getBytes(getCurrentRow(), i);
	}

	protected byte[] getBytes(int rowindex, int colindex) throws SQLException {
		return getBytes(getRow(rowindex), colindex);
	}

	private byte[] getBytes(BaseRow row, int i) throws SQLException {
		if (!isBinary(getMetaData().getColumnType(i)))
			throw new SQLException("Data Type Mismatch");
		else
			return (byte[]) getColumnObject(row, i);
	}

	public byte[] getBytes(String s) throws SQLException {
		return getBytes(getColIdxByName(s));
	}

	public Reader getCharacterStream(int i) throws SQLException {
		return getCharacterStream(getCurrentRow(), i);
	}

	protected Reader getCharacterStream(int rowindex, int colindex) throws SQLException {
		return getCharacterStream(getRow(rowindex), colindex);
	}

	private Reader getCharacterStream(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		}

		if (isBinary(getMetaData().getColumnType(i))) {
			super.charStream = new InputStreamReader(new ByteArrayInputStream((byte[]) obj));
		} else if (isString(getMetaData().getColumnType(i))) {
			super.charStream = new StringReader(obj.toString());
		} else {
			throw new SQLException("Datatye mismatch");
		}
		return super.charStream;
	}

	public Reader getCharacterStream(String s) throws SQLException {
		return getCharacterStream(getColIdxByName(s));
	}

	public Clob getClob(int i) throws SQLException {
		return getClob(getCurrentRow(), i);
	}

	protected Clob getClob(int rowindex, int colindex) throws SQLException {
		return getClob(getRow(rowindex), colindex);
	}

	private Clob getClob(BaseRow row, int i) throws SQLException {
		if (getMetaData().getColumnType(i) != 2005)
			throw new SQLException("Datatype Mismatch");
		return (Clob) getColumnObject(row, i);
	}

	public Clob getClob(String s) throws SQLException {
		return getClob(getColIdxByName(s));
	}

	private int getColIdxByName(String s) throws SQLException {
		int i = getMetaData().getColumnCount();
		for (int j = 1; j <= i; j++) {
			String s1 = getMetaData().getColumnName(j);
			if (s1 != null && s.equalsIgnoreCase(s1))
				return j;
		}

		throw new SQLException("Invalid column name");
	}

	public Connection getConnection() throws SQLException {
		return conn;
	}

	protected BaseRow getCurrentRow() {
		if (onInsertRow)
			return insertRow;
		else
			return (Row) cachedRows.get(Math.max(cursorPos - 1, 0));
	}

	public String getCursorName() throws SQLException {
		throw new SQLException("Positioned updates not supported");
	}

	public Date getDate(int i) throws SQLException {
		return getDate(getCurrentRow(), i);
	}

	protected Date getDate(int rowindex, int colindex) throws SQLException {
		return getDate(getRow(rowindex), colindex);
	}

	private Date getDate(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		}

		switch (getMetaData().getColumnType(i)) {
		case 91: // '['
			return (Date) obj;

		case 93: // ']'
			long l = ((Timestamp) obj).getTime();
			return new Date(l);

		case -1:
		case 1: // '\001'
		case 12: // '\f'
			try {
				DateFormat dateformat = DateFormat.getDateInstance();
				return (Date) dateformat.parse(obj.toString());
			} catch (ParseException _ex) {
				throw new SQLException("getDate Failed on value (" + obj.toString().trim() + ") in column " + i);
			}
		}
		throw new SQLException("getDate Failed on value (" + obj.toString().trim() + ") in column " + i + "no conversion available");
	}

	public Date getDate(int i, Calendar calendar) throws SQLException {
		return getDate(getCurrentRow(), i, calendar);
	}

	protected Date getDate(int rowindex, int colindex, Calendar calendar) throws SQLException {
		return getDate(getRow(rowindex), colindex, calendar);
	}

	private Date getDate(BaseRow row, int i, Calendar calendar) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		} else {
			obj = convertTemporal(obj, getMetaData().getColumnType(i), 91);
			Calendar calendar1 = Calendar.getInstance();
			calendar1.setTime((java.util.Date) obj);
			calendar.set(1, calendar1.get(1));
			calendar.set(2, calendar1.get(2));
			calendar.set(5, calendar1.get(5));
			return new Date(calendar.getTime().getTime());
		}
	}

	public Date getDate(String s) throws SQLException {
		return getDate(getColIdxByName(s));
	}

	public Date getDate(String s, Calendar calendar) throws SQLException {
		return getDate(getColIdxByName(s), calendar);
	}

	public double getDouble(int i) throws SQLException {
		return getDouble(getCurrentRow(), i);
	}

	protected double getDouble(int rowindex, int colindex) throws SQLException {
		return getDouble(getRow(rowindex), colindex);
	}

	private double getDouble(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return 0.0D;
		}
		try {
			return (new Double(obj.toString().trim())).doubleValue();
		} catch (NumberFormatException _ex) {
			throw new SQLException("getDouble Failed on value (" + obj.toString().trim() + ") in column " + i);
		}
	}

	public double getDouble(String s) throws SQLException {
		return getDouble(getColIdxByName(s));
	}

	public float getFloat(int i) throws SQLException {
		return getFloat(getCurrentRow(), i);
	}

	protected float getFloat(int rowindex, int colindex) throws SQLException {
		return getFloat(getRow(rowindex), colindex);
	}

	public float getFloat(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return 0.0F;
		}
		try {
			return (new Float(obj.toString())).floatValue();
		} catch (NumberFormatException _ex) {
			throw new SQLException("getfloat Failed on value (" + obj.toString().trim() + ") in column " + i);
		}
	}

	public float getFloat(String s) throws SQLException {
		return getFloat(getColIdxByName(s));
	}

	public int getInt(int i) throws SQLException {
		return getInt(getCurrentRow(), i);
	}

	protected int getInt(int rowindex, int colindex) throws SQLException {
		return getInt(getRow(rowindex), colindex);
	}

	private int getInt(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return 0;
		}
		try {
			return (new Integer(obj.toString().trim())).intValue();
		} catch (NumberFormatException _ex) {
			throw new SQLException("getInt Failed on value (" + obj.toString() + ") in column " + i);
		}
	}

	public int getInt(String s) throws SQLException {
		return getInt(getColIdxByName(s));
	}

	public int[] getKeyColumns() throws SQLException {
		return keyCols;
	}

	public long getLong(int i) throws SQLException {
		return getLong(getCurrentRow(), i);
	}

	protected long getLong(int rowindex, int colindex) throws SQLException {
		return getLong(getRow(rowindex), colindex);
	}

	private long getLong(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return 0L;
		}
		try {
			return (new Long(obj.toString().trim())).longValue();
		} catch (NumberFormatException _ex) {
			throw new SQLException("getLong Failed on value (" + obj.toString().trim() + ") in column " + i);
		}
	}

	public long getLong(String s) throws SQLException {
		return getLong(getColIdxByName(s));
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return RowSetMD;
	}

	public Object getObject(int i) throws SQLException {
		return getObject(getCurrentRow(), i, getTypeMap());
	}

	protected Object getObject(int rowindex, int colindex) throws SQLException {
		return getObject(getRow(rowindex), colindex, getTypeMap());
	}

	private Object getObject(BaseRow row, int i, Map map) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		}
		if (obj instanceof Struct) {
			Struct struct = (Struct) obj;
			Class class1 = (Class) map.get(struct.getSQLTypeName());
			if (class1 != null) {
				SQLData sqldata = null;
				try {
					sqldata = (SQLData) class1.newInstance();
				} catch (InstantiationException instantiationexception) {
					throw new SQLException("Unable to instantiate: " + instantiationexception.getMessage());
				} catch (IllegalAccessException illegalaccessexception) {
					throw new SQLException("Unable to instantiate: " + illegalaccessexception.getMessage());
				}
				Object aobj[] = struct.getAttributes(map);
				SQLInputImpl sqlinputimpl = new SQLInputImpl(aobj, map);
				sqldata.readSQL(sqlinputimpl, struct.getSQLTypeName());
				return sqldata;
			}
		}
		return obj;
	}

	protected Object getObject(int rowindex, int colindex, Map map) throws SQLException {
		return getObject(getRow(rowindex), colindex, map);
	}

	public Object getObject(String s) throws SQLException {
		return getObject(getColIdxByName(s));
	}

	public ResultSet getOriginal() throws SQLException {
		CachedRowSet cachedrowset = new CachedRowSet();
		cachedrowset.RowSetMD = (RowSetMetaData) getMetaData();
		cachedrowset.numRows = numRows;
		cachedrowset.cursorPos = 0;
		cachedrowset.setReader(null);
		cachedrowset.setWriter(null);
		int i = getMetaData().getColumnCount();
		Row row;
		for (Iterator<Row> iterator = cachedRows.iterator(); iterator.hasNext(); cachedrowset.cachedRows.add(row))
			row = new Row(i, ((Row) iterator.next()).getOrigRow());

		return cachedrowset;
	}

	public ResultSet getOriginalRow() throws SQLException {
		CachedRowSet cachedrowset = new CachedRowSet();
		cachedrowset.RowSetMD = (RowSetMetaData) getMetaData();
		cachedrowset.numRows = 1;
		cachedrowset.cursorPos = 0;
		cachedrowset.setReader(null);
		cachedrowset.setWriter(null);
		Row row = new Row(getMetaData().getColumnCount(), getCurrentRow().getOrigRow());
		cachedrowset.cachedRows.add(row);
		return cachedrowset;
	}

	public RowSetReader getReader() throws SQLException {
		return rowSetReader;
	}

	public Ref getRef(int i) throws SQLException {
		return getRef(getCurrentRow(), i);
	}

	protected Ref getRef(int rowindex, int colindex) throws SQLException {
		return getRef(getRow(rowindex), colindex);
	}

	private Ref getRef(BaseRow row, int i) throws SQLException {
		if (getMetaData().getColumnType(i) != 2006)
			throw new SQLException("Datatype Mismatch");
		Ref ref = (Ref) getColumnObject(row, i);
		if (ref == null) {
			return null;
		} else {
			return ref;
		}
	}

	public Ref getRef(String s) throws SQLException {
		return getRef(getColIdxByName(s));
	}

	public int getRow() throws SQLException {
		if (numRows > 0 && cursorPos > 0 && cursorPos < numRows + 1 && !getShowDeleted() && !rowDeleted())
			return absolutePos;
		else
			return 0;
	}

	public short getShort(int i) throws SQLException {
		return getShort(getCurrentRow(), i);
	}

	protected short getShort(int rowindex, int colindex) throws SQLException {
		return getShort(getRow(rowindex), colindex);
	}

	private short getShort(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return 0;
		}
		try {
			return (new Short(obj.toString().trim())).shortValue();
		} catch (NumberFormatException _ex) {
			throw new SQLException("getShort Failed on value (" + obj.toString() + ") in column " + i);
		}
	}

	public short getShort(String s) throws SQLException {
		return getShort(getColIdxByName(s));
	}

	public Statement getStatement() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getString(int i) throws SQLException {
		return getString(getCurrentRow(), i);
	}

	protected String getString(int rowindex, int colindex) throws SQLException {
		return getString(getRow(rowindex), colindex);
	}

	private String getString(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		} else {
			return obj.toString();
		}
	}

	public String getString(String s) throws SQLException {
		return getString(getColIdxByName(s));
	}

	public String getTableName() throws SQLException {
		return tableName;
	}

	public Time getTime(int i) throws SQLException {
		return getTime(getCurrentRow(), i);
	}

	protected Time getTime(int rowindex, int colindex) throws SQLException {
		return getTime(getRow(rowindex), colindex);
	}

	private Time getTime(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		}
		switch (getMetaData().getColumnType(i)) {
		case 92: // '\\'
			return (Time) obj;

		case 93: // ']'
			long l = ((Timestamp) obj).getTime();
			return new Time(l);

		case -1:
		case 1: // '\001'
		case 12: // '\f'
			try {
				DateFormat dateformat = DateFormat.getTimeInstance();
				return (Time) dateformat.parse(obj.toString());
			} catch (ParseException _ex) {
				throw new SQLException("getTime Failed on value (" + obj.toString().trim() + ") in column " + i);
			}
		}
		throw new SQLException("getTime Failed on value (" + obj.toString().trim() + ") in column " + i + "no conversion available");
	}

	protected Time getTime(int rowindex, int colindex, Calendar calendar) throws SQLException {
		return getTime(getRow(rowindex), colindex, calendar);
	}

	public Time getTime(int i, Calendar calendar) throws SQLException {
		return getTime(getCurrentRow(), i, calendar);
	}

	private Time getTime(BaseRow row, int i, Calendar calendar) throws SQLException {
		Object obj = row.getColumnObject(i);
		if (obj == null) {
			return null;
		} else {
			return new Time(getTimestamp(i, calendar).getTime());
		}
	}

	public Time getTime(String s) throws SQLException {
		return getTime(getColIdxByName(s));
	}

	public Time getTime(String s, Calendar calendar) throws SQLException {
		return getTime(getColIdxByName(s), calendar);
	}

	public Timestamp getTimestamp(int i) throws SQLException {
		return getTimestamp(getCurrentRow(), i);
	}

	protected Timestamp getTimestamp(int rowindex, int colindex) throws SQLException {
		return getTimestamp(getRow(rowindex), colindex);
	}

	private Timestamp getTimestamp(BaseRow row, int i) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		}
		switch (getMetaData().getColumnType(i)) {
		case 93: // ']'
			return (Timestamp) obj;

		case 92: // '\\'
			long l = ((Time) obj).getTime();
			return new Timestamp(l);

		case 91: // '['
			long l1 = ((Date) obj).getTime();
			return new Timestamp(l1);

		case -1:
		case 1: // '\001'
		case 12: // '\f'
			try {
				DateFormat dateformat = DateFormat.getTimeInstance();
				return (Timestamp) dateformat.parse(obj.toString());
			} catch (ParseException _ex) {
				throw new SQLException("getTime Failed on value (" + obj.toString().trim() + ") in column " + i);
			}
		}
		throw new SQLException("getTime Failed on value (" + obj.toString().trim() + ") in column " + i + "no conversion available");
	}

	public Timestamp getTimestamp(int i, Calendar calendar) throws SQLException {
		return getTimestamp(getCurrentRow(), i, calendar);
	}

	protected Timestamp getTimestamp(int rowindex, int colindex, Calendar calendar) throws SQLException {
		return getTimestamp(getRow(rowindex), colindex, calendar);
	}

	private Timestamp getTimestamp(BaseRow row, int i, Calendar calendar) throws SQLException {
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		} else {
			obj = convertTemporal(obj, getMetaData().getColumnType(i), 93);
			Calendar calendar1 = Calendar.getInstance();
			calendar1.setTime((java.util.Date) obj);
			calendar.set(1, calendar1.get(1));
			calendar.set(2, calendar1.get(2));
			calendar.set(5, calendar1.get(5));
			calendar.set(11, calendar1.get(11));
			calendar.set(12, calendar1.get(12));
			calendar.set(13, calendar1.get(13));
			return new Timestamp(calendar.getTime().getTime());
		}
	}

	public Timestamp getTimestamp(String s) throws SQLException {
		return getTimestamp(getColIdxByName(s));
	}

	public Timestamp getTimestamp(String s, Calendar calendar) throws SQLException {
		return getTimestamp(getColIdxByName(s), calendar);
	}

	/**
	 * @deprecated Method getUnicodeStream is deprecated
	 */

	public InputStream getUnicodeStream(int i) throws SQLException {
		super.unicodeStream = null;
		return getUnicodeStream(getCurrentRow(), i);
	}

	protected InputStream getUnicodeStream(int rowindex, int colindex) throws SQLException {
		return getUnicodeStream(getRow(rowindex), colindex);
	}

	private InputStream getUnicodeStream(BaseRow row, int i) throws SQLException {
		if (!isBinary(getMetaData().getColumnType(i)) && !isString(getMetaData().getColumnType(i)))
			throw new SQLException("Data Type Mismatch");
		Object obj = getColumnObject(row, i);
		if (obj == null) {
			return null;
		} else {
			super.unicodeStream = new StringBufferInputStream(obj.toString());
			return super.unicodeStream;
		}
	}

	/**
	 * @deprecated Method getUnicodeStream is deprecated
	 */

	public InputStream getUnicodeStream(String s) throws SQLException {
		return getUnicodeStream(getColIdxByName(s));
	}

	public SQLWarning getWarnings() {
		throw new UnsupportedOperationException();
	}

	public RowSetWriter getWriter() throws SQLException {
		return rowSetWriter;
	}

	private void initContainer() {
		cachedRows = new Vector<Row>(100);
		cursorPos = 0;
		absolutePos = 0;
		numRows = 0;
		numDeleted = 0;
	}

	private void initMetaData(RowSetMetaData rowsetmetadata, ResultSetMetaData resultsetmetadata) throws SQLException {
		int i = resultsetmetadata.getColumnCount();
		rowsetmetadata.setColumnCount(i);
		for (int j = 1; j <= i; j++) {
			rowsetmetadata.setAutoIncrement(j, resultsetmetadata.isAutoIncrement(j));
			rowsetmetadata.setCaseSensitive(j, resultsetmetadata.isCaseSensitive(j));
			rowsetmetadata.setCurrency(j, resultsetmetadata.isCurrency(j));
			rowsetmetadata.setNullable(j, resultsetmetadata.isNullable(j));
			rowsetmetadata.setSigned(j, resultsetmetadata.isSigned(j));
			rowsetmetadata.setSearchable(j, resultsetmetadata.isSearchable(j));
			rowsetmetadata.setColumnDisplaySize(j, resultsetmetadata.getColumnDisplaySize(j));
			rowsetmetadata.setColumnLabel(j, resultsetmetadata.getColumnLabel(j));
			rowsetmetadata.setColumnName(j, resultsetmetadata.getColumnName(j));
			// rowsetmetadata.setSchemaName(j, resultsetmetadata.getSchemaName(j));
			rowsetmetadata.setPrecision(j, resultsetmetadata.getPrecision(j));
			rowsetmetadata.setScale(j, resultsetmetadata.getScale(j));
			rowsetmetadata.setTableName(j, resultsetmetadata.getTableName(j));
			rowsetmetadata.setCatalogName(j, resultsetmetadata.getCatalogName(j));
			rowsetmetadata.setColumnType(j, resultsetmetadata.getColumnType(j));
			rowsetmetadata.setColumnTypeName(j, resultsetmetadata.getColumnTypeName(j));
		}

	}

	private void initProperties() throws SQLException {
		setShowDeleted(false);
		setQueryTimeout(0);
		setMaxRows(0);
		setMaxFieldSize(0);
		setType(1004);
		setConcurrency(1007);
		setReadOnly(true);
		setTransactionIsolation(2);
		setEscapeProcessing(true);
		setTypeMap(null);
	}

	public void insertRow() throws SQLException {
		if (!onInsertRow || !insertRow.isCompleteRow((RowSetMetaData) getMetaData()))
			throw new SQLException("Failed to insert Row");
		Row row = new Row(getMetaData().getColumnCount(), insertRow.getOrigRow());
		row.setInserted();
		int i;
		if (currentRow >= numRows || currentRow < 0)
			i = numRows;
		else
			i = currentRow;
		cachedRows.add(i, row);
		numRows++;
		notifyRowChanged();
	}

	protected boolean internalFirst() throws SQLException {
		boolean flag = false;
		if (numRows > 0) {
			cursorPos = 1;
			if (!getShowDeleted() && rowDeleted())
				flag = internalNext();
			else
				flag = true;
		}
		if (flag)
			absolutePos = 1;
		else
			absolutePos = 0;
		return flag;
	}

	protected boolean internalLast() throws SQLException {
		boolean flag = false;
		if (numRows > 0) {
			cursorPos = numRows;
			if (!getShowDeleted() && rowDeleted())
				flag = internalPrevious();
			else
				flag = true;
		}
		if (flag)
			absolutePos = numRows - numDeleted;
		else
			absolutePos = 0;
		return flag;
	}

	protected boolean internalNext() throws SQLException {
		boolean flag = false;
		do {
			if (cursorPos < numRows) {
				cursorPos++;
				flag = true;
				continue;
			}
			if (cursorPos != numRows)
				continue;
			cursorPos++;
			flag = false;
			break;
		} while (!getShowDeleted() && rowDeleted());
		if (flag)
			absolutePos++;
		else
			absolutePos = 0;
		return flag;
	}

	protected boolean internalPrevious() throws SQLException {
		boolean flag = false;
		do {
			if (cursorPos > 1) {
				cursorPos--;
				flag = true;
				continue;
			}
			if (cursorPos != 1)
				continue;
			cursorPos--;
			flag = false;
			break;
		} while (!getShowDeleted() && rowDeleted());
		if (flag)
			absolutePos--;
		else
			absolutePos = 0;
		return flag;
	}

	public boolean isAfterLast() throws SQLException {
		return cursorPos == numRows + 1 && numRows > 0;
	}

	public boolean isBeforeFirst() throws SQLException {
		return cursorPos == 0 && numRows > 0;
	}

	private boolean isBinary(int i) {
		switch (i) {
		case -4:
		case -3:
		case -2:
			return true;
		}
		return false;
	}

	public boolean isFirst() throws SQLException {
		int i = cursorPos;
		int j = absolutePos;
		internalFirst();
		if (cursorPos == i) {
			return true;
		} else {
			cursorPos = i;
			absolutePos = j;
			return false;
		}
	}

	public boolean isLast() throws SQLException {
		int i = cursorPos;
		int j = absolutePos;
		boolean flag = getShowDeleted();
		setShowDeleted(true);
		internalLast();
		if (cursorPos == i) {
			setShowDeleted(flag);
			return true;
		} else {
			setShowDeleted(flag);
			cursorPos = i;
			absolutePos = j;
			return false;
		}
	}

	private boolean isNumeric(int i) {
		switch (i) {
		case -7:
		case -6:
		case -5:
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
		case 8: // '\b'
			return true;

		case -4:
		case -3:
		case -2:
		case -1:
		case 0: // '\0'
		case 1: // '\001'
		default:
			return false;
		}
	}

	private boolean isString(int i) {
		switch (i) {
		case -1:
		case 1: // '\001'
		case 12: // '\f'
			return true;
		}
		return false;
	}

	private boolean isTemporal(int i) {
		switch (i) {
		case 91: // '['
		case 92: // '\\'
		case 93: // ']'
			return true;
		}
		return false;
	}

	public boolean last() throws SQLException {
		if (getType() == 1003) {
			throw new SQLException("last: TYPE_FORWARD_ONLY");
		} else {
			boolean flag = internalLast();
			notifyCursorMoved();
			return flag;
		}
	}

	private void makeRowOriginal(Row row) {
		if (row.getInserted())
			row.clearInserted();
		if (row.getUpdated())
			row.moveCurrentToOrig();
	}

	public void moveToCurrentRow() throws SQLException {
		if (!onInsertRow) {
			return;
		} else {
			cursorPos = currentRow;
			onInsertRow = false;
			return;
		}
	}

	public void moveToInsertRow() throws SQLException {
		if (getType() == 1003)
			throw new SQLException("last: TYPE_FORWARD_ONLY");
		if (insertRow == null) {
			int i = getMetaData().getColumnCount();
			if (i > 0)
				insertRow = new InsertRow(i);
		}
		onInsertRow = true;
		currentRow = cursorPos;
		cursorPos = -1;
		insertRow.initInsertRow();
	}

	public boolean next() throws SQLException {
		if (cursorPos < 0 || cursorPos >= numRows + 1) {
			throw new SQLException("Invalid Cursor position");
		} else {
			boolean flag = internalNext();
			notifyCursorMoved();
			return flag;
		}
	}

	public void populate(ResultSet resultset) throws SQLException {
		populate(resultset, 0, Integer.MAX_VALUE);
	}

	public void populate(ResultSet resultset, int skip, int length) throws SQLException {
		Map map = getTypeMap();
		RSMD = resultset.getMetaData();

		int j = RSMD.getColumnCount();
		if (skip > 0) {
			while (skip-- > 0 && resultset.next())
				;
		}

		int i;
		for (i = 0; resultset.next() && i < length; i++) {
			Row row = new Row(j);
			for (int k = 1; k <= j; k++) {
				Object obj;
				if (map == null)
					obj = resultset.getObject(k);
				else
					obj = resultset.getObject(k, map);

				if (obj instanceof Struct)
					obj = new SerialStruct((Struct) obj, map);
				else if (obj instanceof SQLData)
					obj = new SerialStruct((SQLData) obj, map);
				else if (obj instanceof Blob) {
					obj = new SerialBlob((Blob) obj);
				} else if (obj instanceof Clob) {
					SerialClob serialClob = new SerialClob((Clob) obj);
					obj = serialClob;

//					try {
//						Reader reader = ((Clob)obj).getCharacterStream();
//						String str = IOUtil.toString(reader);
//						obj = str ;
//						reader.close() ;
//					} catch (IOException e) {
//						throw new SQLException(e.getMessage()) ;
//					}
				} else if (obj instanceof Array)
					obj = new SerialArray((Array) obj, map);
				row.initColumnObject(k, obj);
			}

			cachedRows.add(row);
		}
		// TODO screen count ?

		numRows = i;
		RowSetMD = new RowSetMetaDataImpl();
		initMetaData(RowSetMD, RSMD);
		RSMD = null;
		notifyRowSetChanged();
	}

	public boolean previous() throws SQLException {
		if (getType() == 1003)
			throw new SQLException("last: TYPE_FORWARD_ONLY");
		if (cursorPos < 0 || cursorPos > numRows + 1) {
			throw new SQLException("Invalid Cursor position");
		} else {
			boolean flag = internalPrevious();
			notifyCursorMoved();
			return flag;
		}
	}

	public void refreshRow() throws SQLException {
		checkCursor();
		if (onInsertRow) {
			throw new SQLException("Invalid cursor position.");
		} else {
			Row row = (Row) getCurrentRow();
			row.clearUpdated();
			return;
		}
	}

	public boolean relative(int i) throws SQLException {
		if (numRows == 0 || isBeforeFirst() || isAfterLast() || getType() == 1003)
			throw new SQLException("relative: Invalid cursor operation");
		if (i == 0)
			return true;
		if (i > 0) {
			if (cursorPos + i > numRows) {
				afterLast();
			} else {
				for (int j = 0; j < i; j++)
					if (!internalNext())
						break;

			}
		} else if (cursorPos + i < 0) {
			beforeFirst();
		} else {
			for (int k = i; k > 0; k--)
				if (!internalPrevious())
					break;

		}
		notifyCursorMoved();
		return !isAfterLast() && !isBeforeFirst();
	}

	public void release() throws SQLException {
		initContainer();
		notifyRowSetChanged();
	}

	protected void removeCurrentRow() {
		cachedRows.remove(cursorPos - 1);
		numRows--;
	}

	public void restoreOriginal() throws SQLException {
		for (Iterator<Row> iterator = cachedRows.iterator(); iterator.hasNext();) {
			Row row = iterator.next();
			if (row.getInserted()) {
				iterator.remove();
				numRows--;
			} else {
				if (row.getDeleted())
					row.clearDeleted();
				if (row.getUpdated())
					row.clearUpdated();
			}
		}

		cursorPos = 0;
		notifyRowSetChanged();
	}

	public boolean rowDeleted() throws SQLException {
		if (isAfterLast() || isBeforeFirst() || onInsertRow)
			throw new SQLException("Invalid cursor position");
		else
			return ((Row) getCurrentRow()).getDeleted();
	}

	public boolean rowInserted() throws SQLException {
		checkCursor();
		if (onInsertRow)
			throw new SQLException("Operation invalid on insert row");
		else
			return ((Row) getCurrentRow()).getInserted();
	}

	public boolean rowUpdated() throws SQLException {
		checkCursor();
		if (onInsertRow)
			throw new SQLException("Operation invalid on insert row");
		else
			return ((Row) getCurrentRow()).getUpdated();
	}

	public void setCommand(String s) throws SQLException {
		super.setCommand(s);
	}

	private void setConnection(Connection connection) {
		conn = connection;
	}

	public void setKeyColumns(int ai[]) throws SQLException {
		int i = 0;
		if (getMetaData() != null) {
			i = getMetaData().getColumnCount();
			if (ai.length > i)
				throw new SQLException("Invalid key columns");
		}
		keyCols = new int[ai.length];
		for (int j = 0; j < ai.length; j++) {
			if (getMetaData() != null && (ai[j] <= 0 || ai[j] > i))
				throw new SQLException("Invalid column index: " + ai[j]);
			keyCols[j] = ai[j];
		}

	}

	protected void setLastValueNull(boolean flag) {
		lastValueNull = flag;
	}

	public void setMetaData(RowSetMetaData rowsetmetadata) throws SQLException {
		RowSetMD = rowsetmetadata;
	}

	public void setOriginal() throws SQLException {
		for (Iterator iterator = cachedRows.iterator(); iterator.hasNext();) {
			Row row = (Row) iterator.next();
			makeRowOriginal(row);
			if (row.getDeleted()) {
				iterator.remove();
				numRows--;
			}
		}

		numDeleted = 0;
		notifyRowSetChanged();
	}

	public void setOriginalRow() throws SQLException {
		if (onInsertRow)
			throw new SQLException("Invalid operation on Insert Row");
		Row row = (Row) getCurrentRow();
		makeRowOriginal(row);
		if (row.getDeleted()) {
			removeCurrentRow();
			numRows--;
		}
	}

	public void setReader(RowSetReader rowsetreader) throws SQLException {
		rowSetReader = rowsetreader;
	}

	public void setTableName(String s) throws SQLException {
		if (s == null)
			tableName = null;
		else
			tableName = new String(s);
	}

	public void setWriter(RowSetWriter rowsetwriter) throws SQLException {
		rowSetWriter = rowsetwriter;
	}

	public int size() {
		return numRows;
	}

	public Collection toCollection() throws SQLException {
		int i = 0;
		int j = getMetaData().getColumnCount();
		TreeMap<Integer, Vector> treemap = new TreeMap<Integer, Vector>();
		for (Iterator<Row> iterator = cachedRows.iterator(); iterator.hasNext();) {
			Vector vector = new Vector(j);
			Row row = iterator.next();
			for (int k = 1; k <= j; k++)
				vector.add(row.getColumnObject(k));

			treemap.put(new Integer(i), vector);
			i++;
		}

		return (Collection) treemap;
	}

	public Collection toCollection(int i) throws SQLException {
		Vector vector = new Vector(numRows);
		Row row;
		for (Iterator<Row> iterator = cachedRows.iterator(); iterator.hasNext(); vector.add(row.getColumnObject(i)))
			row = iterator.next();

		return vector;
	}

	public void updateAsciiStream(int i, InputStream inputstream, int j) {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream(String s, InputStream inputstream, int i) {
		throw new UnsupportedOperationException();
	}

	public void updateBigDecimal(int i, BigDecimal bigdecimal) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertNumeric(bigdecimal, 2, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateBigDecimal(String s, BigDecimal bigdecimal) throws SQLException {
		updateBigDecimal(getColIdxByName(s), bigdecimal);
	}

	public void updateBinaryStream(int i, InputStream inputstream, int j) throws SQLException {
		checkIndex(i);
		checkCursor();
		if (!isBinary(getMetaData().getColumnType(i)))
			throw new SQLException("Data Type Mismatch");
		byte abyte0[] = new byte[j];
		try {
			int k = 0;
			do
				k += inputstream.read(abyte0, k, j - k);
			while (k != -1);
		} catch (IOException _ex) {
			throw new SQLException("read failed for binaryStream");
		}
		getCurrentRow().setColumnObject(i, abyte0);
	}

	public void updateBinaryStream(String s, InputStream inputstream, int i) throws SQLException {
		updateBinaryStream(getColIdxByName(s), inputstream, i);
	}

	public void updateBoolean(int i, boolean flag) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertNumeric(new Boolean(flag), -7, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateBoolean(String s, boolean flag) throws SQLException {
		updateBoolean(getColIdxByName(s), flag);
	}

	public void updateByte(int i, byte byte0) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertNumeric(new Byte(byte0), -6, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateByte(String s, byte byte0) throws SQLException {
		updateByte(getColIdxByName(s), byte0);
	}

	public void updateBytes(int i, byte abyte0[]) throws SQLException {
		checkIndex(i);
		checkCursor();
		if (!isBinary(getMetaData().getColumnType(i))) {
			throw new SQLException("Data Type Mismatch");
		} else {
			getCurrentRow().setColumnObject(i, abyte0);
			return;
		}
	}

	public void updateBytes(String s, byte abyte0[]) throws SQLException {
		updateBytes(getColIdxByName(s), abyte0);
	}

	public void updateCharacterStream(int i, Reader reader, int j) throws SQLException {
		checkIndex(i);
		checkCursor();
		if (!isString(getMetaData().getColumnType(i)) && !isBinary(getMetaData().getColumnType(i)))
			throw new SQLException("Data Type Mismatch");
		char ac[] = new char[j];
		try {
			int k = 0;
			do
				k += reader.read(ac, k, j - k);
			while (k != -1);
		} catch (IOException _ex) {
			throw new SQLException("read failed for binaryStream");
		}
		String s = new String(ac);
		getCurrentRow().setColumnObject(i, s);
	}

	public void updateCharacterStream(String s, Reader reader, int i) throws SQLException {
		updateCharacterStream(getColIdxByName(s), reader, i);
	}

	public void updateDate(int i, Date date) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertTemporal(date, 91, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateDate(String s, Date date) throws SQLException {
		updateDate(getColIdxByName(s), date);
	}

	public void updateDouble(int i, double d) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertNumeric(new Double(d), 8, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateDouble(String s, double d) throws SQLException {
		updateDouble(getColIdxByName(s), d);
	}

	public void updateFloat(int i, float f) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertNumeric(new Float(f), 7, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateFloat(String s, float f) throws SQLException {
		updateFloat(getColIdxByName(s), f);
	}

	public void updateInt(int i, int j) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertNumeric(new Integer(j), 4, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateInt(String s, int i) throws SQLException {
		updateInt(getColIdxByName(s), i);
	}

	public void updateLong(int i, long l) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertNumeric(new Long(l), -5, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateLong(String s, long l) throws SQLException {
		updateLong(getColIdxByName(s), l);
	}

	public void updateNull(int i) throws SQLException {
		checkIndex(i);
		checkCursor();
		BaseRow baserow = getCurrentRow();
		baserow.setColumnObject(i, null);
	}

	public void updateNull(String s) throws SQLException {
		updateNull(getColIdxByName(s));
	}

	public void updateObject(int i, Object obj) throws SQLException {
		checkIndex(i);
		checkCursor();
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateObject(int i, Object obj, int j) throws SQLException {
		checkIndex(i);
		checkCursor();
		int k = getMetaData().getColumnType(i);
		if (k == 3 || k == 2)
			((BigDecimal) obj).setScale(j);
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateObject(String s, Object obj) throws SQLException {
		updateObject(getColIdxByName(s), obj);
	}

	public void updateObject(String s, Object obj, int i) throws SQLException {
		updateObject(getColIdxByName(s), obj, i);
	}

	public void updateRow() throws SQLException {
		if (onInsertRow) {
			throw new SQLException("updateRow called while on insert row");
		} else {
			((Row) getCurrentRow()).setUpdated();
			notifyRowChanged();
			return;
		}
	}

	public void updateShort(int i, short word0) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertNumeric(new Short(word0), 5, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateShort(String s, short word0) throws SQLException {
		updateShort(getColIdxByName(s), word0);
	}

	public void updateString(int i, String s) throws SQLException {
		checkIndex(i);
		checkCursor();
		getCurrentRow().setColumnObject(i, s);
	}

	public void updateString(String s, String s1) throws SQLException {
		updateString(getColIdxByName(s), s1);
	}

	public void updateTime(int i, Time time) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertTemporal(time, 92, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateTime(String s, Time time) throws SQLException {
		updateTime(getColIdxByName(s), time);
	}

	public void updateTimestamp(int i, Timestamp timestamp) throws SQLException {
		checkIndex(i);
		checkCursor();
		Object obj = convertTemporal(timestamp, 93, getMetaData().getColumnType(i));
		getCurrentRow().setColumnObject(i, obj);
	}

	public void updateTimestamp(String s, Timestamp timestamp) throws SQLException {
		updateTimestamp(getColIdxByName(s), timestamp);
	}

	public boolean wasNull() throws SQLException {
		return lastValueNull;
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setNull(String parameterName, int sqlType) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBoolean(String parameterName, boolean x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setByte(String parameterName, byte x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setShort(String parameterName, short x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInt(String parameterName, int x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLong(String parameterName, long x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFloat(String parameterName, float x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDouble(String parameterName, double x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setString(String parameterName, String x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBytes(String parameterName, byte[] x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setObject(String parameterName, Object x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlob(String parameterName, Blob x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setClob(String parameterName, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setClob(String parameterName, Clob x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setClob(String parameterName, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDate(String parameterName, Date x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTime(String parameterName, Time x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRowId(String parameterName, RowId x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNString(int parameterIndex, String value) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNString(String parameterName, String value) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNClob(String parameterName, NClob value) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNClob(String parameterName, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setURL(int parameterIndex, java.net.URL x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

}
