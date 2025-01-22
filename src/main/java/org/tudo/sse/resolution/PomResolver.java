package org.tudo.sse.resolution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.tudo.sse.ArtifactFactory;
import org.tudo.sse.model.*;
import org.tudo.sse.model.pom.License;
import org.tudo.sse.model.pom.PomInformation;
import org.tudo.sse.model.pom.RawPomFeatures;
import org.tudo.sse.utils.MavenCentralRepository;
import scala.Tuple2;

import java.io.*;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * The PomResolver resolves a multitude of features from pom files.
 * These include the raw features, parent and imports, dependencies, all transitive dependencies, and effective transitive dependencies.
 */
public class PomResolver {
    private final Path pathToDirectory;
    private final boolean output;
    private static final MavenCentralRepository MavenRepo = MavenCentralRepository.getInstance();
    private final boolean resolveTransitives;

    private static final Logger log = LogManager.getLogger(PomResolver.class);
    private final Map<String, Function<PomInformation, String>> predefinedPomValues;

    /**
     * In the constructor for this class, a boolean is passed to determine if transitive dependencies should be resolved when the resolution is run.
     *
     * @param resolveTransitives determines if transitive dependencies are to be resolved
     */
    public PomResolver(boolean resolveTransitives) {
        output = false;
        pathToDirectory = null;
        this.resolveTransitives = resolveTransitives;
        predefinedPomValues = new HashMap<>();
        predefinedPomValues.put("project.version", pom -> pom.getIdent().getVersion());
        predefinedPomValues.put("pom.version", pom -> pom.getIdent().getVersion());
        predefinedPomValues.put("version", pom -> pom.getIdent().getVersion());
        predefinedPomValues.put("pom.currentVersion", pom -> pom.getIdent().getVersion());
        predefinedPomValues.put("project.parent.version", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getVersion();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("pom.parent.version", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getVersion();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("parent.version", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getVersion();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("groupId", pom -> pom.getIdent().getGroupID());
        predefinedPomValues.put("project.groupId", pom -> pom.getIdent().getGroupID());
        predefinedPomValues.put("pom.groupId", pom -> pom.getIdent().getGroupID());
        predefinedPomValues.put("project.parent.groupId", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getGroupID();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("parent.groupId", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getGroupID();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("artifactId", pom -> pom.getIdent().getArtifactID());
        predefinedPomValues.put("project.artifactId", pom -> pom.getIdent().getArtifactID());
        predefinedPomValues.put("pom.artifactId", pom -> pom.getIdent().getArtifactID());
        predefinedPomValues.put("project.parent.artifactId", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getArtifactID();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("parent.artifactId", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getArtifactID();
            } else {
                return null;
            }
        });
    }

    public PomResolver(boolean output, Path pathToDirectory, boolean resolveTransitives) {
        this.output = output;
        this.pathToDirectory = pathToDirectory;
        this.resolveTransitives = resolveTransitives;
        predefinedPomValues = new HashMap<>();
        predefinedPomValues.put("project.version", pom -> pom.getIdent().getVersion());
        predefinedPomValues.put("pom.version", pom -> pom.getIdent().getVersion());
        predefinedPomValues.put("version", pom -> pom.getIdent().getVersion());
        predefinedPomValues.put("pom.currentVersion", pom -> pom.getIdent().getVersion());
        predefinedPomValues.put("project.parent.version", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getVersion();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("pom.parent.version", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getVersion();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("parent.version", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getVersion();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("groupId", pom -> pom.getIdent().getGroupID());
        predefinedPomValues.put("project.groupId", pom -> pom.getIdent().getGroupID());
        predefinedPomValues.put("pom.groupId", pom -> pom.getIdent().getGroupID());
        predefinedPomValues.put("project.parent.groupId", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getGroupID();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("parent.groupId", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getGroupID();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("artifactId", pom -> pom.getIdent().getArtifactID());
        predefinedPomValues.put("project.artifactId", pom -> pom.getIdent().getArtifactID());
        predefinedPomValues.put("pom.artifactId", pom -> pom.getIdent().getArtifactID());
        predefinedPomValues.put("project.parent.artifactId", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getArtifactID();
            } else {
                return null;
            }
        });
        predefinedPomValues.put("parent.artifactId", pom -> {
            if (pom.getRawPomFeatures().getParent() != null) {
                return pom.getRawPomFeatures().getParent().getArtifactID();
            } else {
                return null;
            }
        });
    }

    /**
     * This method resolves all artifacts given a list of identifiers.
     *
     * @param idents list of identifiers to resolve
     * @return list of resolved artifacts
     * @see Artifact
     */
    public List<Artifact> resolveArtifacts(List<ArtifactIdent> idents) {
        List<Artifact> poms = new ArrayList<>();

        log.info("There are {} identifiers to resolve", idents.size());
        int count =0;
        for(ArtifactIdent ident : idents) {
            try {
                poms.add(resolveArtifact(ident));
            } catch(PomResolutionException e) {
                log.error(e);
            } catch ( IOException e) {
                throw new RuntimeException(e);
            } catch(FileNotFoundException ignored) {}

            count++;
            if(count % 10000 == 0) {
                log.info("{} artifacts have been processed", count);
            }
        }

        log.info("Finished processing {} pomArtifacts", count);
        log.info("Collected {} pomArtifacts", poms.size());
        return poms;
    }

    /**
     * This method given an Artifact Identifier, resolves a single artifact (raw features, parent, imports, dependencies, and transitive dependencies).
     *
     * @param identifier id for the pom artifact to resolve
     * @return an artifact with resolved PomInformation
     * @see PomInformation
     */
    public Artifact resolveArtifact(ArtifactIdent identifier) throws FileNotFoundException, IOException, PomResolutionException {
        Map<ArtifactIdent, Artifact> alrEncountered = new HashMap<>();
        if(output) {
            try(InputStream inputStream = MavenRepo.openPomFileInputStream(identifier)){
                byte[] pomBytes = inputStream.readAllBytes();

                Path filePath = pathToDirectory.resolve(identifier.getGroupID() + "-" + identifier.getArtifactID() + "-" + identifier.getVersion() + ".xml");
                if(!Files.exists(filePath)) {
                    Files.createFile(filePath);
                    Files.write(filePath, pomBytes);
                }
            }
        }

        Artifact toReturn = processArtifact(identifier);
        toReturn.getPomInformation().setResolvedDependencies(resolveDependencies(toReturn.getPomInformation()));

        if(resolveTransitives) {
            resolveAllTransitives(toReturn, alrEncountered, null);
            resolveEffectiveTransitives(toReturn);
        }
        return toReturn;
    }

    /**
     * This method parses raw features from the pom file for the given artifact identifier.
     *
     * @param input the pom file to be processed
     * @param identifier the identifier of the pom artifact being processed
     * @return an object containing all the features of the pom file
     * @see RawPomFeatures
     */
    public RawPomFeatures processRawPomFeatures(InputStream input, ArtifactIdent identifier) throws PomResolutionException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(input, true);
            RawPomFeatures rawPomFeatures = new RawPomFeatures();

            if(model.getDistributionManagement() != null && model.getDistributionManagement().getRelocation() != null) {
                Relocation relocation = model.getDistributionManagement().getRelocation();
                ArtifactIdent relocationIdent = new ArtifactIdent(identifier);

                if(relocation.getGroupId() != null) {
                 relocationIdent.setGroupID(relocation.getGroupId());
                }

                if(relocation.getArtifactId() != null) {
                    relocationIdent.setArtifactID(relocation.getArtifactId());
                }

                if(relocation.getVersion() != null) {
                    relocationIdent.setVersion(relocation.getVersion());
                }
                rawPomFeatures.setRelocation(relocationIdent);
                return rawPomFeatures;
            }

            if(model.getRepositories() != null) {
                List<String> repos = new ArrayList<>();
                for(Repository repo : model.getRepositories()) {
                    repos.add(repo.getUrl());
                }
                rawPomFeatures.setRepositories(repos);
            }

            if(model.getParent() != null) {
                rawPomFeatures.setParent(new ArtifactIdent(model.getParent().getGroupId(), model.getParent().getArtifactId(), model.getParent().getVersion()));
            }

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

            List<org.tudo.sse.model.pom.Dependency> dependencies = getDependencies(model.getDependencies());
            rawPomFeatures.setDependencies(dependencies);

            List<License> licenses = new ArrayList<>();
            if(!model.getLicenses().isEmpty()) {
                for(org.apache.maven.model.License license : model.getLicenses()) {
                    licenses.add(new License(license.getName(), license.getUrl()));
                }
            }
            rawPomFeatures.setLicenses(licenses);

            if(model.getDependencyManagement() != null) {
                List<org.tudo.sse.model.pom.Dependency> managedDependencies = getDependencies(model.getDependencyManagement().getDependencies());
                rawPomFeatures.setDependencyManagement(managedDependencies);
            }

            return rawPomFeatures;

        } catch (IOException | XmlPullParserException e) {
            try { input.close(); } catch (Exception ignored) {}
            throw new PomResolutionException(e.getMessage(), identifier, e);
        }
    }

    /**
     * This method converts the pom model dependencies to custom class dependency objects
     *
     * @param modelDependencies a list of maven model dependencies
     * @see Dependency
     * @see org.tudo.sse.model.pom.Dependency
     * @return a custom class dependency object
     */
    public List<org.tudo.sse.model.pom.Dependency> getDependencies(List<Dependency> modelDependencies) {
        List<org.tudo.sse.model.pom.Dependency> dependencies = new ArrayList<>();
        for(Dependency dependency: modelDependencies) {
            boolean isResolved = true;
            boolean versionRange = false;

            if(dependency.getVersion() == null || dependency.getVersion().contains("${") || dependency.getScope() == null) {
                isResolved = false;
            } else {
                versionRange = isVersionRange(dependency.getVersion());
            }

            ArtifactIdent currentID = new ArtifactIdent(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            String scope = null;
            Set<String> exclusions = null;

            if(dependency.getScope() != null) {
                scope = dependency.getScope();
            }

            if(dependency.getExclusions() != null) {
                exclusions = new HashSet<>();
                for(Exclusion exclusion : dependency.getExclusions()) {
                    exclusions.add(exclusion.getGroupId() + ":" + exclusion.getArtifactId());
                }
            }

            org.tudo.sse.model.pom.Dependency current = new org.tudo.sse.model.pom.Dependency(currentID, scope, isResolved, versionRange, dependency.isOptional(), exclusions);
            dependencies.add(current);
        }
        return dependencies;
    }

    /**
     * This method handles resolving an artifact via raw Pom information and parent and dependency resolution
     * @param identifier used to retrieve the pom artifact from the maven central repository
     * @return an artifact with resolved rawPomFeatures, parent, and import information
     */
    public Artifact processArtifact(ArtifactIdent identifier) throws PomResolutionException, FileNotFoundException, IOException {
        if(ArtifactFactory.getArtifact(identifier) != null && Objects.requireNonNull(ArtifactFactory.getArtifact(identifier)).getPomInformation() != null) {
            return ArtifactFactory.getArtifact(identifier);
        }

        PomInformation pomInformation = new PomInformation(identifier);

        RawPomFeatures rawPomFeatures;
        try(InputStream is = MavenRepo.openPomFileInputStream(identifier) ) {
            rawPomFeatures = processRawPomFeatures(is, identifier);
        } catch (SocketException e) {
            throw new PomResolutionException(e.getMessage(), identifier, e);
        }

        ArtifactIdent relocation = null;
        while(rawPomFeatures != null && rawPomFeatures.getRelocation() != null) {
            relocation = rawPomFeatures.getRelocation();
            InputStream relocStream = MavenRepo.openPomFileInputStream(relocation);
            rawPomFeatures = processRawPomFeatures(relocStream, relocation);
            relocStream.close();
        }
        pomInformation.setRelocation(relocation);
        pomInformation.setRawPomFeatures(rawPomFeatures);

        if(pomInformation.getRawPomFeatures() != null) {
            getAllRelevantFiles(pomInformation);
        }

        return ArtifactFactory.createArtifact(pomInformation);
    }

    private void getAllRelevantFiles(PomInformation pomInformation) throws PomResolutionException, FileNotFoundException, IOException {
        if(pomInformation.getRawPomFeatures().getParent() != null) {
            try {
                pomInformation.setParent(processArtifact(pomInformation.getRawPomFeatures().getParent()));
            } catch(PomResolutionException e) {
                log.error("Failed to resolve parent: " + e.getMessage());
            } catch (FileNotFoundException ignored) {}
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if(pomInformation.getRawPomFeatures().getDependencyManagement() != null) {
            pomInformation.setImports(resolveImports(pomInformation.getRawPomFeatures().getDependencyManagement(), pomInformation));
        }
    }

    /**
     *
     * @param managedDependencies list of managed dependencies from the rawPomFeatures, to be looked through for an import
     * @param info the current information object
     * @return a list of imported artifacts
     * @throws PomResolutionException handles errors that arise with pom resolution
     * @throws FileNotFoundException handles errors with not finding a file
     * @throws IOException handles errors with opening and closing files
     */
    public List<Artifact> resolveImports(List<org.tudo.sse.model.pom.Dependency> managedDependencies, PomInformation info) throws PomResolutionException, FileNotFoundException, IOException {
        ArrayList<Artifact> imports = new ArrayList<>();

        for(org.tudo.sse.model.pom.Dependency dependency: managedDependencies) {
            if(dependency.getScope() != null && dependency.getScope().equals("import")) {
                dependency = resolveVersion(dependency, info);
                Artifact temp = processArtifact(dependency.getIdent());
                if(temp != null) {
                    imports.add(temp);
                }
            }
        }

        return imports;
    }

    /**
     * Resolves a list of dependencies, based on the list provided in the current PomInformation.
     *
     * @param current this is the current information object that dependencies will be resolved from
     * @see PomInformation
     * @return a list of resolved dependencies
     */
    public List<org.tudo.sse.model.pom.Dependency> resolveDependencies(PomInformation current) {
        List<org.tudo.sse.model.pom.Dependency> dependencies = new ArrayList<>();

        if(current.getRawPomFeatures() != null && current.getRawPomFeatures().getDependencies() != null) {
            for(org.tudo.sse.model.pom.Dependency dependency : current.getRawPomFeatures().getDependencies()) {
                if(!dependency.isResolved() && dependency.getIdent().getGroupID() != null && dependency.getIdent().getArtifactID() != null) {
                    if(current.getRawPomFeatures().getDependencyManagement() != null) {
                        matchManaged(dependency, current);
                    }

                    dependencies.add(resolveDependency(dependency, current));
                } else if(dependency.getIdent().getGroupID() != null && dependency.getIdent().getArtifactID() != null) {
                    org.tudo.sse.model.pom.Dependency toReturn = new org.tudo.sse.model.pom.Dependency(dependency);
                    if(dependency.isVersionRange()) {
                        String resolved = resolveVersionRange(dependency);
                        if(resolved != null) {
                            toReturn.getIdent().setVersion(resolved);
                        }
                    }

                    dependencies.add(toReturn);
                }
            }
        }
        return dependencies;
    }

    /**
     * This method resolves the dependencies that have any missing information from them (groupId, version, scope)
     *
     * @param dependency dependency to resolve
     * @param current where the dependency is coming from
     * @return a resolved dependency
     */
    public org.tudo.sse.model.pom.Dependency resolveDependency(org.tudo.sse.model.pom.Dependency dependency, PomInformation current) {
        org.tudo.sse.model.pom.Dependency toReturn = new org.tudo.sse.model.pom.Dependency(dependency);
        toReturn = resolveGroupId(toReturn, current);
        toReturn = resolveArtifactId(toReturn, current);
        toReturn = resolveVersion(toReturn, current);
        toReturn = resolveScope(toReturn, current);
        if(toReturn.getIdent().getVersion() != null && !toReturn.getIdent().getVersion().contains("${")) {
            toReturn.setVersionRange(isVersionRange(toReturn.getIdent().getVersion()));

            if(!toReturn.isVersionRange()) {
                toReturn.setResolved(true);
            }
        }

        if(toReturn.isVersionRange()) {
            String resolved = resolveVersionRange(toReturn);
            if(resolved != null) {
                toReturn.getIdent().setVersion(resolved);
                toReturn.setResolved(true);
            }
        }

        return toReturn;
    }

    private void matchManaged(org.tudo.sse.model.pom.Dependency dep, PomInformation current) {
        if(dep.getIdent().getVersion() == null) {
            String missingVersion = dep.getIdent().getGroupID()  + ":" + dep.getIdent().getArtifactID();

            for(org.tudo.sse.model.pom.Dependency cur : current.getRawPomFeatures().getDependencyManagement()) {
                if(missingVersion.equals(cur.getIdent().getGroupID() + ":" + cur.getIdent().getArtifactID())) {
                    dep.getIdent().setVersion(cur.getIdent().getVersion());

                }
            }
        }
    }

    private org.tudo.sse.model.pom.Dependency resolveScope(org.tudo.sse.model.pom.Dependency toResolve, PomInformation current) {
        org.tudo.sse.model.pom.Dependency toReturn = new org.tudo.sse.model.pom.Dependency(toResolve);
        ArtifactIdent curIdent = toReturn.getIdent();
        String scope = toReturn.getScope();

        if(scope == null) {
            String missingVersion = curIdent.getGroupID()  + ":" + curIdent.getArtifactID();
            if(current.getParent() != null) {
                if(current.getParent().getPomInformation().getRawPomFeatures() != null && current.getParent().getPomInformation().getRawPomFeatures().getDependencyManagement() != null) {
                    Tuple2<String, String> depStuff = findGA(missingVersion, current.getParent().getPomInformation().getRawPomFeatures().getDependencyManagement());
                    if(depStuff != null) {
                        if(depStuff._2 != null) {
                            toReturn.setScope(depStuff._2);
                            return toReturn;
                        }
                    }
                }

                toReturn = resolveScope(toReturn, current.getParent().getPomInformation());
                if (toReturn.getScope() != null) {
                    return toReturn;
                }
            }

            if(current.getImports() != null) {
                for(Artifact anImport : current.getImports()) {
                    if(anImport.getPomInformation().getRawPomFeatures().getDependencyManagement() != null) {
                        Tuple2<String, String> depStuff = findGA(missingVersion, anImport.getPomInformation().getRawPomFeatures().getDependencyManagement());
                        if(depStuff != null) {
                            if(depStuff._2 != null) {
                                toReturn.setScope(depStuff._2);
                                return toReturn;
                            }
                        }
                    }
                    toReturn = recursiveHandler(anImport.getPomInformation(), toReturn);
                    if(toReturn.getScope() != null) {
                        return toReturn;
                    }
                }
            }
        }

        if(scope == null) {
            toReturn.setScope("compile");
        }

        return toReturn;
    }

    private org.tudo.sse.model.pom.Dependency resolveGroupId(org.tudo.sse.model.pom.Dependency toResolve, PomInformation current) {
        org.tudo.sse.model.pom.Dependency toReturn = new org.tudo.sse.model.pom.Dependency(toResolve);
        if(toResolve.getIdent().getGroupID().contains("${")) {
            String groupID = toResolve.getIdent().getGroupID();
            String key = groupID.substring(2, groupID.indexOf("}"));
            if(predefinedPomValues.containsKey(key) && predefinedPomValues.get(key).apply(current) != null) {
                groupID = predefinedPomValues.get(key).apply(current);
            }
            else if(current.getRawPomFeatures().getProperties().containsKey(key)) {
                groupID = current.getRawPomFeatures().getProperties().get(key);
            }
            toReturn.getIdent().setGroupID(groupID);
        }

        return toReturn;
    }

    private org.tudo.sse.model.pom.Dependency resolveArtifactId(org.tudo.sse.model.pom.Dependency toResolve, PomInformation current) {
        org.tudo.sse.model.pom.Dependency toReturn = new org.tudo.sse.model.pom.Dependency(toResolve);
        if(toResolve.getIdent().getArtifactID().contains("${")) {
            String artifactID = toResolve.getIdent().getArtifactID();
            String key = artifactID.substring(2, artifactID.indexOf("}"));
            if(predefinedPomValues.containsKey(key)) {
                artifactID = predefinedPomValues.get(key).apply(current);
            }

            else if(current.getRawPomFeatures().getProperties().containsKey(key)) {
                artifactID = current.getRawPomFeatures().getProperties().get(key);
            }
            toReturn.getIdent().setArtifactID(artifactID);
        }
        return toReturn;
    }

    private org.tudo.sse.model.pom.Dependency resolveVersion(org.tudo.sse.model.pom.Dependency toResolve, PomInformation current) {
        org.tudo.sse.model.pom.Dependency toReturn = new org.tudo.sse.model.pom.Dependency(toResolve);
        ArtifactIdent curIdent = toReturn.getIdent();
        String version = curIdent.getVersion();
        boolean parentResolved = false;

        if(version == null) {
            String missingVersion = curIdent.getGroupID()  + ":" + curIdent.getArtifactID();
            if(current.getParent() != null) {
                if(current.getParent().getPomInformation().getRawPomFeatures() != null && current.getParent().getPomInformation().getRawPomFeatures().getDependencyManagement() != null) {
                    Tuple2<String, String> depStuff = findGA(missingVersion, current.getParent().getPomInformation().getRawPomFeatures().getDependencyManagement());
                    if(depStuff != null) {
                        version = depStuff._1;
                        toReturn.getIdent().setVersion(version);
                        if(depStuff._2 != null && toReturn.getScope() == null) {
                            toReturn.setScope(depStuff._2);
                        }
                        parentResolved = true;
                        current = current.getParent().getPomInformation();
                    }
                }

                if (version == null) {
                    toReturn = resolveVersion(toReturn, current.getParent().getPomInformation());
                    curIdent = toReturn.getIdent();
                    version = curIdent.getVersion();
                }
            }

            if(!parentResolved && current.getImports() != null) {
                for(Artifact anImport : current.getImports()) {
                    if(anImport.getPomInformation().getRawPomFeatures().getDependencyManagement() != null) {
                        Tuple2<String, String> depStuff = findGA(missingVersion, anImport.getPomInformation().getRawPomFeatures().getDependencyManagement());
                        if(depStuff != null) {
                            version = depStuff._1;
                            if(depStuff._2 != null && toReturn.getScope() == null) {
                                toReturn.setScope(depStuff._2);
                            }
                            toReturn.getIdent().setVersion(version);
                            current = anImport.getPomInformation();
                        }
                    }
                    if(version == null) {
                        toReturn = recursiveHandler(anImport.getPomInformation(), toReturn);
                        curIdent = toReturn.getIdent();
                        version = curIdent.getVersion();
                    }
                }
            }
        }

        if(version != null && version.contains("${")) {
            String key = version.substring(version.indexOf("${") + 2, version.indexOf("}"));
            if(predefinedPomValues.containsKey(key) && predefinedPomValues.get(key).apply(current) != null) {
                version = predefinedPomValues.get(key).apply(current);
            } else if(current.getRawPomFeatures() != null && current.getRawPomFeatures().getProperties() != null) {
                while(version.contains("${") && current.getRawPomFeatures().getProperties().containsKey(key)) {
                    if(version.indexOf("}") + 1 == version.length()) {
                        version = current.getRawPomFeatures().getProperties().get(key);
                    } else {
                        version = current.getRawPomFeatures().getProperties().get(key) + version.substring(version.indexOf("}") + 1);
                    }

                    toReturn.getIdent().setVersion(version);

                    if(version.contains("}")) {
                        key = version.substring(2, version.indexOf("}"));
                    }
                }
            }

            if(version.contains("${")) {
                   toReturn = recursiveHandler(current, toReturn);
                   curIdent = toReturn.getIdent();
                   version = curIdent.getVersion();
            }

            toReturn.getIdent().setVersion(version);
        }
        return toReturn;
    }

    private Tuple2 findGA(String missing, List<org.tudo.sse.model.pom.Dependency> management) {
        for (org.tudo.sse.model.pom.Dependency dependency : management) {
            if (missing.equals(dependency.getIdent().getGroupID() + ":" + dependency.getIdent().getArtifactID())) {
                if(dependency.getScope() != null) {
                    return new Tuple2<>(dependency.getIdent().getVersion(), dependency.getScope());
                } else {
                    return new Tuple2<>(dependency.getIdent().getVersion(), null);
                }
            }
        }
        return null;
    }

    private org.tudo.sse.model.pom.Dependency recursiveHandler(PomInformation current, org.tudo.sse.model.pom.Dependency toResolve) {
        if(current.getParent() != null) {
            return resolveVersion(toResolve, current.getParent().getPomInformation());
        } else if(current.getImports() != null) {
            for(Artifact anotherOne : current.getImports()) {
                return resolveVersion(toResolve, anotherOne.getPomInformation());
            }
        }
            return toResolve;
    }

    /**
     * Resolves a version for a dependency where the version is defined as a range.
     *
     * @param toResolve the dependency for which a range is being resolved
     * @return the resolved version value
     */
    public String resolveVersionRange(org.tudo.sse.model.pom.Dependency toResolve) {
        List<VersionRange> ranges = new ArrayList<>();
        List<String> sets = splitSets(toResolve.getIdent().getVersion());
        GenericVersionScheme scheme = new GenericVersionScheme();
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        String highestMatching = null;

        try {
            for(String splitRange : sets) {
              ranges.add(scheme.parseVersionRange(splitRange));
            }

            var xmlData = new BufferedReader(new InputStreamReader(Objects.requireNonNull(MavenRepo.openXMLFileInputStream(toResolve.getIdent()))));
            Metadata meta = reader.read(xmlData);
            xmlData.close();

            if(meta.getVersioning() != null) {
                List<String> versioning = meta.getVersioning().getVersions();

                for (VersionRange range : ranges) {
                    if (range.getUpperBound() == null) {
                        //add comparison here to check that the lower bound is met
                        Version current = scheme.parseVersion(versioning.get(versioning.size() - 1));

                        if (range.getLowerBound().isInclusive()) {
                            if (range.getLowerBound().getVersion().compareTo(current) <= 0) {
                                highestMatching = versioning.get(versioning.size() - 1);
                            }
                        } else {
                            if (range.getLowerBound().getVersion().compareTo(current) < 0) {
                                highestMatching = versioning.get(versioning.size() - 1);
                            }
                        }
                        return highestMatching;
                    }
                    if (!range.getUpperBound().isInclusive()) {
                        for (String version : versioning) {
                            Version current = scheme.parseVersion(version);
                            if (current.compareTo(range.getUpperBound().getVersion()) < 0) {
                                highestMatching = version;
                            }
                        }
                    } else {
                        for (String version : versioning) {
                            Version current = scheme.parseVersion(version);
                            if (current.compareTo(range.getUpperBound().getVersion()) <= 0) {
                                highestMatching = version;
                            }
                        }
                    }
                }
            } else {
                throw new IOException();
            }

        } catch (IOException | XmlPullParserException | InvalidVersionSpecificationException | FileNotFoundException e) {
            log.error(e);
        }

        return highestMatching;
    }

    private List<String> splitSets(String range) {
        List<String> ranges = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean isRangeOpen = false;

        for(int i = 0; i < range.length(); i++){
            switch(range.charAt(i)){
                case '[':
                case '(':
                    isRangeOpen = true;
                    current = new StringBuilder();
                    current.append(range.charAt(i));
                    break;
                case ']':
                case ')':
                    current.append(range.charAt(i));
                    isRangeOpen = false;
                    ranges.add(current.toString());
                    break;
                default:
                    // Keep anything inside a range (whitespaces, etc) as is, drop anything between ranges (commas, whitespaces)
                    if(isRangeOpen)
                        current.append(range.charAt(i));
                    break;
            }
        }

        return ranges;
    }

    private static boolean isVersionRange(String version) {
        return version.contains("[") || version.contains("]") || version.contains("(") || version.contains(")") || version.contains(",");
    }

    /**
     * This method resolves all the transitive dependencies of a given artifact. This is done recursively working with resolveTransitives and the recursiveHandler.
     *
     * @param toResolve the current artifact transitive dependencies are being resolved for.
     * @throws IOException thrown when there's an issue opening the pom for an artifact
     */
    public void resolveAllTransitives(Artifact toResolve) throws IOException {
        resolveAllTransitives(toResolve, new HashMap<>(), null);
    }

    /**
     * This method resolves all the transitive dependencies of a given artifact. This is done recursively working with resolveTransitives and the recursiveHandler.
     *
     * @param current the current artifact transitive dependencies are being resolved for.
     * @param alrEncountered a map of dependencies that have already been resolved to avoid double resolutions
     * @param exclusions a Set of G:A values to not include in resolution
     * @throws IOException thrown when there's an issue opening the pom for an artifact
     */
    public void resolveAllTransitives(Artifact current, Map<ArtifactIdent, Artifact> alrEncountered, Set<String> exclusions) throws IOException {
        List<Artifact> transitives = new ArrayList<>();
        for (org.tudo.sse.model.pom.Dependency dependency : current.getPomInformation().getResolvedDependencies()) {
            if(exclusions == null || !exclusions.contains(dependency.getIdent().getGA())) {

                boolean dependencyRelevant = (dependency.getScope().equals("compile") || dependency.getScope().equals("runtime")) &&
                        !dependency.isOptional() && dependency.isResolved();

                if (dependencyRelevant && !alrEncountered.containsKey(dependency.getIdent())) {
                    alrEncountered.put(dependency.getIdent(), null);
                    Artifact transitive = resolveTransitives(current, dependency, alrEncountered, dependency.getExclusions());
                    if (transitive != null) {
                        if(transitive.getRelocation() != null) {
                            alrEncountered.remove(dependency.getIdent());
                            alrEncountered.put(transitive.getRelocation(), transitive);
                        } else {
                            alrEncountered.put(dependency.getIdent(), transitive);
                        }
                        transitives.add(transitive);
                    }
                } else if(dependencyRelevant && alrEncountered.get(dependency.getIdent()) != null) {
                    transitives.add(alrEncountered.get(dependency.getIdent()));
                }
            }
        }

        current.getPomInformation().setAllTransitiveDependencies(transitives);
    }

    private Artifact resolveTransitives(Artifact current, org.tudo.sse.model.pom.Dependency toResolve, Map<ArtifactIdent, Artifact> alrEncountered, Set<String> exclusions) throws IOException {
        try {
            return recursiveResolver(toResolve.getIdent(), alrEncountered, exclusions);
        } catch(FileNotFoundException | PomResolutionException e) {
            if(!current.getPomInformation().getRawPomFeatures().getRepositories().isEmpty()) {
                return resolveFromSecondaryRepo(current.getPomInformation().getRawPomFeatures().getRepositories(), toResolve, alrEncountered, exclusions);
            }
            if(!(e instanceof FileNotFoundException)) log.error(e);
            return null;
        }
    }

    private Artifact recursiveResolver(ArtifactIdent identifier, Map<ArtifactIdent, Artifact> alrEncountered, Set<String> exclusions) throws FileNotFoundException, IOException, PomResolutionException {
        Artifact current = processArtifact(identifier);
        current.getPomInformation().setResolvedDependencies(resolveDependencies(current.getPomInformation()));
        resolveAllTransitives(current, alrEncountered, exclusions);
        return current;
    }

    private Artifact resolveFromSecondaryRepo(List<String> repos, org.tudo.sse.model.pom.Dependency toResolve, Map<ArtifactIdent, Artifact> alrEncountered, Set<String> exclusions) {
        int i = 0;
        Artifact toReturn = null;
        while(i < repos.size() && toReturn == null) {
            toResolve.getIdent().setRepository(repos.get(i));
            try {
                toReturn = recursiveResolver(toResolve.getIdent(), alrEncountered, exclusions);
            } catch (IOException | PomResolutionException e) {
                log.error(e);
            } catch (FileNotFoundException ignored) {}
            i++;
        }

        return toReturn;
    }

    /**
     * This method attempts to get all effective transitive dependencies, resolving conflicts, and removing duplicates.
     * @param toResolve the artifact for which to resolve the effective transitive dependencies
     */
    public void resolveEffectiveTransitives(Artifact toResolve) {
        // Can only resolve dependencies if POM info is present.
        // IMPROVE: Do we want to load POM info on demand?
        if(!toResolve.hasPomInformation()){
            throw new IllegalStateException("Cannot resolve dependencies for " + toResolve.ident.getCoordinates() + " : No POM information loaded");
        }

        List<Artifact> allTransitive = toResolve.getPomInformation().getAllTransitiveDependencies();

        // If "normal" transitive dependencies have not been computed yet, we try to do that on demand
        if(allTransitive == null){
            try {
                this.resolveAllTransitives(toResolve, new HashMap<>(), null);
                allTransitive = toResolve.getPomInformation().getAllTransitiveDependencies();
            } catch (IOException iox){
                throw new IllegalStateException("Failed to compute transitive dependencies on demand", iox);
            }
        }

        // If "normal" transitive dependencies are still null, we cannot do anything and abort
        if(allTransitive == null){
            throw new RuntimeException("Unable to compute transitive dependencies for " + toResolve.ident.getCoordinates());
        }


        Map<String, ArtifactIdent> foundGA = new HashMap<>();
        Map<String, List<ArtifactIdent>> conflicts = new HashMap<>();
        List<Artifact> toReturn = new ArrayList<>();

        Queue<Artifact> toProcess = new LinkedList<>(allTransitive);

        while(!toProcess.isEmpty()) {
            Artifact artifact = toProcess.poll();

            String key = artifact.getIdent().getGroupID() + ":" + artifact.getIdent().getArtifactID();
            if(!foundGA.containsKey(key)) {
                foundGA.put(key, artifact.getIdent());
                if(!artifact.getPomInformation().getAllTransitiveDependencies().isEmpty()) {
                    toProcess.addAll(artifact.getPomInformation().getAllTransitiveDependencies());
                }
            } else {
                if(conflicts.containsKey(key)) {
                    conflicts.get(key).add(artifact.getIdent());
                } else {
                    List<ArtifactIdent> temp = new ArrayList<>();
                    temp.add(foundGA.get(key));
                    temp.add(artifact.getIdent());
                    conflicts.put(key, temp);
                }
            }

        }

        for(Map.Entry<String,ArtifactIdent> entry : foundGA.entrySet()) {
            toReturn.add(ArtifactFactory.getArtifact(entry.getValue()));
        }
        toResolve.getPomInformation().setEffectiveTransitiveDependencies(toReturn);
        toResolve.getPomInformation().setTransitiveConflicts(conflicts);
    }
}
