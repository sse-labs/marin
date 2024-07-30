package org.tudo.sse.model.pom;

import org.tudo.sse.model.ArtifactIdent;

import java.util.List;
import java.util.Map;

/**
 * The RawPomFeatures class holds the raw features parsed from a pom file.
 */
public class RawPomFeatures {

    private ArtifactIdent parent;
    private String name;
    private String description;
    private Map<String, String> properties;
    private String url;
    private String packaging;
    private String inceptionYear;
    private List<Dependency> dependencies;
    private List<String> repositories;
    private List<License> licenses;
    private List<Dependency> dependencyManagement;
    private ArtifactIdent relocation;

    /**
     * Retrieves the artifact identifier of the parent identifier
     * @return artifactIdent representing the parent artifact
     */
    public ArtifactIdent getParent() {
        return parent;
    }

    /**
     * Updates the value of the parent artifactIdent
     * @param parent new artifactIdent to update with
     */
    public void setParent(ArtifactIdent parent) {
        this.parent = parent;
    }

    /**
     * Retrieves the name of the current artifact
     * @return string representing the name
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the name of the artifact
     * @param name new name to update with
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the description of the artifact
     * @return string representing the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Updates the description of the artifact
     * @param description new description value
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Retrieves a map of the properties of the pom file
     * @return a map of string names mapping to string property values
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * updates the map of properties
     * @param properties new properties map to update with
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Retrieves the url for the artifact
     * @return a string representing the url to the artifact
     */
    public String getUrl() {
        return url;
    }

    /**
     * Updates the value of the url
     * @param url new url value to update with
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Retrieves the packaging of the artifact
     * @return a string representing the value of the packaging
     */
    public String getPackaging() {
        return packaging;
    }

    /**
     * Updates the packaging value
     * @param packaging string value to update packaging with
     */
    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    /**
     * Retrieves the inception year of the artifact
     * @return string representing the inception year of the artifact
     */
    public String getInceptionYear() {
        return inceptionYear;
    }

    /**
     * Updates the value of the inception year of the project
     * @param inceptionYear new string value to update inception year with
     */
    public void setInceptionYear(String inceptionYear) {
        this.inceptionYear = inceptionYear;
    }

    /**
     * Retrieves a list of the dependencies defined in the pom file
     * @return a list of dependency objects
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * updates the value of the list of dependencies found in the pom file
     * @param dependencies list to update value with
     */
    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Retrieves list of licenses defined in the pom file
     * @return a list of license objects
     */
    public List<License> getLicenses() {
        return licenses;
    }

    /**
     * Updates the value of the license list
     * @param licenses list to update value with
     */
    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    /**
     * Retrieves a list of managed dependencies found in the pom file
     * @return a list of managed dependencies
     */
    public List<Dependency> getDependencyManagement() {
        return dependencyManagement;
    }

    /**
     * Updates the value of the managed dependencies list
     * @param dependencyManagement list to update the value with
     */
    public void setDependencyManagement(List<Dependency> dependencyManagement) {
        this.dependencyManagement = dependencyManagement;
    }

    /**
     * Retrieves the artifact identifier of where the pom file is relocated to
     * @return artifactIdent object of relocation
     */
    public ArtifactIdent getRelocation() {
        return relocation;
    }

    /**
     * Updates the value of the relocation artifact Identifier
     * @param relocation artifactIdent value to update with
     */
    public void setRelocation(ArtifactIdent relocation) {
        this.relocation = relocation;
    }

    /**
     * Retrieves a list of secondary repositories to use to find dependencies
     * @return a list of strings representing the urls to repositories
     */
    public List<String> getRepositories() {
        return repositories;
    }

    /**
     * Updates the list of repositories
     * @param repositories list to update the repositories list with
     */
    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
    }
}
