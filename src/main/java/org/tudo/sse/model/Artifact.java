package org.tudo.sse.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tudo.sse.model.index.IndexInformation;
import org.tudo.sse.model.jar.*;
import org.tudo.sse.model.pom.PomInformation;
import org.tudo.sse.resolution.JarResolutionException;
import org.tudo.sse.resolution.JarResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds all the artifact information. For each artifact index, jar, and pom information can be defined.
 * Two identifiers are also defined as some artifact are relocated under new identifiers.
 */
public class Artifact {
    /**
     * The identifier object for the artifact.
     */
    public final ArtifactIdent ident;
    private static final Logger log = LogManager.getLogger(Artifact.class);

    /**
     * A secondary identifier, for if its pom information has been moved on the maven central repository.
     */
    public ArtifactIdent relocation;
    private IndexInformation indexInformation;
    private PomInformation pomInformation;
    private JarInformation jarInformation;

    /**
     * Creates a new artifact based on given IndexInformation. This artifact will have no POM or JAR information
     * associated.
     *
     * @param indexInformation The IndexInformation for which to create the artifact
     */
    public Artifact(IndexInformation indexInformation) {
        this.indexInformation = indexInformation;
        this.ident = indexInformation.getIdent();
        pomInformation = null;
        jarInformation = null;
    }

    /**
     * Creates a new artifact based on given PomInformation. This artifact will have no Index or JAR information
     * associated.
     *
     * @param pomInformation The PomInformation for which to create the artifact
     */
    public Artifact(PomInformation pomInformation) {
        this.pomInformation = pomInformation;
        this.ident = pomInformation.getIdent();
        this.relocation = pomInformation.getRelocation();
        indexInformation = null;
        jarInformation = null;
    }

    /**
     * Creates a new artifact based on the given JarInformation. This artifact will have no Index or POM information
     * associated.
     *
     * @param jarInformation The JarInformation for which to create the artifact
     */
    public Artifact(JarInformation jarInformation) {
        this.jarInformation = jarInformation;
        this.ident = jarInformation.getIdent();
        indexInformation = null;
        pomInformation = null;
    }


    /**
     * Returns the artifact identifier for this artifact.
     *
     * @return ArtifactIdent object for this artifact
     */
    public ArtifactIdent getIdent() {
        return ident;
    }

    /**
     * Returns the artifact identifier of a potential relocation for this artifact. If this artifact has not been
     * relocated, null is returned.
     *
     * @return ArtifactIdent identifying the relocation of this artifact, or null
     */
    public ArtifactIdent getRelocation() {
        return relocation;
    }

    /**
     * Returns the IndexInformation of this artifact, or null if no IndexInformation is provided.
     * @return IndexInformation of this artifact, or null
     */
    public IndexInformation getIndexInformation() {
        return indexInformation;
    }

    /**
     * Checks whether this artifact provides IndexInformation.
     * @return True if IndexInformation is available, false otherwise
     */
    public boolean hasIndexInformation() {
        return indexInformation != null;
    }

    /**
     * Sets the IndexInformation for this artifact.
     * @param indexInformation The IndexInformation to associated with this artifact
     */
    public void setIndexInformation(IndexInformation indexInformation) {
        this.indexInformation = indexInformation;
    }

    /**
     * Returns the PomInformation of this artifact, or null if no PomInformation is provided.
     * @return PomInformation of this artifact, or null
     */
    public PomInformation getPomInformation() {
        return pomInformation;
    }

    /**
     * Checks whether this artifact provides PomInformation.
     * @return True if PomInformation is available, false otherwise
     */
    public boolean hasPomInformation() {
        return pomInformation != null;
    }

    /**
     * Sets the PomInformation for this artifact.
     * @param pomInformation The PomInformation to associated with this artifact
     */
    public void setPomInformation(PomInformation pomInformation) {
        this.pomInformation = pomInformation;
    }

    /**
     * Returns the JarInformation of this artifact, or null if no JarInformation is provided.
     * @return JarInformation of this artifact, or null
     */
    public JarInformation getJarInformation() {
        return jarInformation;
    }

    /**
     * Checks whether this artifact provides JarInformation.
     * @return True if JarInformation is available, false otherwise
     */
    public boolean hasJarInformation(){
        return jarInformation != null;
    }

    /**
     * Sets the JarInformation for this artifact.
     * @param jarInformation The JarInformation to associated with this artifact
     */
    public void setJarInformation(JarInformation jarInformation) {
        this.jarInformation = jarInformation;
    }


    /**
     * Builds the Type hierarchy for this artifact and returns a map of type FQNs to their hierarchy node. Only works
     * if JarInformation is available.
     *
     * @return Map of Type FQNs to their hierarchy node
     */
    public Map<String, ClassFileNode> buildTypeStructure() {
        Map<String, ClassFileNode> roots = new HashMap<>();
        roots.put("java/lang/Object", new VirtualClassFileNode(new ObjType(0, "java/lang/Object", "java/lang")));
        if(jarInformation != null) {
            Map<String, List<ClassFile>> packages = jarInformation.getPackages();
            Map<String, Artifact> depArts = new HashMap<>();

            if(pomInformation != null) {
                JarResolver resolver = new JarResolver();
                for(Artifact artifact : pomInformation.getEffectiveTransitiveDependencies()) {
                    try {
                        artifact.setJarInformation(resolver.parseJar(artifact.getIdent()).getJarInformation());
                        depArts.put(artifact.getIdent().getGroupID() + ":" + artifact.getIdent().getArtifactID(), artifact);
                    } catch (JarResolutionException e) {
                        log.error(e);
                    }
                }
            }

            for(Map.Entry<String, List<ClassFile>> classes : packages.entrySet()) {
                for(ClassFile clase : classes.getValue()) {
                    resolveNode(roots, clase, packages, depArts);
                }
            }
        }
        return roots;
    }

    private ClassFileNode resolveNode(Map<String, ClassFileNode> root, ClassFile clase, Map<String, List<ClassFile>> packages, Map<String, Artifact> depArts) {
            ClassFileNode node = new DefinedClassFileNode(clase.getAccessFlags(), clase.getThistype(), clase.getVersion());

            if(clase.getSuperType() != null) {
               resolveSuperClass(root, node, clase, packages, depArts);
            }

            if(!clase.getInterfaceTypes().isEmpty()) {
                resolveInterfaces(root, node, clase, packages, depArts);
            }

            return node;
    }

    private void resolveSuperClass(Map<String, ClassFileNode> roots, ClassFileNode node, ClassFile clase, Map<String, List<ClassFile>> packages, Map<String, Artifact> depArts) {
        String packName = clase.getSuperType().getPackageName();

        //Check if the superclass can be found in the root map
        if(roots.containsKey(clase.getSuperType().getFqn())) {
            node.setSuperClass(roots.get(clase.getSuperType().getFqn()));
            roots.get(clase.getSuperType().getFqn()).addChild(node);
        }
        //Check if
        else if(packages.containsKey(packName)) {
            List<ClassFile> toLookThrough = packages.get(packName);
            for(ClassFile cls : toLookThrough) {
                if(cls.getThistype().getFqn().equals(clase.getSuperType().getFqn())) {
                    node.setSuperClass(resolveNode(roots, cls, packages, depArts));
                    node.getSuperClass().addChild(node);
                    break;
                }
            }
        } else {
            boolean found = false;

            for(Map.Entry<String, Artifact> entry : depArts.entrySet()) {
                if(entry.getValue().getJarInformation().getPackages().containsKey(packName)) {
                    for(ClassFile depClass : entry.getValue().getJarInformation().getPackages().get(packName)) {
                        if(depClass.getThistype().getFqn().equals(clase.getSuperType().getFqn())) {
                            found = true;
                            node.setSuperClass(resolveNode(roots, depClass, entry.getValue().getJarInformation().getPackages(), depArts));
                            node.getSuperClass().addChild(node);
                            break;
                        }
                    }
                    if(found) {
                        break;
                    }
                }
            }

            //add a new root to the map
            if(!found) {
                node.setSuperClass(new VirtualClassFileNode(clase.getSuperType()));
                node.getSuperClass().addChild(node);
                roots.put(node.getSuperClass().getThisType().getFqn(), node.getSuperClass());
            }
        }
    }

    private void resolveInterfaces(Map<String, ClassFileNode> roots, ClassFileNode node, ClassFile clase, Map<String, List<ClassFile>> packages, Map<String, Artifact> depArts) {
        for(ObjType itfe : clase.getInterfaceTypes()) {
            String packName = itfe.getPackageName();

            if(packages.containsKey(packName)) {
                List<ClassFile> toLookThrough = packages.get(packName);
                for(ClassFile cls : toLookThrough) {
                    if(cls.getThistype().getFqn().equals(itfe.getFqn())) {
                        ClassFileNode resolved = resolveNode(roots, cls, packages, depArts);
                        resolved.addChild(node);
                        ((DefinedClassFileNode) node).addInterfaceNode(resolved);
                        break;
                    }
                }
            } else {
                boolean found = false;

                for(Map.Entry<String, Artifact> entry : depArts.entrySet()) {
                    if(entry.getValue().getJarInformation().getPackages().containsKey(packName)) {
                        for(ClassFile depClass : entry.getValue().getJarInformation().getPackages().get(packName)) {
                            if(depClass.getThistype().getFqn().equals(itfe.getFqn())) {
                                found = true;
                                ClassFileNode resolved = resolveNode(roots, depClass, entry.getValue().getJarInformation().getPackages(), depArts);
                                resolved.addChild(node);
                                ((DefinedClassFileNode) node).addInterfaceNode(resolved);
                                break;
                            }
                        }
                        if(found) {
                            break;
                        }
                    }
                }

                if(!found) {
                    ClassFileNode notFound = new VirtualClassFileNode(itfe);
                    ((DefinedClassFileNode) node).addInterfaceNode(notFound);
                    notFound.addChild(node);
                }
            }
        }
    }

}
