package net.kozelka.npmpack.mojo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * @author Petr Kozelka
 */
@Mojo(name="bower", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true, threadSafe = true)
public class BowerMojo extends AbstractNpmpackMojo {
    /**
     * The executable to run as grunt. We expect that it resides under node_modules/.bin.
     */
    @Parameter(defaultValue = "bower,bower.cmd", required = true, property = "bower.executable")
    String bowerExecutables;

    /**
     * The execution is skipped it this is true.
     */
    @Parameter(defaultValue = "false", required = true, property = "bower.skip")
    boolean skip;


    /**
     * Create temporary bower configuration file <code>.bowerrc</code> , making bower gather the dependencies into
     * the ${project.build.directory}/${project.build.finalName}/res directory.
     *
     * This option is not effective if the file already exists.
     * @todo consider if this logic is ok; wouldn't simple overwriting be better?
     */
    @Parameter(defaultValue = "true")
    boolean bowerrcAutocreate;

    /**
     * Directory where bower will create its libraries unless existing .bowerrc is defined.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}/lib")
    File bowerOutputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final Commandline commandline = new Commandline();
            commandline.setWorkingDirectory(basedir);
            final File localBin = new File(node_modules, ".bin");
            commandline.setExecutable(new File(localBin, selectAlternative(bowerExecutables)).getAbsolutePath());
            commandline.addArguments(new String[]{"install"});
            if (skip) {
                getLog().info("Bower execution is skipped: " + CommandLineUtils.toString(commandline.getShellCommandline()));
            } else {
                // TODO: add the trick to avoid the need for calling bower in regular builds!

                // TODO: make sure that bower is in node_modules ?
                final File bowerrc = new File(basedir, ".bowerrc");
                final boolean temporary = !bowerrc.exists();
                if (temporary) {
                    FileUtils.fileWrite(bowerrc, "{\n    directory: \""+ jsonEscape(bowerOutputDirectory.getAbsolutePath()) +"\"\n}");
                    bowerrc.deleteOnExit();
                }
                executeCommandline("bower", commandline);
                if (temporary) {
                    bowerrc.delete();
                }
            }
        } catch (InterruptedException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (CommandLineException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private String jsonEscape(String s) {
        //TODO improve this
        return s.replace('\\', '/');
    }
}
