package net.sf.buildbox.npmpack.mojo;

import net.sf.buildbox.npmpack.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.tar.TarUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Extracts archive of "node_modules" from the specified dependency
 */
@Mojo(name = "unpack")
public class UnpackMojo extends AbstractNpmpackMojo {

    @Parameter( defaultValue = "${project.groupId}.npmpack", readonly = true, required = true )
    private String binaryGroupId;

    @Parameter( defaultValue = "node_modules", readonly = true, required = true )
    private String binaryArtifactId;

    /**
     * Pointer to package.json file.
     */
    @Parameter( defaultValue = "package.json", readonly = true, required = true )
    private File packageJson;

    /**
     * Pointer to node_modules directory.
     */
    @Parameter( defaultValue = "node_modules", readonly = true, required = true )
    private File node_modules;

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
            final String packageJsonHash = Utils.fileHash(packageJson);
            getLog().info("NEW HASH: " + packageJsonHash);
            final File oldHashFile = new File(node_modules, "package.json.hash");
            final String oldHash = oldHashFile.exists() ? FileUtils.readFileToString(oldHashFile) : "__NONE__";
            getLog().info("OLD HASH: " + oldHash);
            if (! oldHash.equals(packageJsonHash)) {
                FileUtils.deleteDirectory(node_modules);
                // repo lookup
                getLog().info(String.format("Artifact %s:%s:%s", binaryGroupId, binaryArtifactId, packageJsonHash));
                final Artifact artifact = factory.createBuildArtifact(binaryGroupId, binaryArtifactId, packageJsonHash, "tgz");
                getLog().info(String.format("Trying to resolve artifact %s", artifact));
                resolver.resolve(artifact, remoteRepositories, localRepository); //TODO: on failure, generate one!

                // unpack
                getLog().info(String.format("Unpacking %s to %s", artifact.getFile(), node_modules));
                node_modules.mkdirs();
                final TarGZipUnArchiver unArchiver = new TarGZipUnArchiver();

                unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG, "unpack"));
                unArchiver.setSourceFile(artifact.getFile());
                unArchiver.setDestDirectory(node_modules);
                unArchiver.setUseJvmChmod(true);
                unArchiver.extract();

                // npm rebuild
                final Commandline npmr = new Commandline("npm rebuild");
                getLog().info(String.format("Executing %s", npmr));
                final Process process = npmr.execute();
                final int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new MojoExecutionException("npm rebuild has failed");
                }
                // save checksum
                getLog().info(String.format("Saving hash marker into %s", oldHashFile));
                FileUtils.writeStringToFile(oldHashFile, packageJsonHash);
                getLog().info("DONE");
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ArtifactNotFoundException e) {
            getLog().info("TODO: CREATE THE ARTIFACT NOW, by calling npm install!!!");
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (CommandLineException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

/* here is the ant fragment that we are reimplementing:
<fixcrlf file="package.json" destdir="target" eol="lf"/>
<checksum property="package.json.checksum" file="target/package.json" format="MD5SUM"/>
<echo>Depending on: ${npm-pack.hash}</echo>
<echo>Current hash: ${package.json.checksum}</echo>
<fail message="package.json has changed, please update the dependency (property npm-pack.hash in pom.xml file)">
    <condition>
        <not>
            <equals arg1="${npm-pack.hash}" arg2="${package.json.checksum}"/>
        </not>
    </condition>
</fail>
<delete includeemptydirs="true">
    <fileset dir="${basedir}" includes="node_modules/**" defaultexcludes="false"/>
</delete>
<mkdir dir="node_modules"/>
<!-- untar -->
<c:if>
    <os family="windows"/>
    <c:then>
        <untar src="${com.example.npmpack:node_modules:tgz}" dest="node_modules" compression="gzip"/>
    </c:then>
    <c:else>
        <!-- on *nixes, we use system tar in order to restore symlinks and executable flags -->
        <exec executable="tar" dir="node_modules" taskname="tar_zxf" failonerror="true">
            <arg value="zxf"/>
            <arg value="${com.example.npmpack:node_modules:tgz}"/>
            <arg value="-C"/>
            <arg value="${basedir}/node_modules"/>
        </exec>
    </c:else>
</c:if>

<exec executable="${npm.launcher}" failonerror="true" taskname="npm-rebuild" vmlauncher="${exec.vmlauncher}" dir="${basedir}">
    <arg value="rebuild"/>
    <redirector output="${project.build.directory}/npm-rebuild.log"/>
</exec>
*/
}
