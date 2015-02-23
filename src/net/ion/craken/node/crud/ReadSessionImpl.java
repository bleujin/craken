package net.ion.craken.node.crud;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.ion.craken.node.AbstractReadSession;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;
import net.ion.framework.file.HexUtil;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;

public class ReadSessionImpl extends AbstractReadSession {

	private Analyzer queryAnalyzer;
	private Map<String, Object> attrs = MapUtil.newMap();

	public ReadSessionImpl(Credential credential, Workspace workspace, Analyzer queryAnalyzer) {
		super(credential, workspace);
		this.queryAnalyzer = queryAnalyzer;

		attribute(EncryptKeyBytes, "40674244".getBytes());
		attribute(EncryptIvBytes, "@1B2c3D4".getBytes());
	}

	@Override
	public ReadSessionImpl awaitListener() throws InterruptedException, ExecutionException {

		return this;
	}

	@Override
	public Searcher newSearcher() throws IOException {
		return central().newSearcher();
	}

	@Override
	public <T> T indexInfo(IndexInfoHandler<T> indexInfo) {
		return indexInfo.handle(this, central().newReader());
	}

	public Central central() {
		return workspace().central();
	}

	@Override
	public ChildQueryRequest queryRequest(String query) throws IOException, ParseException {
		return root().childQuery(query, true);
	}

	@Override
	public Analyzer queryAnalyzer() {
		return queryAnalyzer;
	}

	@Override
	public ReadSession queryAnayzler(Analyzer analyzer) {
		this.queryAnalyzer = analyzer;
		return this;
	}

	@Override
	public void attribute(String key, Object value) {
		attrs.put(key, value);
	}

	public Object attribute(String key) {
		return attrs.get(key);
	}

	public String encrypt(String value) throws IOException {
		try {
			byte[] keyBytes = (byte[]) attribute(ReadSession.EncryptKeyBytes);
			byte[] ivBytes = (byte[]) attribute(ReadSession.EncryptIvBytes);

			SecretKeySpec skey = new SecretKeySpec(keyBytes, "DES"); // wrap key data in Key/IV specs to pass to cipher
			IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding"); // create the cipher with the algorithm you choose see javadoc for Cipher class for more info, e.g.

			byte[] input = value.getBytes("UTF-8");

			cipher.init(Cipher.ENCRYPT_MODE, skey, ivSpec);
			byte[] encrypted = new byte[cipher.getOutputSize(input.length)];

			int enc_len = cipher.update(input, 0, input.length, encrypted, 0);
			enc_len += cipher.doFinal(encrypted, enc_len);
			
			return HexUtil.toHex(encrypted) ;
		} catch (NoSuchAlgorithmException ex) {
			throw new IOException(ex);
		} catch (ShortBufferException e) {
			throw new IOException(e);
		} catch (IllegalBlockSizeException e) {
			throw new IOException(e);
		} catch (BadPaddingException e) {
			throw new IOException(e);
		} catch (NoSuchPaddingException e) {
			throw new IOException(e);
		} catch (InvalidKeyException e) {
			throw new IOException(e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new IOException(e);
		}
	}
	
}
