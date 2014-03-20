package net.sf.buildbox.npmpack.mojo;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;

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
}
