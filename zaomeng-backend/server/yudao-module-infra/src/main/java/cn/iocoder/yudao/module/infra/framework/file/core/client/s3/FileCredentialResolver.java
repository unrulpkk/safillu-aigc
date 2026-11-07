package cn.iocoder.yudao.module.infra.framework.file.core.client.s3;
import java.nio.file.*;
/** Allows deployment secrets without storing storage credentials in the database. */
public final class FileCredentialResolver {
 private FileCredentialResolver(){}
 public static String resolve(String value){if(value==null||!value.startsWith("file:"))return value;try{Path root=Path.of("/run/secrets/storage").toRealPath(),file=Path.of(value.substring(5)).normalize().toRealPath();if(!file.startsWith(root)||!Files.isRegularFile(file)||Files.size(file)>4096)throw new IllegalArgumentException();String secret=Files.readString(file).trim();if(secret.isEmpty())throw new IllegalArgumentException();return secret;}catch(Exception e){throw new IllegalStateException("Storage credential reference is unavailable");}}
}
