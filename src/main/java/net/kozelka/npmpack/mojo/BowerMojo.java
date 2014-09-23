package net.kozelka.npmpack.mojo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
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
     * The execution is skipped if this is true.
     */
    @Parameter(defaultValue = "false", required = true, property = "bower.skip")
    boolean skip;

    /**
     * When true, manual (or maintenance) mode is assumed. In this mode, the plugin is allowed to download artifacts from non-Maven repositories, like NPM registry or from Bower.
     * Otherwise it fails if the needed artifact is not available in maven repository.
     */
    @Parameter(defaultValue = "false", required = true, property = "npmpack.manual")
    boolean manualMode;


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
                return;
            }

            final File bowerrcFile = new File(basedir, ".bowerrc");
            final boolean temporary = !bowerrcFile.exists();
            if (! temporary) {
                final JsonObject bowerrc = readBowerRc(bowerrcFile);
                bowerOutputDirectory = new File(bowerrc.get("directory").getAsString());
                getLog().info(".bowerrc/directory=" + bowerOutputDirectory);
            }
            // TODO: check that current content of bowerOutputDirectory matches bower.json (use checksum of anonymized canonicalized clone)
            // TODO: add the trick to avoid the need for calling bower in regular builds!
            if (manualMode) {
                // TODO: make sure that bower is in node_modules ? Like, by using "npm install bower" ?
                if (temporary) {
                    FileUtils.fileWrite(bowerrcFile, "{\n    directory: \""+ jsonEscape(bowerOutputDirectory.getAbsolutePath()) +"\"\n}");
                    bowerrcFile.deleteOnExit();
                }
                executeCommandline("bower", commandline);
                if (temporary) {
                    bowerrcFile.delete();
                }
                //TODO: package the files in bowerOutputDirectory and put in local/remote repo
            } else {
                throw new MojoExecutionException("Bower artifact not found, cannot create one in regular mode. Add '-DmanualMode' to Maven commandline to prepare one.");
            }
        } catch (InterruptedException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (CommandLineException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private JsonObject readBowerRc(File bowerrcFile) throws IOException {
        final String text = FileUtils.fileRead(bowerrcFile);
        // always call this, to at least validate that it is correct json format
        final JsonParser parser = new JsonParser();
        return (JsonObject) parser.parse(text);
    }

    private String jsonEscape(String s) {
        //TODO improve this
        return s.replace('\\', '/');
    }
}
