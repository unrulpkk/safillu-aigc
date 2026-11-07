package cn.iocoder.yudao.module.infra.framework.file.core.client.s3;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
/** Authenticated storage envelope. Public object URLs reveal ciphertext only. */
public final class StorageContentCipher {
 private static final byte[] MAGIC={'Z','M','O','B','J',1,0,0};private static final SecureRandom RANDOM=new SecureRandom();
 private StorageContentCipher(){}
 private static Cipher cipher(int mode,String path,String key,byte[] iv)throws Exception{byte[] material=Base64.getDecoder().decode(FileCredentialResolver.resolve(key));if(material.length!=32)throw new IllegalArgumentException();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(mode,new SecretKeySpec(material,"AES"),new GCMParameterSpec(128,iv));cipher.updateAAD(path.getBytes(StandardCharsets.UTF_8));Arrays.fill(material,(byte)0);return cipher;}
 public static byte[] encrypt(byte[] data,String path,String key){try{byte[] iv=new byte[12];RANDOM.nextBytes(iv);byte[] encrypted=cipher(Cipher.ENCRYPT_MODE,path,key,iv).doFinal(data);byte[] out=new byte[MAGIC.length+iv.length+encrypted.length];System.arraycopy(MAGIC,0,out,0,MAGIC.length);System.arraycopy(iv,0,out,MAGIC.length,iv.length);System.arraycopy(encrypted,0,out,MAGIC.length+iv.length,encrypted.length);return out;}catch(Exception e){throw new IllegalStateException("Stored asset encryption failed");}}
 public static byte[] decrypt(byte[] data,String path,String key){try{if(data.length<MAGIC.length+12+16||!Arrays.equals(MAGIC,Arrays.copyOf(data,MAGIC.length)))throw new IllegalArgumentException();byte[] iv=Arrays.copyOfRange(data,MAGIC.length,MAGIC.length+12);return cipher(Cipher.DECRYPT_MODE,path,key,iv).doFinal(data,MAGIC.length+12,data.length-MAGIC.length-12);}catch(Exception e){throw new IllegalStateException("Stored asset decryption failed");}}
}
