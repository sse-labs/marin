# Maven Central Research Interface
An interface focused on creating an easy way to index and retrieve data from the Maven Central repository.

## Required Java Version
  The Maven Central Research Interface requires Java 11.

## Usage
To use Maven Central Research Interface extend the Maven Central Analysis class and implement the analyzeArtifact() method with the data that you are trying to extract. From there create an instance of your implementation in main, then build the project, and use the cli described below to run the different configurations.

```java -jar executableName *INSERT CLI HERE* ```

##Example Use Cases:
For these examples, these will perform the analysis on the first 1000 artifacts. The steps are the same for each one, except the implementation being used and the cli arguments added to execute the jar.
1. Set up the implementation of the MavenCentralAnalysis class.
2. Run the executable with the given cli

### Counting all classFiles from jar artifacts:
```
public class ExampleImplementation() extends MavenCentralAnalysis {
    private long numberOfClassfiles;
    
    public ExampleImplementation() {
      super();
      numberOfClassfiles = 0;
    }
    
    @Override
    public void analyzeArtifact(Artifact toAnalyze) {
        if(toAnalyze.getJarInformation() != null) {
          numberOfClassfiles += toAnalyze.getJarInformation().getNumClassFiles();
        }
    }

    public long getNumberOfClassfiles() {
        return numberOfClassfiles;
    }
}  
```

```java -jar executableName --jar -st 0:1000```

### Find all Unique Licenses from pom artifacts
```
public class ExampleImplementation() extends MavenCentralAnalysis {
    private Set<License> uniqueLicenses;

    public ExampleImplementation() {
      super();
      uniqueLicenses = new HashSet<>();
    }  

    @Override
    public void analyzeArtifact(Artifact toAnalyze) {
        if(toAnalyze.getPomInformation() != null) {
          PomInformation info = toAnalyze.getPomInformation();
          if(!info.getRawPomFeatures().getLicenses().isEmpty()) {
              for(License license : info.getRawPomFeatures().getLicenses()) {
                  if(!uniqueLicenses.contains(license)) {
                      uniqueLicenses.add(license);
                  }
              }    
          }
        }
    }

    public Set<License> getUniqueLicenses() {
        return uniqueLicenses;
    }
} 
```
```java -jar executableName --pom false -st 0:1000```

### Collect all artifacts that have javadocs
```
public class ExampleImplementation() extends MavenCentralAnalysis {
    private Set<Artifact> hasJavadocs;

    public ExampleImplementation() {
      super();
      hasJavadocs = new HashSet<>();
    }  

    @Override
    public void analyzeArtifact(Artifact toAnalyze) {
        if(toAnalyze.getIndexInformation() != null) {
          List<Package> packages = toAnalyze.getIndexInformation().getPackages();
          for(Package current : packages) {
            if(current.getJavadocExists() > 0) {
              hasJavadocs.add(toAnalyze);
              break;
            }
          }
        }
    }

    public Set<Artifact> getHasJavadocs() {
        return hasJavadocs;
    }
}  
```

 ```java -jar executableName --index -st 0:1000```

## MavenCentralAnalysis
An abstract class that can be extended to easily run a multitude of analyses on artifacts of the Maven Central repository. This is an encapsulation of the components defined below.

These include:
- skip/take
  - description: Set how many artifacts to skip from the beginning of the index, and how many indexes to attempt to resolve.
  - usage: ```-st skip:take```
- since/until
  - description: Filter the artifact identifiers collected by a given lastModified range.
  - usage: ```-su since:until ```
- coordinates
  - description: Specify a path to a file containing artifact identifiers to resolve. 
  - usage: ```--coordinates path/to/file```
- lastIndexProcessed
  - description: Specify a file path containing which index was last processed, in order to skip already processed indexes
  - usage: ```-ip path/to/file ``` 
- name
  - description: Specify a file path / file name to write the lastIndexProcessed information out to.
  - usage: ```--name path/to/file ```
- index
  - description: Specify whether indexInformation should be collected or just the artifact identifiers.
  - usage: ```--index ```
- pom
  - description: Specify whether to run the pom resolver, with or without transitive dependency resolution.
  - usage: ```--pom bool```
- jar
  - description: Specify whether to run the jar resolver.
  - usage: ```--jar ```
- multi
  - description: Specify to run the multithreaded implementation, and how many threads should be used
  -  usage: ```--multi threads```


## IndexWalker
This part of the interface enables an easy traversal and collection of information from the Maven Central Indexer.

### Data Extracted
For each Artifact these are the attributes that are collected, with some artifacts having more than one package.

- GroupID : ArtifactID : Version
- Name
- Index
- Packages
  - Packaging
  - last Modified
  - Size
  - Sources Exist
  - Javadoc Exist
  - Signature Exist
  - Sha1 Checksum

### Usage
IndexWalker can be implemented into mining software for the iteration and collection of identifiers and other information to be analyzed. From the identifiers collected, a more in-depth analysis can be performed through the POM and JAR files of each artifact.

### Modes
- Normal:
  In this mode all information listed above is collected.
- Lazy:
  In this mode just the identifiers are collected.

### Functions
- Walk all indexes:
    Traverses and retrieves information from all indexes on the Maven Central Repository.
  
- Paginated Walk:
    Traverses and retrieves information from a given index, for a specified number of artifacts.


## Pom Resolver
The pom resolver allows for easy collection of raw pom features and resolution of others through parent and dependency resolution.

### Raw Features
For each Pom file resolved these are the raw features that are collected:
- Parent
- Name
- Description
- Inception Year
- Properties
- Dependencies
- Licenses
- Managed Depedencies

### Parent and Import Resolution
For each pom file that contains a reference to a parent pom or import, those references are also resolved.

### Dependency Resolution
Some dependency versions are not explicitly defined in the current pom file. So Pom resolver includes a dependency resolution algorithm to search through parents and imports to resolve versions.

### Dependency Version Ranges
Other dependencies versions are defined via a version range. An algorithm for resolving these dependencies is also present in the Pom Resolver.

### All Transitive Dependency Resolution
Transitive dependencies are collected without resolving conflicts via the repeated resolution of dependencies.

### Effective Transitive Dependency Resolution
The effective transitive dependencies are resolved via a breadth first traversal of all the transitive dependencies of a given artifact.

### Local Pom Resolution
Pom Resolver also has the capability to resolve local pom files passed by an absolute or relative path.

### Usage
Pom Resolver can be used for varying sizes of pom parsing jobs, with easy access to the data collected. Thus, making it possible to collect information for studies of any size.


## Jar Resolver
The Jar resolver collects information about jar files stored under a given G:A:V artifact Identifier. Utilizing the OPAL framework different information about packages, classes, and methods.

### Extracted Features
- code size
- number of classfiles
- number of methods
- number of packages
- number of fields
- number of virtual methods
- list of classfile objects 

### Usage
The jar resolver makes it easy to run static analysis on any amount of maven central jar artifacts.

