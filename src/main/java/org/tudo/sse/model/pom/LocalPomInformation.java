package org.tudo.sse.model.pom;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.tudo.sse.model.*;
import org.tudo.sse.resolution.PomResolutionException;
import org.tudo.sse.resolution.PomResolver;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class can be used to perform pom resolution on a local pom file.
 */
public class LocalPomInformation extends PomInformation {

    private Path pomPath;

    /**
     * Creates a new LocalPomInformation object for a POM file located at the given path.
     *
     * @param path The Path object identifying a pom file
     * @param resolver The PomResolver to use for resolution
     * @throws FileNotFoundException If the given Path does not exist
     */
    public LocalPomInformation(String path, PomResolver resolver) throws FileNotFoundException {
        this(new FileInputStream(path), resolver);
        this.pomPath = Paths.get(path);
    }

    /**
     * Creates a new LocalPomInformation object for a given POM file
     * @param localPom The file object identifying the local POM file
     * @param resolver The PomResolver to use for resolution
     * @throws FileNotFoundException If the given File does not exist
     */
    public LocalPomInformation(File localPom, PomResolver resolver) throws FileNotFoundException{
        this(new FileInputStream(localPom), resolver);
        this.pomPath = localPom.toPath();
    }

    /**
     * Creates a new LocalPomInformation object for a given InputStream (representing a text file).
     * @param localPom An InputStream containing the POM file contents
     * @param resolver The PomResolver to use for resolution
     */
    public LocalPomInformation(InputStream localPom, PomResolver resolver) {
        super();
        try {
            resolveLocalFile(resolver, localPom);
            resolveParentAndImport(resolver);
            resolveDependencies(resolver);
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the path to this local pom File, if available. If this Information object was instantiated from an
     * InputStream, i.e. no path information is available, null will be returned.
     *
     * @return The Path to this local POM file, if available - else null.
     */
    public Path getPomPath(){
        return pomPath;
    }

    /**
     * This resolves a local pom file, using the same methods as resolving pom from the maven central repository
     * @param resolver a pomResolver instance used for resolution
     * @param pomFile the local file to resolve
     * @throws IOException when there's an issue opening the file
     * @throws XmlPullParserException when there's an issue parsing the local file using the maven model
     */
    public void resolveLocalFile(PomResolver resolver, InputStream pomFile) throws IOException, XmlPullParserException{
        MavenXpp3Reader reader = new MavenXpp3Reader();

        Model model = reader.read(pomFile, true);
        String groupId = model.getGroupId();
        String version = model.getVersion();

        if(groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }

        if(version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }

        if(groupId == null || model.getArtifactId() == null || version == null) {
            throw new NullPointerException();
        }

        setIdent(new ArtifactIdent(groupId, model.getArtifactId(), version));
        setRawPomFeatures(processRawPomFeatures(model, resolver));
    }

    /**
     * This method handles parsing the raw features from the local pom file.
     *
     * @param model instance of the maven model which has parsed the pom file
     * @param resolver pom resolver to aid in feature collection
     * @return raw features that are collected during resolution
     */
    public RawPomFeatures processRawPomFeatures(Model model, PomResolver resolver) {
        RawPomFeatures rawPomFeatures = new RawPomFeatures();

        //parent information
        if(model.getParent() != null) {
            rawPomFeatures.setParent(new ArtifactIdent(model.getParent().getGroupId(), model.getParent().getArtifactId(), model.getParent().getVersion()));
        }

        //more project information
        rawPomFeatures.setName(model.getName());
        rawPomFeatures.setDescription(model.getDescription());
        rawPomFeatures.setUrl(model.getUrl());

        if(model.getProperties() != null) {
            Map<String, String> props = new HashMap<>();
            for(String propName : model.getProperties().stringPropertyNames()) {
                props.put(propName, model.getProperties().getProperty(propName));
            }
            rawPomFeatures.setProperties(props);
        }

        rawPomFeatures.setPackaging(model.getPackaging());
        rawPomFeatures.setInceptionYear(model.getInceptionYear());

        List<Dependency> dependencies = resolver.getDependencies(model.getDependencies());
        rawPomFeatures.setDependencies(dependencies);

        List<License> licenses = new ArrayList<>();
        if(!model.getLicenses().isEmpty()) {
            for(org.apache.maven.model.License license : model.getLicenses()) {
                licenses.add(new License(license.getName(), license.getUrl()));
            }
        }
        rawPomFeatures.setLicenses(licenses);

        if(model.getDependencyManagement() != null) {
            List<Dependency> managedDependencies = resolver.getDependencies(model.getDependencyManagement().getDependencies());
            rawPomFeatures.setDependencyManagement(managedDependencies);
        }

        return rawPomFeatures;

    }

    /**
     * @see PomResolver
     * @param resolver the resolver used to resolve the parents and imports of the local pom
     */
    public void resolveParentAndImport(PomResolver resolver) {
        if(getRawPomFeatures().getParent() != null) {
            try {
                setParent(resolver.processArtifact(getRawPomFeatures().getParent()));
            } catch (PomResolutionException | org.tudo.sse.resolution.FileNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        if(getRawPomFeatures().getDependencyManagement() != null) {
            try {
                setImports(resolver.resolveImports(getRawPomFeatures().getDependencyManagement(), this));
            } catch (PomResolutionException | org.tudo.sse.resolution.FileNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * @see PomResolver
     * @param resolver the pomResolver used to drive this method
     */
    public void resolveDependencies(PomResolver resolver) {
        setResolvedDependencies(resolver.resolveDependencies(this));
    }

}
