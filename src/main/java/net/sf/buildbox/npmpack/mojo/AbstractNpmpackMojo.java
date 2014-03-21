package net.sf.buildbox.npmpack.mojo;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.util.List;

/**
 * Base class for this plugin's mojos
 *
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
     * Location of the local repository.
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    protected ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * Pointer to package.json file.
     * <p>Recommended use: do not change</p>
     */
    @Parameter(defaultValue = "package.json", required = true)
    File packageJson;

    /**
     * Pointer to node_modules directory.
     * <p>Recommended use: do not change if you wish to use npm-based tools from commandline as well. Otherwise it might be practical to change this to "${project.build.directory}/node_modules".</p>
     */
    @Parameter(defaultValue = "node_modules", required = true)
    File node_modules;

    @Parameter(defaultValue = "npm,npm.cmd", required = true, property = "npm.executable")
    String npmExecutables;

    final class MyStreamConsumer implements StreamConsumer {
        private final String prefix;
        private final boolean isErr;
        private int lineCount = 0;

        MyStreamConsumer(String prefix, boolean isErr) {
            this.prefix = prefix;
            this.isErr = isErr;
        }

        @Override
        public void consumeLine(String line) {
            final String msg = prefix + line;
            if (isErr) {
                getLog().warn(msg);
            } else {
                getLog().info(msg);
            }
            lineCount++;
        }

        public int getLineCount() {
            return lineCount;
        }
    }

    /**
     * Selects configured executable.
     * If it consists of two comma-separated strings, the first is used on non-windows OS and the second on Windows.
     * If there is only one string, it is always used as a whole.
     * @param configuredAlternatives one or two comma-separated alternatives by OS; non-windows comes first, windows is second
     * @return selected alternative
     * @throws CommandLineException -
     */
    protected String selectAlternative(String configuredAlternatives) throws CommandLineException {
        final String[] executables = configuredAlternatives.split(",");
        final String npmExecutable;
        if (executables.length == 1) {
            npmExecutable = executables[0];
            getLog().warn("Only one executable specified. You may have problems or other platforms: " + configuredAlternatives);
        } else if (executables.length == 0) {
            throw new CommandLineException("at least one executable variant must be specified");
        } else {
            npmExecutable = executables[Os.isFamily(Os.FAMILY_WINDOWS) ? 1 : 0];
        }
        return npmExecutable;
    }

    /**
     * Executes given commandline. The output of execution if prefixed with taskName;
     * square braces surround stdout lines, exclamation marks surround stderr lines.
     * Little stats is printed at the end.
     * @param taskName  logical task name to be used in prefix
     * @param commandline -
     * @throws CommandLineException
     * @throws InterruptedException
     */
    protected void executeCommandline(String taskName, Commandline commandline) throws CommandLineException, InterruptedException {
        getLog().info(String.format(" :::%s::: executing %s", taskName, CommandLineUtils.toString(commandline.getShellCommandline())));
        final MyStreamConsumer stdout = new MyStreamConsumer("   [" + taskName + "] ", false);
        final MyStreamConsumer stderr = new MyStreamConsumer("!" + taskName + "! ", true);
        final long startTime = System.currentTimeMillis();
        final int exitCode = CommandLineUtils.executeCommandLine(commandline, stdout, stderr);
        getLog().info(String.format(" :::%s::: finished - exitCode: %d, duration: %d millis, output lines: %d on stdout, %d on stderr",
                taskName,
                exitCode,
                System.currentTimeMillis() - startTime,
                stdout.getLineCount(),
                stderr.getLineCount()
                ));
        if (exitCode != 0) {
            throw new CommandLineException(commandline + " has failed with exitCode = " + exitCode);
        }
    }

    protected void npm(String taskName, String... arguments) throws InterruptedException, CommandLineException {
        final Commandline cl = new Commandline();
        cl.setExecutable(selectAlternative(npmExecutables));
        cl.addArguments(arguments);
        executeCommandline(taskName, cl);
    }

}
