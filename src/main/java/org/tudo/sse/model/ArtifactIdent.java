package org.tudo.sse.model;

import java.net.URI;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.semver.SemanticVersionNumber;
import org.tudo.sse.semver.SemanticVersionParsingException;
import org.tudo.sse.utils.MavenCentralRepository;


/**
 * The ArtifactIdent holds the gav triple for each artifact on the maven central repository.
 * As well as methods for retrieving different file types from the maven central repository.
 */
public class ArtifactIdent {

    /**
     * The Maven Central repository base URL
     */
    public static final String CENTRAL_REPOSITORY_URL = "https://repo1.maven.org/maven2/";
    /**
     * The group section of the identifier.
     */
    private String groupID;

    /**
     * The specific artifact identifier.
     */
    private String artifactID;

    /**
     * The GA tuple of this artifact
     */
    private String GA;

    /**
     * The GAV triple of this artifact
     */
    private String GAV;

    /**
     * The version of the artifact.
     */
    private String version;
    /**
     * The repository where this artifact can be found - if different from the central Repo
     */
    private String customRepository;

    private boolean didParseSemVer = false;
    private boolean semVerValid = false;
    private SemanticVersionNumber semVer = null;
    private SemanticVersionParsingException semVerException = null;

    private static final Logger log = LoggerFactory.getLogger(ArtifactIdent.class);

    /**
     * Creates a new artifact identifier with the given attributes. Artifact identifiers correspond to Maven GAV-Triples.
     * @param groupID The Maven group ID
     * @param artifactID The Maven artifact ID
     * @param version The artifact version
     */
    public ArtifactIdent(String groupID, String artifactID, String version) {
        this.artifactID = artifactID;
        this.groupID = groupID;
        this.version = version;
    }

    /**
     * Copy-Constructor, creates a copy of the given artifact identifier.
     *
     * @param toCopy Identifier to copy
     */
    public ArtifactIdent(ArtifactIdent toCopy) {
        this.groupID = toCopy.groupID;
        this.artifactID = toCopy.artifactID;
        this.version = toCopy.version;
        this.customRepository = toCopy.customRepository;
    }

    /**
     * Gets the groupID.
     * @return groupID
     */
    public String getGroupID() {
        return groupID;
    }

    /**
     * This updates the groupID value
     * @param groupID new groupID value
     */
    public void setGroupID(String groupID) {
        this.groupID = groupID;
        this.GA = this.groupID + ":" + this.artifactID;
        this.GAV = groupID + ":" + artifactID + ":" + version;
    }

    /**
     * Gets the artifactID.
     * @return artifactID
     */
    public String getArtifactID() {
        return artifactID;
    }

    /**
     * This updates the artifactID value
     * @param artifactID new artifactID value
     */
    public void setArtifactID(String artifactID) {
        this.artifactID = artifactID;
        this.GA = this.groupID + ":" + this.artifactID;
        this.GAV = groupID + ":" + artifactID + ":" + version;
    }

    /**
     * Gets GA tuple for this artifact.
     * @return GA tuple separated by colon
     */
    public String getGA(){
        if(this.GA == null){
            this.GA = this.groupID + ":" + this.artifactID;
        }

        return this.GA;
    }
    /**
     * Gets the version.
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * This updates the version value
     * @param version new version value
     */
    public void setVersion(String version) {
        this.version = version;
        this.GAV = groupID + ":" + artifactID + ":" + version;

        this.semVer = null;
        this.didParseSemVer = false;
    }

    /**
     * Gets the repository where this artifact can be found.
     * @return repository
     */
    public String getRepository() {
        if(this.customRepository != null) return this.customRepository;
        else return CENTRAL_REPOSITORY_URL;
    }

    /**
     * Sets the repository for where this artifact can be found.
     * @param repository new repository value
     */
    public void setRepository(String repository) {
        this.customRepository = repository;
    }

    /**
     * Gets the coordinates, the full built identifier for a maven artifact.
     * @return full g:a:v value
     */
    public String getCoordinates() {

        if(this.GAV == null){
            this.GAV = groupID + ":" + artifactID + ":" + version;
        }
        return this.GAV;
    }

    /**
     * Gets the url for the pom file on maven central
     * @return pom URI
     */
    public URI getMavenCentralPomUri() {
        try {
            if(customRepository == null) {
                return MavenCentralRepository.buildPomFileURI(this);
            } else {
                return MavenCentralRepository.buildSecondaryPomFileURI(this, getRepository());
            }
        } catch(Exception x){
            log.error("Failed to build artifact pom url: {}", x.getMessage());
            return null;
        }
    }

    /**
     * Gets the url for the jar file on maven central
     * @return jar URI
     */
    public URI getMavenCentralJarUri() {
        try {
            return MavenCentralRepository.buildJarFileURI(this);
        } catch(Exception x){
            log.error("Failed to build artifact jar url: {}", x.getMessage());
            return null;
        }
    }

    /**
     * Gets the url for the versions file on maven central
     * @return versions.xml URI
     */
    public URI getMavenCentralXMLUri() {
        try {
            return MavenCentralRepository.buildVersionsFileURI(this);
        } catch(Exception x){
            log.error("Failed to build maven-metadata url: {}", x.getMessage());
            return null;
        }
    }

    /**
     * Parses this identifier's version according to the semantic versioning 2.0.0 standard. Returns the parsed number,
     * or throws an exception if the version does not comply to the required syntax. Note that the result of this method
     * invocation is cached, meaning that future calls will return the same parsed semantic version or throw the same
     * exception - parsing is only ever attempted once.
     *
     * @return Semantic version number if parsing was successful.
     * @throws SemanticVersionParsingException If version does not adhere to required syntax
     */
    public SemanticVersionNumber getSemanticVersion() throws SemanticVersionParsingException {

        if(this.didParseSemVer && this.semVerValid) return this.semVer;
        if(this.didParseSemVer) throw this.semVerException;

        try {
            this.didParseSemVer = true;
            this.semVer = SemanticVersionNumber.parse(this.version);
            this.semVerValid = true;
        } catch (SemanticVersionParsingException svpx){
            this.semVerException = svpx;
            this.semVerValid = false;
            throw this.semVerException;
        }

        return this.semVer;
    }

    /**
     * Checks whether this identifier's version is valid according to the semantic versioning 2.0.0 standard. The results
     * of this method invocation are cached so that future calls do not have to parse the version string again.
     * @return True if this identifier has a valid semantic version, false otherwise
     */
    public boolean hasValidSemanticVersion() {
        try {
            this.getSemanticVersion();
            return true;
        } catch(SemanticVersionParsingException svpx){
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtifactIdent that = (ArtifactIdent) o;
        return Objects.equals(groupID, that.groupID) &&
                Objects.equals(artifactID, that.artifactID) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupID, artifactID, version);
    }

    @Override
    public String toString() {
        return getCoordinates();
    }
}
