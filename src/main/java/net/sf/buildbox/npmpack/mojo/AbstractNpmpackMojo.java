package net.sf.buildbox.npmpack.mojo;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.util.List;

/**
 * Base class for this plugin's mojos
 * @author Petr Kozelka
 */
public abstract class AbstractNpmpackMojo extends AbstractMojo {
    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected ArtifactResolver resolver;

    /**
     * Artifact collector, needed to resolve dependencies.
     */
    @Component( role = ArtifactCollector.class )
    protected ArtifactCollector artifactCollector;

    /**
     *
     */
    @Component( role = ArtifactMetadataSource.class, hint = "maven" )
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * Location of the local repository.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    protected ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true )
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * Pointer to package.json file.
     * <p>Recommended use: do not change</p>
     */
    @Parameter( defaultValue = "package.json", required = true )
    File packageJson;

    /**
     * Pointer to node_modules directory.
     * <p>Recommended use: do not change if you wish to use npm-based tools from commandline as well. Otherwise it might be practical to change this to "${project.build.directory}/node_modules".</p>
     */
    @Parameter( defaultValue = "node_modules", required = true )
    File node_modules;

    private final StreamConsumer STDOUT = new StreamConsumer() {
        @Override
        public void consumeLine(String line) {
            getLog().info(". " + line);
        }
    };

    private final StreamConsumer STDERR = new StreamConsumer() {
        @Override
        public void consumeLine(String line) {
            getLog().warn("! " + line);
        }
    };

    private void executeCommandline(Commandline cl) throws CommandLineException, InterruptedException {
        getLog().info(String.format("Executing %s", cl));
        final int exitCode = CommandLineUtils.executeCommandLine(cl, STDOUT, STDERR);
        if (exitCode != 0) {
            throw new CommandLineException(cl + " has failed with exitCode = " + exitCode);
        }
    }

    protected void npm(String ... arguments) throws InterruptedException, CommandLineException {
        final Commandline cl = new Commandline();
        cl.setExecutable("npm");
        cl.addArguments(arguments);
        executeCommandline(cl);
    }
}
