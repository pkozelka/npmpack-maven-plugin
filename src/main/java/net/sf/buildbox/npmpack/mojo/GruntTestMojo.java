package net.sf.buildbox.npmpack.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.CommandLineException;

/**
 * Executes grunt test sequence.
 * @author Petr Kozelka
 */
@Mojo(name="grunt-test", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true)
public class GruntTestMojo extends AbstractGruntMojo {

    @Parameter(defaultValue = "test", required = true)
    String testCommand;

    @Parameter(defaultValue = "false", required = true)
    boolean skipTests;



    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (skipTests) {
                getLog().info("Tests are skipped");
            } else {
                grunt(testCommand);
            }
        } catch (InterruptedException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (CommandLineException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
