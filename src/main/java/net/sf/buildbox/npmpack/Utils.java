package net.sf.buildbox.npmpack;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.digest.Hex;
import org.codehaus.plexus.digest.Md5Digester;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Petr Kozelka
 */
public class Utils {
    public static String fileHash(File file) throws IOException, NoSuchAlgorithmException {
/*
        final String contents = FileUtils.readFileToString(file);
        final String normalized = "AHOJ"; //contents.replace("a","\n"); // todo: improve the normalization
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] normalizedBytes = normalized.getBytes();
        final InputStream is = new ByteArrayInputStream(normalizedBytes);
        final DigestInputStream dis = new DigestInputStream(is, md);
        dis.on(true);
        try {
            final byte[] digest = md.digest();
            return Hex.encode(digest);
        } finally {
            dis.close();
        }
*/
        return MD5Checksum.getMD5Checksum(file.getAbsolutePath());
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.out.println(fileHash(new File(args[0])));
    }
}
