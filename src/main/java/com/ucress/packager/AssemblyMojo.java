package com.ucress.packager;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.mojos.AbstractAssemblyMojo;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;
import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.util.Iterator;

@Mojo(name = "assembly", defaultPhase = LifecyclePhase.PACKAGE)
public class AssemblyMojo extends AbstractAssemblyMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    @Parameter(property = "format", defaultValue = "zip")
    private String format;

    @Component
    private MavenProjectHelper projectHelper;

    @Component
    private DependencyResolver dependencyResolver;

    @Component
    private AssemblyArchiver assemblyArchiver;

    private static final String BIN_DIR_NAME = "bin";

    private static final String CONF_DIR_NAME = "conf";

    private static final String LIB_DIR_NAME = "lib";

    private static final String BOOTSTRAP_JAR_NAME = "bootstrap.jar";

    private static final String ASSEMBLY_RESOURCES_DIR_NAME = "assembly-resources";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
        coordinate.setGroupId("com.ucress");
        coordinate.setArtifactId("ucress-loader");
        coordinate.setVersion("1.0.0");
        try {
            Iterable<ArtifactResult> results = dependencyResolver.resolveDependencies(buildingRequest, coordinate, null);
            Iterator<ArtifactResult> it = results.iterator();
            while (it.hasNext()) {
                ArtifactResult ar = it.next();
                if (ar.getArtifact().getGroupId().equals(coordinate.getGroupId())
                        && ar.getArtifact().getArtifactId().equals(coordinate.getArtifactId())) {
                    File destFile = new File(project.getBuild().getDirectory(), BOOTSTRAP_JAR_NAME);
                    FileUtils.copyFile(ar.getArtifact().getFile(), destFile);
                    break;
                }
            }
        } catch (DependencyResolverException e) {
            getLog().error(e.getMessage());
        } catch (IOException e) {
            getLog().error(e.getMessage());
        }
        String[] files = new String[]{"start.bat", "start.sh"};
        File resources = new File(project.getBuild().getDirectory(), ASSEMBLY_RESOURCES_DIR_NAME);
        if (!resources.exists()) {
            resources.mkdirs();
        }
        for (String file : files) {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = this.getClass().getResourceAsStream("/" + file);
                os = new FileOutputStream(new File(resources, file));
                int len = 0;
                byte[] bytes = new byte[1024];
                while ((len = is.read(bytes)) != -1) {
                    os.write(bytes, 0, len);
                }
            } catch (IOException e) {
                getLog().error(e.getMessage());
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        getLog().error(e.getMessage());
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        getLog().error(e.getMessage());
                    }
                }
            }
        }

        Assembly assembly = new Assembly();
        assembly.setId(project.getArtifactId());

        FileItem bootstrap = new FileItem();
        bootstrap.setSource(project.getBuild().getDirectory() + File.separator + BOOTSTRAP_JAR_NAME);
        bootstrap.setOutputDirectory(BIN_DIR_NAME);
        assembly.addFile(bootstrap);

        FileSet bats = new FileSet();
        bats.setDirectory(resources.getAbsolutePath());
        bats.setOutputDirectory(BIN_DIR_NAME);
        bats.addInclude("*.bat");
        bats.setLineEnding("dos");
        bats.setFiltered(true);
        assembly.addFileSet(bats);

        FileSet shs = new FileSet();
        shs.setDirectory(resources.getAbsolutePath());
        shs.setOutputDirectory(BIN_DIR_NAME);
        shs.addInclude("*.sh");
        shs.setFileMode("0744");
        shs.setLineEnding("unix");
        shs.setFiltered(true);
        assembly.addFileSet(shs);

        FileSet confs = new FileSet();
        confs.setDirectory("src/main/resources");
        confs.setOutputDirectory(CONF_DIR_NAME);
        assembly.addFileSet(confs);

        DependencySet lib = new DependencySet();
        lib.setOutputDirectory(LIB_DIR_NAME);
        assembly.addDependencySet(lib);

        try {
            File destFile = assemblyArchiver
                    .createArchive(assembly, project.getBuild().getFinalName(), format, this, true, null);
            projectHelper.attachArtifact(project, format, null, destFile);
        } catch (ArchiveCreationException e) {
            getLog().error(e.getMessage());
        } catch (AssemblyFormattingException e) {
            getLog().error(e.getMessage());
        } catch (InvalidAssemblerConfigurationException e) {
            getLog().error(e.getMessage());
        }
    }

    @Override
    public MavenProject getProject() {
        return project;
    }
}
