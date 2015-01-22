package net.ion.nsearcher.common;

import java.io.IOException;
import java.io.Reader;
import java.util.Date;

import net.ion.framework.util.DateUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.MyField.MyFieldType;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;

public abstract class FieldIndexingStrategy {

	
	public static final FieldIndexingStrategy DEFAULT = new FieldIndexingStrategy() {
		@Override
		public void save(Document doc, MyField myField, final Field ifield) {

			final String fieldName = IKeywordField.Field.reservedId(ifield.name()) ? ifield.name() :  StringUtil.lowerCase(ifield.name());
			
			if (myField.myFieldtype() == MyFieldType.Number){
				doc.add(new StringField(fieldName, ifield.stringValue(), Store.NO));
				doc.add(new LongField(fieldName, Long.parseLong(ifield.stringValue()), Store.NO));
			}
			if (myField.myFieldtype() == MyFieldType.Unknown && NumberUtil.isNumber(ifield.stringValue())){
				doc.add(new DoubleField(fieldName, Double.parseDouble(ifield.stringValue()), Store.NO));
			}
			if (myField.myFieldtype() == MyFieldType.Date){
				// new Date().getTime();
				Date date = DateUtil.stringToDate(ifield.stringValue(), "yyyyMMdd HHmmss") ;
				doc.add(new StringField(fieldName, StringUtil.substringBefore(ifield.stringValue(), " "), Store.NO)) ;
				doc.add(new StringField(fieldName, ifield.stringValue(), Store.NO)) ;
				doc.add(new LongField(fieldName, Long.parseLong(DateUtil.dateToString(date, "yyyyMMdd")), Store.NO)) ;
			}
			
			doc.add(new IndexableField() {
				@Override
				public String stringValue() {
					return ifield.stringValue();
				}
				
				@Override
				public Reader readerValue() {
					return ifield.readerValue();
				}
				
				@Override
				public Number numericValue() {
					return ifield.numericValue();
				}
				
				@Override
				public String name() {
					return fieldName ;
				}
				
				@Override
				public IndexableFieldType fieldType() {
					return ifield.fieldType();
				}
				
				@Override
				public float boost() {
					return ifield.boost();
				}
				
				@Override
				public BytesRef binaryValue() {
					return ifield.binaryValue();
				}

				public TokenStream tokenStream(Analyzer analyzer) throws IOException {
					return ifield.tokenStream(analyzer);
				}

//				public TokenStream tokenStream(Analyzer analyzer, TokenStream stream) throws IOException {
//					return ifield.tokenStream(analyzer, stream);
//				}
			});
		}
	};
	
	public  static FieldIndexingStrategy SENSITIVE_FIELDNAME = new FieldIndexingStrategy() {
		public void save(Document doc, MyField myField, Field ifield) {
			final String fieldName = IKeywordField.Field.reservedId(ifield.name()) ? ifield.name() :  StringUtil.lowerCase(ifield.name());
			
			if (myField.myFieldtype() == MyFieldType.Number){
				doc.add(new StringField(fieldName, ifield.stringValue(), Store.NO));
			}
			if (myField.myFieldtype() == MyFieldType.Unknown && NumberUtil.isNumber(ifield.stringValue())){
				doc.add(new LongField(fieldName, Long.parseLong(ifield.stringValue()), Store.NO));
			}
			if (myField.myFieldtype() == MyFieldType.Date){
				// new Date().getTime();
				Date date = DateUtil.stringToDate(ifield.stringValue(), "yyyyMMdd HHmmss") ;
				doc.add(new StringField(fieldName, StringUtil.substringBefore(ifield.stringValue(), " "), Store.NO)) ;
				doc.add(new StringField(fieldName, ifield.stringValue(), Store.NO)) ;
				doc.add(new LongField(fieldName, Long.parseLong(DateUtil.dateToString(date, "yyyyMMdd")), Store.NO)) ;
			}

			doc.add(ifield);
		}
	};
	
	public abstract void save(Document doc, MyField myField, Field ifield)  ;

	public static String makeSortFieldName(String fieldName) {
		return fieldName;
		// return (fieldName + MyField.SORT_POSTFIX);
	}

}
