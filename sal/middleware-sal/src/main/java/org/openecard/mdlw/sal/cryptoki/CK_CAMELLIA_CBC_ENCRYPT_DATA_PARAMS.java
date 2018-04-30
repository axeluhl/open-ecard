package org.openecard.mdlw.sal.cryptoki;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : pkcs11_v2.40/pkcs11t.h</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class CK_CAMELLIA_CBC_ENCRYPT_DATA_PARAMS extends Structure {
	/** C type : CK_BYTE[16] */
	public byte[] iv = new byte[16];
	public byte[] getIv() {
		return iv;
	}
	public void setIv(byte iv[]) {
		this.iv = iv;
	}
	/** C type : CK_BYTE_PTR */
	public Pointer pData;
	public Pointer getPData() {
		return pData;
	}
	public void setPData(Pointer pData) {
		this.pData = pData;
	}
	/** C type : CK_ULONG */
	public long length;
	public long getLength() {
		return length;
	}
	public void setLength(long length) {
		this.length = length;
	}
	public CK_CAMELLIA_CBC_ENCRYPT_DATA_PARAMS() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("iv", "pData", "length");
	}
	/**
	 * @param iv C type : CK_BYTE[16]<br>
	 * @param pData C type : CK_BYTE_PTR<br>
	 * @param length C type : CK_ULONG
	 */
	public CK_CAMELLIA_CBC_ENCRYPT_DATA_PARAMS(byte iv[], Pointer pData, long length) {
		super();
		if ((iv.length != this.iv.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.iv = iv;
		this.pData = pData;
		this.length = length;
	}
	public CK_CAMELLIA_CBC_ENCRYPT_DATA_PARAMS(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends CK_CAMELLIA_CBC_ENCRYPT_DATA_PARAMS implements Structure.ByReference {
		
	};
	public static class ByValue extends CK_CAMELLIA_CBC_ENCRYPT_DATA_PARAMS implements Structure.ByValue {
		
	};
}
