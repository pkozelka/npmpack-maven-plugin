package net.sf.buildbox.npmpack.mojo;

import net.sf.buildbox.npmpack.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.cli.*;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Prepares the <code>node_modules</code> directory.
 * <p>If it exists, uses MD5 hash of the package.json file is used to determine if its contents should be updated from "npm install".</p>
 * <p>If it does not exist, unpacks it from an archive within maven repository. If that does not exist, uses "npm install" to produce it.</p>
 * @author Petr Kozelka
 */
@Mojo(name = "node_modules", defaultPhase = LifecyclePhase.COMPILE, requiresProject = true, threadSafe = true)
public class NodeModulesMojo extends AbstractNpmpackMojo {

    /**
     * The groupId under which to cache the npm artifacts in the repository.
     * <p>Recommended use: define once per whole project (or company), inside parent pom's pluginManagement.</p>
     */
    @Parameter( defaultValue = "${project.groupId}.npmpack", required = true )
    String binaryGroupId;

    /**
     * The artifactId to use for caching in maven repositories.
     * <p>Recommended use: do not change</p>
     */
    @Parameter( defaultValue = "node_modules", required = true )
    String binaryArtifactId;

    @Parameter( defaultValue = "${project.build.directory}", required = true)
    File workdir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        /* outline:
        - compute checksum of "normalized" package.json (ignore whitespace, eols etc)
        - if it equals to current node_modules/package.json.md5, DONE
        - look it up in repo
            - if NOT FOUND, create one
        - unpack it into node_modules
        - invoke "npm rebuild" to adjust symlinks, executable flags etc
        */

        try {
            final String packageJsonHash = Utils.md5sumNormalized(packageJson);
            final File oldHashFile = new File(node_modules, "package.json.hash");
            final String oldHash = oldHashFile.exists() ? FileUtils.readFileToString(oldHashFile) : "__NONE__";
            if (oldHash.equals(packageJsonHash)) {
                getLog().info(String.format("No change in %s (#%s), keeping %s", packageJson, packageJsonHash, node_modules));
            } else {
                getLog().info(String.format("Differs from previous hash: %s, updating content of %s", oldHash, node_modules));

                // repository lookup
                getLog().info(String.format("Artifact %s:%s:%s", binaryGroupId, binaryArtifactId, packageJsonHash));
                final Artifact artifact = factory.createBuildArtifact(binaryGroupId, binaryArtifactId, packageJsonHash, "tgz");
                getLog().info(String.format("Trying to resolve artifact %s", artifact));

                getLog().info(String.format("Deleting %s", node_modules));
                FileUtils.deleteDirectory(node_modules); //TODO: just move to target, so that user has a way back when no net is reachable
                try {
                    resolver.resolveAlways(artifact, remoteRepositories, localRepository);
                    unpack(artifact.getFile());
                } catch (ArtifactNotFoundException e) {
                    pack(artifact);
                }

                // save checksum
                getLog().info(String.format("Saving hash marker into %s", oldHashFile));
                FileUtils.writeStringToFile(oldHashFile, packageJsonHash);
                getLog().info(String.format("Directory %s has been successfully recreated", node_modules));
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (CommandLineException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void unpack(File binArtifactFile) throws CommandLineException, InterruptedException, MojoExecutionException {
        // unpack
        getLog().info(String.format("Unpacking %s to %s", binArtifactFile, node_modules));
        node_modules.mkdirs();
        final TarGZipUnArchiver unArchiver = new TarGZipUnArchiver();
        final int logLevel = getLog().isDebugEnabled() ? Logger.LEVEL_DEBUG : Logger.LEVEL_INFO;
        unArchiver.enableLogging(new ConsoleLogger(logLevel, "unpack"));
        unArchiver.setSourceFile(binArtifactFile);
        unArchiver.setDestDirectory(node_modules);
        unArchiver.setUseJvmChmod(true);
        unArchiver.extract();

        // npm rebuild
        npm("npm_rebuild", "rebuild");
    }

    private void pack(Artifact artifact) throws MojoExecutionException, IOException, CommandLineException, InterruptedException {
        npm("npm_install", "install");

        final File archiveFile = new File(localRepository.getLayout().pathOf(artifact));
        final File archiveFileTmp = new File(workdir, archiveFile.getName());

        FileUtils.copyFile(packageJson, new File(node_modules, packageJson.getName()));

        getLog().info(String.format("Creating binary artifact %s in %s", artifact, archiveFile));
        final TarArchiver archiver = new TarArchiver();
        final TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
        tarCompressionMethod.setValue("gzip");
        archiver.setCompression(tarCompressionMethod);
        archiver.setDestFile(archiveFileTmp);
        //NOTE: .bin dirs will be recreated by npm rebuild; that makes the archive platform independent
        archiver.addDirectory(node_modules, null, new String[]{"**/.bin/**"});
        archiver.createArchive();

        final Artifact pomArtifact = factory.createBuildArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "pom");
        final File pomFile = new File(localRepository.getLayout().pathOf(pomArtifact));
        getLog().info(String.format("Generating pom in %s", pomFile));
        FileUtils.writeStringToFile(pomFile, String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>%s</groupId>\n" +
                "  <artifactId>%s</artifactId>\n" +
                "  <version>%s</version>\n" +
                "  <packaging>tgz</packaging>\n" +
                "  <description>generated by npmpack-maven-plugin</description>\n" +
                "</project>\n",
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion()));
        getLog().info(String.format("Moving artifact to local repository: %s", archiveFile));
        FileUtils.moveFile(archiveFileTmp, archiveFile);
        // TODO: publish into nexus if desired
    }
}
