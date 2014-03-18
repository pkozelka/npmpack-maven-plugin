package net.sf.buildbox.maven.npmpack;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Prepares archive of fresh "node_modules" in the local repository.
 * TODO: ???Also updates the specified version property in the hosting module's <code>pom.xml</code>.
 * <p>Attention: accesses network outside Maven repositories!</p>
 * @goal pack
 */
public class PackMojo extends AbstractNpmpackMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
