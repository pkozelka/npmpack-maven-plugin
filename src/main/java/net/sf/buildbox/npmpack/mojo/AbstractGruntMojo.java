package net.sf.buildbox.npmpack.mojo;

import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;

/**
 * @author Petr Kozelka
 */
public abstract class AbstractGruntMojo extends AbstractNpmpackMojo {

    @Parameter(defaultValue = "grunt,grunt.cmd", required = true, property = "grunt.executable")
    String gruntExecutables;

    @Parameter(defaultValue = "--no-color", required = true)
    String gruntArguments;

    protected void grunt(String taskName, String... arguments) throws InterruptedException, CommandLineException {
        final Commandline commandline = new Commandline();
        commandline.setExecutable(new File(node_modules, selectAlternative(gruntExecutables)).getAbsolutePath());
        final Arg arg = new Commandline.Argument();
        arg.setLine(gruntArguments);
        commandline.addArg(arg);
        commandline.addArguments(arguments);
        executeCommandline(taskName, commandline);
    }
}
