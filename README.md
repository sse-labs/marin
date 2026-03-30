 # MARIN (MAven Research INterface)
An interface focused on creating an accessible and scalable way to do research on artifacts on the Maven Central repository. MARIN contains an overarching implementation of the different modules in the interface, allowing for quick and repeated analysis runs to be performed.

Add the following dependency to your `pom.xml` to add MARIN to your project:
```
<dependency>
    <groupId>eu.sse-labs</groupId>
    <artifactId>marin</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Required Java Version
You will need Java 11 or higher to build MARIN yourself.

## Writing an Analysis
MARIN provides two main classes that can be extended to write custom large-scale analyses:

* `MavenCentralArtifactAnalysis` This class can be extended to write an analysis that processes individual *artifacts* (meaning GAV-triple) hosted on Maven Central. Override the abstract method `void analyzeArtifact(Artifact current)` to define how a single artifact shall be analyzed.
* `MavenCentralLibraryAnalysis` This class can be extended to write analyses that process entire libraries (meaning GA-tuple) hosted on Maven Central at a time. Override the abstract method `void analyzeLibrary(String libraryGA, List<Artifact> releases)` to define how the library shall be analyzed.

Extending both classes requires you to set three boolean values (when invoking the `super` constructor):

* `boolean resolvePom` If set to true, the analysis will automatically download and parse `pom.xml` files, build a `PomInformation` object and annotate `Artifact` objects with it.
* `boolean resolveTransitives` If set to true, the analysis will automatically download and parse all `pom.xml` files that are transitively referenced by the artifact's root `pom.xml` file. These references mainly include `<parent>` relations and import-scope dependencies. The information will be added to the artifact's `PomInformation`.
* `boolean resolveJar` If set to true, the analysis will automatically download and parse the artifact's JAR file, build a `JarInformation` object and annotate `Artifact` objects with it.

Additionally, the `MavenCentralArtifactAnalysis` provides the `resolveIndex` option. If set to true, an `IndexInformation` object will be created based on the information available in Maven Centrals Lucene index.

Both analysis types provide a method called `void runAnalysis(String[] args)`. Invoking this method will start the analysis. By default, MARIN analyses will support the following CLI parameters:

| **Argument**                                               | **Artifact Analysis** | **Library Analysis** | **Description**                                                                                                                                                                                                                                    | **Example**                  |
|------------------------------------------------------------|-----------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|
| `-st <s>:<t>` <br/>`--skip-take <s>:<t>`                   | Yes                   | Yes                  | Skips the first `<s>` inputs and then only processes the next `<t>` ones.                                                                                                                                                                          | `-st 200:10`                 |
| `-su <s>:<u>` <br/> `--since-until <s>:<u>`                | Yes                   | No                   | Skips artifacts released before the  timestamp `<s>` or after timestamp `<u>`. A  timestamp is either a UNIX timestamp (in seconds) or a date of format YYYY-MM-DD. Note that `<u>` is automatically parsed as end-of-day if YYYY-MM-DD is used.   | `-su 1768209126:2026-02-02`  |
| `-i <filepath>`<br/>`--inputs <filepath>`                  | Yes                   | Yes                  | Processes inputs from a file instead of the Maven Central index. Expects on input per line. Inputs are either G:A:V triple (artifacts) or G:A tuple (libraries).                                                                                   | `-i libraries.txt`           |
| `-pof <filepath>`<br/>`--progress-output-file <filepath>`  | Yes                   | Yes                  | File to periodically write number of processed artifacts to. Defaults to `./marin-progress`.                                                                                                                                                       | `-pof marin-progress`        |
| `-prf <filename>`<br/>`--progress-restore-file <filepath>` | Yes                   | Yes                  | File to load a previous run's progress from. Inputs will be skipped until progress is restored. Not used by default.                                                                                                                               | `-prf marin-progress`        |
| `-spi <num>` <br/> `--save-progress-interval <num>`        | Yes                   | Yes                  | Number of inputs after which to store progress. Defaults to 100.                                                                                                                                                                                   | `-spi 10`                    |
| `-t <num>` <br/> `--threads <num>`                         | Yes                   | Yes                  | Number of threads to use. Defaults to 1.                                                                                                                                                                                                           | `-t 8`                       |
| `-o <dir>` <br/> `--output <dir>`                          | Yes                   | No                   | Output directory to optionally write processed artifacts to. Depending on the artifact information required by the analysis, this can be `pom.xml` files, `*.jar` files or GAV triple. Not used by default.                                        | `-o ./jars-processed/`       |


If you do not want to extend one of the aforementioned base classes, MARIN also provides corresponding implementations of the `java.util.Iterator` interface to enumerate and enrich artifacts or libraries. Note that those implementations are **not threadsafe** and cannot perform resolution in parallel. MARIN provides:
* The `MavenCentralArtifactIterator extends Iterator<Artifact>` is the equivalent to the `MavenCentralArtifactAnalysis` and supports all configuration options besides `--threads`.
* The `MavenCentralLibraryIterator extends Iterator<LibraryResolutionContext>` is the equivalent to the `MavenCentralLibraryAnalysis` and supports all configuration options besides `--threads`.

## Usage
When using MARIN, you can decide whether to extend one of the  abstract base classes available, or to rely on of the analysis-equivalent implementations of `java.util.Iterator`.

### Extending Abstract Base Classes
To use MARIN, you will need to implement two components:
1. You need an implementation of either the abstract class `MavenCentralArtifactAnalysis` or `MavenCentralLibraryAnalysis`. This is your actual analysis implementation that defines how a single artifact or library shall be processed.
2. You need a runner class that passes command line arguments to your analysis implementation. Usually, this will look like this:
```java
public class AnalysisRunner {

  public static void main(String[] args){
    MavenCentralAnalysis myAnalysis = new MyAnalysisImplementation();

    try {
      myAnalysis.runAnalysis(args);
    } catch (Exception e) {
      System.err.println("Error while running Analysis: " + e.getMessage());
    }
    
  }

}
```
Once this is implemented, you can run your analysis using the following command. Any of the above-mentioned CLI arguments will work for your analysis implementation.
```java -jar executableName *INSERT CLI HERE* ```

### Using Iterators
As opposed to extending a base class, in order to use MARIN's iterator implementations you will have to first create an analysis configuration object programmatically. This can be done using the `ArtifactAnalysisConfigBuilder` or `LibraryAnalysisConfigBuilder` classes, respectively. The following example shows how to first build a configuration, and then use it to initialize a `MavenCentralArtifactIterator`.
```java
final boolean resolvePom = true;
final boolean resolveTransitive = false;
final boolean resolveJar = true;
final Path gavInputList = Paths.get("path-to-input-file");

final ArtifactAnalysisConfig config = new ArtifactAnalysisConfigBuilder()
                .withInputList(gavInputList)
                .withSkip(2)
                .withTake(5)
                .build();

MavenCentralArtifactIterator iterator = new MavenCentralArtifactIterator(resolvePom, resolveTransitive, resolveJar, config);

while(iterator.hasNext()){
    Artifact current = iterator.next();
    // TODO: Process artifact
}
```

## Example Use Cases:
In the following, there are some example implementations of `MavenCentralArtifactAnalysis`. All of them can be used with the same `AnalysisRunner` implementation seen above, just replace `MyAnalysisImplementation` with the actual implementation name.
You can run each example on the first 1000 Maven artifacts by invoking `java -jar executableName -st 0:1000` for the respective project executable JAR.

### Counting all classFiles from jar artifacts:
``` java
public class ClassFileCountImplementation extends MavenCentralArtifactAnalysis {

    private long numberOfClassfiles;

    public ClassFileCountImplementation() {
        super(false, false, false, true);
        this.numberOfClassfiles = 0;
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
public class LicenseImplementation extends MavenCentralArtifactAnalysis {
    private final Set<License> uniqueLicenses;

    public LicenseImplementation() {
        super(false, true, false, false);
        this.uniqueLicenses = new HashSet<>();
    }

    @Override
    public void analyzeArtifact(Artifact toAnalyze) {
        if(toAnalyze.getPomInformation() != null) {
            PomInformation info = toAnalyze.getPomInformation();
            if(!info.getRawPomFeatures().getLicenses().isEmpty()) {
                for(License license : info.getRawPomFeatures().getLicenses()) {
                    uniqueLicenses.add(license);
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
public class JavaDocImplementation extends MavenCentralArtifactAnalysis {

    private final Set<Artifact> hasJavadocs;

    public JavaDocImplementation() {
        super(true, false, false, false);
        this.hasJavadocs = new HashSet<>();
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

## Logging
MARIN relies on `slf4j-api` for logging purposes. You will need an SLF4J backend like `ch.qos.logback:logback-classic` on your path to enable and configure logging.
Note that MARIN uses Apache Pekko Actors for multithreaded processing. If your analysis implementations perform a lot of logging operations, we suggest you [use an async appender](https://pekko.apache.org/docs/pekko/current/typed/logging.html#logback) as to not block multithreaded execution.
The following example for `logback.xml` demonstrates how to set up an asnyc appender that logs to the console:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[%date{ISO8601}] [%level] [%logger] - %msg %n</pattern>
        </encoder>
    </appender>
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>8192</queueSize>
        <neverBlock>true</neverBlock>
        <appender-ref ref="CONSOLE" />
    </appender>
    <root level="INFO">
        <appender-ref ref="ASYNC"/>
    </root>
</configuration>
```
Depending on your implementation's logging output and logback queue size for, some log messages may be dropped.

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

