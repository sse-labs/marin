package org.tudo.sse.model.pom;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.tudo.sse.model.*;
import org.tudo.sse.model.ResolutionContext;
import org.tudo.sse.resolution.PomResolutionException;
import org.tudo.sse.resolution.PomResolver;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class can be used to perform pom resolution on a local pom file.
 */
public class LocalPomInformation extends PomInformation {

    private final Path pomPath;
    private final boolean loadTransitives;
    private final InputStream pomStream;


    /**
     * Creates a new LocalPomInformation object for a POM file located at the given path.
     *
     * @param pomPath The Path object identifying a pom file
     * @param loadTransitives Whether to load transitive pom information upon initialization
     */
    public LocalPomInformation(Path pomPath, boolean loadTransitives){
        this.pomPath = pomPath;
        this.loadTransitives = loadTransitives;
        this.pomStream = null;
    }

    /**
     * Creates a new LocalPomInformation object for a pom file input stream without a file path on the current drive.
     * @param pomStream Input Stream containing pom file contents
     * @param loadTransitives Whether to load transitive pom information upon initialization
     */
    public LocalPomInformation(InputStream pomStream, boolean loadTransitives){
        this.pomPath = null;
        this.loadTransitives = loadTransitives;
        this.pomStream = pomStream;
    }

    /**
     * Resolve the local pom file and compute all information as specified upon object creation.
     *
     * @param resolver The pom resolver to use for resolution
     * @throws IOException If reading the pom file contents fails
     * @throws XmlPullParserException If parsing the pom.xml file fails
     */
    public void resolveLocalPom(PomResolver resolver) throws IOException, XmlPullParserException {
        if(pomStream != null) {
            resolveLocalFile(resolver, this.pomStream);
        } else if(pomPath != null) {
            try (FileInputStream is = new FileInputStream(this.pomPath.toFile())) {
                resolveLocalFile(resolver, is);
            }
        }

        if(this.loadTransitives){
            resolveParentAndImport(resolver, ResolutionContext.createAnonymousContext());
            resolveDependencies(resolver);
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
    private void resolveLocalFile(PomResolver resolver, InputStream pomFile) throws IOException, XmlPullParserException{
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

        this.ident = new ArtifactIdent(groupId, model.getArtifactId(), version);
        this.rawPomFeatures = processRawPomFeatures(model, resolver);
    }

    /**
     * This method handles parsing the raw features from the local pom file.
     *
     * @param model instance of the maven model which has parsed the pom file
     * @param resolver pom resolver to aid in feature collection
     * @return raw features that are collected during resolution
     */
    private RawPomFeatures processRawPomFeatures(Model model, PomResolver resolver) {
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
     * Resolves all transitive pom references, i.e. parent POMs and import scoped POM files.
     * @see PomResolver
     * @param resolver the resolver used to resolve the parents and imports of the local pom
     */
    private void resolveParentAndImport(PomResolver resolver, ResolutionContext ctx) {
        if(this.rawPomFeatures.getParent() != null) {
            try {
                this.parent = resolver.processArtifact(this.rawPomFeatures.getParent(), ctx);
            } catch (PomResolutionException | org.tudo.sse.resolution.FileNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        if(this.rawPomFeatures.getDependencyManagement() != null) {
            try {
                this.imports = resolver.resolveImports(this.rawPomFeatures.getDependencyManagement(), this, ctx);
            } catch (PomResolutionException | org.tudo.sse.resolution.FileNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * Resolves all dependencies of the current POM.
     * @see PomResolver
     * @param resolver the pomResolver used to drive this method
     */
    private void resolveDependencies(PomResolver resolver) {
        setResolvedDependencies(resolver.resolveDependencies(this));
    }

}
