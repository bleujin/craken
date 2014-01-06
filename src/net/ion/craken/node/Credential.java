package net.ion.craken.node;

import java.io.PrintStream;

import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.Analyzer;


public class Credential {

	public static final Credential ADMIN = new Credential("admin", "nimda") ;
	public static final Credential EMANON = new Credential("emanon", "emanon") ;
	
	private final String accessKey ;
	private String secretKey ;
	private final Analyzer analyzer;
	private PrintStream tracer = System.out ;

	public Credential(String accessKey, String secretKey){
		this(accessKey, secretKey, new MyKoreanAnalyzer()) ;
	}
	
	public Credential(String accessKey, String secretKey, Analyzer analyzer){
		this.accessKey = accessKey ;
		this.secretKey = secretKey ;
		this.analyzer = analyzer;
	}
	
	public String accessKey() {
		return accessKey;
	}
	
	public String secretKey(){
		return secretKey ;
	}
	
	public Analyzer analyzer(){
		return analyzer ; 
	}

	public Credential clearSecretKey() {
		this.secretKey = null ;
		return this;
	}

	public PrintStream tracer(){
		return tracer ;
	}
	
	public Credential tracer(PrintStream print) {
		this.tracer = print ;
		return this ;
	}
}
