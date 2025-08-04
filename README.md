 # MARIN (MAven Research INterface)
An interface focused on creating an accessible and scalable way to do research on artifacts on the Maven Central repository. MARIN contains an overarching implementation of the different modules in the interface, allowing for quick and repeated analysis runs to be performed. 

## Required Java Version
The Maven Research Interface requires Java 11.

## MavenCentralAnalysis
An abstract class that can be extended to easily run a multitude of analyses on artifacts of the Maven Central repository. Boolean values are used to control which type of information to collect (index, pom, jar) and a cli is in place to configure other aspects of the run.

The boolean values to be set include:
- index: sets if metadata from the Maven Central Index should be collected
- pom: sets if pom artifacts are to be resolved
- transitive: sets if transitive dependencies should be resolved if pom artifacts are also being resolved
- jar: sets if jar artifacts are to be resolved

The CLI includes the following:
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
- output
  - description: Specify whether to write files that resolution is being performed on out to a directory.
  - usage: ```--output path/to/dir ```
- multi
  - description: Specify to run the multithreaded implementation, and how many threads should be used
  -  usage: ```--multi threads```

## Usage
To use MARIN, you will need to implement two components:
1. You need an implementation of the abstract class `MavenCentralAnalysis` that defines the `void analyzeArtifact(Artifact toAnalyze)` method. This is your actual analysis implementation that defines how a single artifact shall be processed.
2. You need a runner class that passes command line arguments to your analysis implementation. Usually, this will look like this:
```java
public class AnalysisRunner {

  public static void main(String[] args){
    MavenCentralAnalysis myAnalysis = new MyAnalysisImplementation();
    myAnalysis.runAnalysis(args);
  }

}
```
Once this is implemented, you can run your analysis using the following command. Any of the above-mentioned CLI arguments will work for your analysis implementation.
```java -jar executableName *INSERT CLI HERE* ```

## Example Use Cases:
In the following, there are some example implementations of `MavenCentralAnalaysis`. All of them can be used with the same `AnalysisRunner` implementation seen above, just replace `MyAnalysisImplementation` with the actual implementation name.
You can run each example on the first 1000 Maven artifacts by invoking `java -jar executableName -st 0:1000` for the respective project executable JAR.

### Counting all classFiles from jar artifacts:
``` java
public class ExampleImplementation extends MavenCentralAnalysis {
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

### Find all Unique Licenses from pom artifacts
``` java
public class ExampleImplementation extends MavenCentralAnalysis {
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

### Collect all artifacts that have javadocs
``` java
public class ExampleImplementation extends MavenCentralAnalysis {
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

## IndexWalker
This part of the interface enables an easy traversal and collection of information from the Maven Central Index. This relies on the IndexIterator which traverses the index storing the values of artifacts with the same identifier in a single artifact objects as a list of packages (representation of each unique artifact under the same identifier).

### Data Extracted
For each Artifact these are the attributes that are collected:

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
The pom resolver allows for easy collection of raw pom features and resolved features. The raw pom features are collected using the Maven-Model library by apache, these features are resolved by collecting parent and import poms, and using them in resolution to find any implicitly defined values.

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

