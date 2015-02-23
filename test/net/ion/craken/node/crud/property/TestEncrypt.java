package net.ion.craken.node.crud.property;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestEncrypt extends TestBaseCrud{

	
	public void testCiper() throws Exception {
		byte[] keyBytes = "40674244".getBytes();
		byte[] ivBytes = "@1B2c3D4".getBytes();

		// wrap key data in Key/IV specs to pass to cipher
		SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
		IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
		// create the cipher with the algorithm you choose see javadoc for Cipher class for more info, e.g.
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		
		
		byte[] input = "administrator".getBytes("UTF-8") ;
		
		cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
		
		byte[] encrypted= new byte[cipher.getOutputSize(input.length)];
		
		int enc_len = cipher.update(input, 0, input.length, encrypted, 0);
		enc_len += cipher.doFinal(encrypted, enc_len);
		Debug.line(encrypted, enc_len);
		
		cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
		byte[] decrypted = new byte[cipher.getOutputSize(enc_len)];
		int dec_len = cipher.update(encrypted, 0, enc_len, decrypted, 0);
		dec_len += cipher.doFinal(decrypted, dec_len);
		
		Debug.line(decrypted, dec_len, new String(decrypted, "UTF-8").trim());
	}
	
	
	public void testEncrypt() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("id", "bleujin").encrypt("pwd", "1234") ;
				return null;
			}
		}) ;

		ReadNode found = session.pathBy("/emp/bleujin");
		Debug.line(found.property("pwd").asString()) ;
		assertEquals(true, found.isMatch("pwd", "1234")) ;
	}
}
