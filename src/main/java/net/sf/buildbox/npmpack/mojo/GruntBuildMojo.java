package net.sf.buildbox.npmpack.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.CommandLineException;

/**
 * Executes grunt build sequence.
 * @author Petr Kozelka
 */
@Mojo(name="grunt-build", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true)
public class GruntBuildMojo extends AbstractGruntMojo {

    @Parameter(defaultValue = "build", required = true)
    String buildCommand;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            grunt("grunt-build", buildCommand.split("\\s+"));
        } catch (InterruptedException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (CommandLineException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
