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
public class CK_WTLS_MASTER_KEY_DERIVE_PARAMS extends Structure {
	/** C type : CK_MECHANISM_TYPE */
	public long DigestMechanism;
	public long getDigestMechanism() {
		return DigestMechanism;
	}
	public void setDigestMechanism(long DigestMechanism) {
		this.DigestMechanism = DigestMechanism;
	}
	/** C type : CK_WTLS_RANDOM_DATA */
	public CK_WTLS_RANDOM_DATA RandomInfo;
	public CK_WTLS_RANDOM_DATA getRandomInfo() {
		return RandomInfo;
	}
	public void setRandomInfo(CK_WTLS_RANDOM_DATA RandomInfo) {
		this.RandomInfo = RandomInfo;
	}
	/** C type : CK_BYTE_PTR */
	public Pointer pVersion;
	public Pointer getPVersion() {
		return pVersion;
	}
	public void setPVersion(Pointer pVersion) {
		this.pVersion = pVersion;
	}
	public CK_WTLS_MASTER_KEY_DERIVE_PARAMS() {
		super();
	}
	 protected List<String> getFieldOrder() {
		return Arrays.asList("DigestMechanism", "RandomInfo", "pVersion");
	}
	/**
	 * @param DigestMechanism C type : CK_MECHANISM_TYPE<br>
	 * @param RandomInfo C type : CK_WTLS_RANDOM_DATA<br>
	 * @param pVersion C type : CK_BYTE_PTR
	 */
	public CK_WTLS_MASTER_KEY_DERIVE_PARAMS(long DigestMechanism, CK_WTLS_RANDOM_DATA RandomInfo, Pointer pVersion) {
		super();
		this.DigestMechanism = DigestMechanism;
		this.RandomInfo = RandomInfo;
		this.pVersion = pVersion;
	}
	public CK_WTLS_MASTER_KEY_DERIVE_PARAMS(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends CK_WTLS_MASTER_KEY_DERIVE_PARAMS implements Structure.ByReference {
		
	};
	public static class ByValue extends CK_WTLS_MASTER_KEY_DERIVE_PARAMS implements Structure.ByValue {
		
	};
}
