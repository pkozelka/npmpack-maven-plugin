package net.sf.buildbox.npmpack;


import org.codehaus.plexus.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Petr Kozelka
 */
public class Utils {

    /**
     * Computes MD5 checksum of a file, but first replaces any EOL characters with LF, in order to produce the same result on any OS.
     * @param file the file to compute
     * @return checksum as lowercase hex string
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String md5sumNormalized(File file) throws IOException, NoSuchAlgorithmException {
        final String contents = FileUtils.fileRead(file);
        final String normalized = contents.replace("\r\n","\n").replace("\r", "\n");
        final byte[] normalizedBytes = normalized.getBytes();
        final InputStream is = new ByteArrayInputStream(normalizedBytes);
        return hex(md5sum(is));
    }

    /**
     * inspired by http://stackoverflow.com/a/304275/455449
     */
    public static byte[] md5sum(InputStream fis) throws IOException, NoSuchAlgorithmException {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[] buffer = new byte[1024];
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    md5.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            return md5.digest();
        } finally {
            fis.close();
        }
    }

    public static String hex(byte[] bytes) throws IOException, NoSuchAlgorithmException {
        final StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(Integer.toHexString((b & 0xff) + 0x100).substring(1));
        }
        return result.toString();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.out.println(md5sumNormalized(new File(args[0])));
    }
}
