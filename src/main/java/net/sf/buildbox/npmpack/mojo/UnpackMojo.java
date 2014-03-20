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
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Extracts archive of "node_modules" from the specified dependency.
 * <p>MD5 hash of the package.json file is used to determine if its contents should be updated from "npm install"</p>
 * @author Petr Kozelka
 */
@Mojo(name = "unpack", defaultPhase = LifecyclePhase.COMPILE, requiresProject = true, threadSafe = true)
public class UnpackMojo extends AbstractNpmpackMojo {

    /**
     * The groupId under which to cache the npm artifacts in the repository.
     * <p>Recommended use: define once per whole project (or company), inside parent pom's pluginManagement.</p>
     */
    @Parameter( defaultValue = "${project.groupId}.npmpack", readonly = true, required = true )
    String binaryGroupId;

    /**
     * The artifactId to use for caching in maven repositories.
     * <p>Recommended use: do not change</p>
     */
    @Parameter( defaultValue = "node_modules", readonly = true, required = true )
    String binaryArtifactId;

    /**
     * Pointer to package.json file.
     * <p>Recommended use: do not change</p>
     */
    @Parameter( defaultValue = "package.json", readonly = true, required = true )
    File packageJson;

    /**
     * Pointer to node_modules directory.
     * <p>Recommended use: do not change if you wish to use npm-based tools from commandline as well. Otherwise it might be practical to change this to "${project.build.directory}/node_modules".</p>
     */
    @Parameter( defaultValue = "node_modules", readonly = true, required = true )
    File node_modules;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        /* outline:
        - compute checksum of "normalized" package.json (ignore whitespace, eols etc)
        - if it equals to current node_modules/package.json.md5, DONE
        - look it up in repo
            - TODO: if NOT FOUND, create one
        - unpack it into node_modules
        - invoke "npm rebuild" to adjust symlinks, executable flags etc
        */

        try {
            final String packageJsonHash = Utils.md5sumNormalized(packageJson);
            getLog().info(String.format("MD5 hash of normalized %s: %s", packageJson, packageJsonHash));
            final File oldHashFile = new File(node_modules, "package.json.hash");
            final String oldHash = oldHashFile.exists() ? FileUtils.readFileToString(oldHashFile) : "__NONE__";
            if (oldHash.equals(packageJsonHash)) {
                getLog().info("The normalized hash did not change, assuming that content needs no changes");
            } else {
                getLog().info(String.format("Differs from previous hash: %s, updating content of %s", oldHash, node_modules));
                getLog().info(String.format("Deleting %s", node_modules));
                FileUtils.deleteDirectory(node_modules);

                // repository lookup
                getLog().info(String.format("Artifact %s:%s:%s", binaryGroupId, binaryArtifactId, packageJsonHash));
                final Artifact artifact = factory.createBuildArtifact(binaryGroupId, binaryArtifactId, packageJsonHash, "tgz");
                getLog().info(String.format("Trying to resolve artifact %s", artifact));

                try {
                    resolver.resolveAlways(artifact, remoteRepositories, localRepository); //TODO: on failure, generate one!
                } catch (ArtifactNotFoundException e) {
                    pack();
                }

                // unpack
                getLog().info(String.format("Unpacking %s to %s", artifact.getFile(), node_modules));
                node_modules.mkdirs();
                final TarGZipUnArchiver unArchiver = new TarGZipUnArchiver();
                final int logLevel = getLog().isDebugEnabled() ? Logger.LEVEL_DEBUG : Logger.LEVEL_INFO;
                unArchiver.enableLogging(new ConsoleLogger(logLevel, "unpack"));
                unArchiver.setSourceFile(artifact.getFile());
                unArchiver.setDestDirectory(node_modules);
                unArchiver.setUseJvmChmod(true);
                unArchiver.extract();

                // npm rebuild
                final Commandline npmRebuild = new Commandline("npm rebuild");
                getLog().info(String.format("Executing %s", npmRebuild));
                final Process process = npmRebuild.execute();
                final int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new MojoExecutionException(npmRebuild + " has failed");
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

    private void pack() throws MojoExecutionException {
        getLog().info("TODO: CREATE THE ARTIFACT NOW, by calling npm install!!!");
        throw new MojoExecutionException("NOT IMPLEMENTED YET");
    }
}
