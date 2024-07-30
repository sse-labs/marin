package org.tudo.sse.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.pom.Dependency;

import java.util.List;

public class PomResolverAnalysis {
    private static final Logger log = LogManager.getLogger(PomResolverAnalysis.class);

    static long parent = 0;
    static long name = 0;
    static long description = 0;
    static long url = 0;
    static long packaging = 0;
    static long inceptionYear = 0;
    static long dependencies = 0;
    static long licenses = 0;


    public static void checkPomFields(Model model) {
        if(model.getParent() != null) {
            parent++;
        }

        if(model.getName() != null) {
            name++;
        }

        if(model.getDescription() != null) {
           description++;
        }

        if(model.getUrl() != null) {
            url++;
        }

        if(model.getPackaging() != null) {
            packaging++;
        }

        if(model.getInceptionYear() != null) {
            inceptionYear++;
        }

        if(!model.getDependencies().isEmpty()) {
            dependencies++;
        }

        if(!model.getLicenses().isEmpty()) {
            licenses++;
        }
    }

    public static void checkStats(double numOfIdents) {
        log.info("Percentage of Parents present: " + (parent / numOfIdents) * 100);
        log.info("Percentage of Names present: " + (name / numOfIdents) * 100);
        log.info("Percentage of Descriptions present: " + (description / numOfIdents) * 100);
        log.info("Percentage of urls present: " + (url / numOfIdents) * 100);
        log.info("Percentage of Packaging present: " + (packaging / numOfIdents) * 100);
        log.info("Percentage of Inception Years present: " + (inceptionYear / numOfIdents) * 100);
        log.info("Percentage of Dependencies present: " + (dependencies / numOfIdents) * 100);
        log.info("Percentage of Licenses present: " + (licenses / numOfIdents) * 100);
    }


    public static void dependencyResolutionRate(List<Artifact> poms) {
        int totalDependencies = 0;
        int totalResolved = 0;

        int i = 0;
        for(org.tudo.sse.model.Artifact pom : poms) {
            if(pom != null) {
                if(pom.getPomInformation().getResolvedDependencies() != null) {
                    for(Dependency depend : pom.getPomInformation().getResolvedDependencies()) {
                        if(depend.getIdent().version != null && !depend.getIdent().getCoordinates().contains("${")) {
                            totalResolved++;
                        } else {

                            log.info("Artifact number " + i + " " + pom.getIdent().getCoordinates() + " wasn't able to resolve " + depend.getIdent().getCoordinates());
                        }
                        totalDependencies++;
                    }
                }
                i++;
            }
        }

        double percent = (totalResolved / (double) totalDependencies) * 100;

        log.info("Out of {} total dependencies, {} were resolved.", totalDependencies, totalResolved);
        log.info("{} % of dependencies were resolved", percent);
    }

}
