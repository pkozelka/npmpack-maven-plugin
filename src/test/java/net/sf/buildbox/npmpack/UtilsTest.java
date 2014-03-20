package net.sf.buildbox.npmpack;

import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Petr Kozelka
 */
public class UtilsTest {
    @Test
    public void testHex() throws Exception {

    }

    @Test
    public void tgz() throws IOException {
        final TarArchiver archiver = new TarArchiver();
//        archiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG, "pack"));
        final TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
        tarCompressionMethod.setValue("gzip");
        archiver.setCompression(tarCompressionMethod);
        final File destFile = new File("xx.tgz").getAbsoluteFile();
        System.out.println("destFile = " + destFile);
        archiver.setDestFile(destFile);
        final File srcDir = new File("src");
        System.out.println("srcDir = " + srcDir);
        archiver.addDirectory(srcDir);
        archiver.createArchive();
    }
}
