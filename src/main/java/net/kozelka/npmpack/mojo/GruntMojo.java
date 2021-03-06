package net.kozelka.npmpack.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;

/**
 * Executes grunt in order to perform grunt-based build.
 *
 * @author Petr Kozelka
 */
@Mojo(name="grunt", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true)
public class GruntMojo extends AbstractNpmpackMojo {

    /**
     * The name to be used in prefix of this grunt command execution
     */
    @Parameter(defaultValue = "grunt", required = true)
    String taskName;


    /**
     * The executable to run as grunt. We expect that it resides under node_modules/.bin.
     */
    @Parameter(defaultValue = "grunt,grunt.cmd", required = true, property = "grunt.executable")
    String gruntExecutables;

    /**
     * The specific grunt command to execute. This is passed as the (whitespace separated) argument list to grunt.
     */
    @Parameter(defaultValue = "build", required = true)
    String gruntCommand;

    /**
     * The execution is skipped it this is true.
     */
    @Parameter(defaultValue = "false", required = true, property = "grunt.skip")
    boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final Commandline commandline = new Commandline();
            commandline.setWorkingDirectory(basedir);
            final File localBin = new File(node_modules, ".bin");
            commandline.setExecutable(new File(localBin, selectAlternative(gruntExecutables)).getAbsolutePath());
            commandline.addArguments(gruntCommand.split("\\s+"));
            if (skip) {
                getLog().info("Grunt execution is skipped: " + CommandLineUtils.toString(commandline.getShellCommandline()));
            } else {
                executeCommandline(taskName, commandline);
            }
        } catch (InterruptedException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (CommandLineException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
