package net.sf.buildbox.npmpack;

import net.sf.buildbox.npmpack.mojo.AbstractNpmpackMojo;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Petr Kozelka
 */
public class UtilsTest {
    @Test
    public void todo() throws IOException {
        final String norm = AbstractNpmpackMojo.readPackageJson(new File("/home/pk/hp.com/consumption/ui/package.json"), true);
        System.out.println("norm = " + norm);
    }
}
