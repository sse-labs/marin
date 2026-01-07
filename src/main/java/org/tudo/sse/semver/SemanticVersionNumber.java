package org.tudo.sse.semver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SemanticVersionNumber implements Comparable<SemanticVersionNumber> {

    private final int majorVersion;
    private final int minorVersion;
    private final int patchVersion;

    private final String preReleaseData;
    private final String buildMetadata;

    private List<PreReleasePart> _parsedPreReleases = null;

    public SemanticVersionNumber(int majorVersion, int minorVersion, int patchVersion) {
        this(majorVersion, minorVersion, patchVersion, null);
    }

    public SemanticVersionNumber(int majorVersion, int minorVersion, int patchVersion, String preReleaseData) {
        this(majorVersion, minorVersion, patchVersion, preReleaseData, null);
    }

    public SemanticVersionNumber(int majorVersion, int minorVersion, int patchVersion, String preReleaseData, String buildMetadata) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;

        this.preReleaseData = preReleaseData;
        this.buildMetadata = buildMetadata;
    }

    public int getMajorVersion(){
        return this.majorVersion;
    }

    public int getMinorVersion(){
        return this.minorVersion;
    }

    public int getPatchVersion(){
        return this.patchVersion;
    }

    public boolean hasPreRelease(){
        return this.preReleaseData != null;
    }
    public boolean hasBuildMetadata(){
        return this.buildMetadata != null;
    }

    public String getPreRelease() {
        return this.preReleaseData;
    }
    public String getBuildMetadata() {
        return this.buildMetadata;
    }

    public final int compareTo(SemanticVersionNumber other){
        // The precedence of semantic version V2 numbers is defined here: https://semver.org/#spec-item-11
        // Note that build metadata is irrelevant to precedence
        // Major version takes precedence
        if(this.majorVersion < other.majorVersion) return -1;
        else if(this.majorVersion > other.majorVersion) return 1;
        else {
            // Minor version is second deciding factor
            if(this.minorVersion < other.minorVersion) return -1;
            else if(this.minorVersion > other.minorVersion) return 1;
            else {
                // Patch version is third deciding factor
                if(this.patchVersion < other.patchVersion) return -1;
                else if(this.patchVersion > other.patchVersion) return 1;
                else {
                    // If major, minor and patch are equal, we proceed as follows:
                    // - if both versions have no prerelease data, they are equal
                    // - if one version has prerelease data and the other has not, prerelease data takes *lower* precedence, i.e. 1.0.0-alpha < 1.0.0
                    if(!this.hasPreRelease() && !other.hasPreRelease()) return 0;
                    else if(!this.hasPreRelease() && other.hasPreRelease()) return 1;
                    else if(this.hasPreRelease() && !other.hasPreRelease()) return -1;
                    else {
                        // If both versions have prerelease data, we must parse this data. SemVer specifies that it may
                        // consist of dot-separated identifiers that are either numeric or textual.
                        List<PreReleasePart> thisParts = this.getParsedPreReleaseParts();
                        List<PreReleasePart> otherParts = other.getParsedPreReleaseParts();

                        // The first difference in prerelease parts decides precedence
                        int pos = 0;
                        while(pos < thisParts.size() && pos < otherParts.size()) {
                            int compareResult = thisParts.get(pos).compareTo(otherParts.get(pos));
                            if(compareResult != 0) return compareResult;
                            pos += 1;
                        }

                        // If we do not find a difference for indices valid in both lists: Fewer parts equal less precedence
                        return thisParts.size() - otherParts.size();
                    }
                }
            }
        }
    }

    private List<PreReleasePart> getParsedPreReleaseParts(){
        if(this._parsedPreReleases == null) {
            this._parsedPreReleases = this.parsePreReleaseData();
        }

        return this._parsedPreReleases;
    }

    private List<PreReleasePart> parsePreReleaseData(){
        List<PreReleasePart> preReleaseParts = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        for(int i = 0; i < preReleaseData.length(); i++){
            char c = preReleaseData.charAt(i);
            if(c == '.'){
                String currValue = current.toString();

                try {
                    int currInt = Integer.parseInt(currValue);
                    preReleaseParts.add(new PreReleasePart(currInt));
                } catch(NumberFormatException ignored){
                    preReleaseParts.add(new PreReleasePart(currValue));
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        String currValue = current.toString();

        try {
            int currInt = Integer.parseInt(currValue);
            preReleaseParts.add(new PreReleasePart(currInt));
        } catch(NumberFormatException ignored){
            preReleaseParts.add(new PreReleasePart(currValue));
        }

        return preReleaseParts;
    }

    /**
     * Class representing a part of a prerelase identifier. The SemVer v2 spec allows prerelease identifiers to be composed
     * of dot-separated parts, which can either be numbers or strings. This class implements their comparison logic
     * in compliance with semantic versioning rules.
     */
    private static final class PreReleasePart implements Comparable<PreReleasePart>{

        private final String stringPart;
        private final Integer intPart;

        PreReleasePart(String value){
            this.stringPart = value;
            this.intPart = null;
        }

        PreReleasePart(int value){
            this.stringPart = null;
            this.intPart = value;
        }

        boolean isNum() {
            return intPart != null;
        }

        int numValue(){
            if(isNum()) return intPart;
            else throw new IllegalStateException("Not a number");
        }

        boolean isString(){
            return stringPart != null;
        }

        String stringValue(){
            if(isString()) return stringPart;
            else throw new IllegalStateException("Not a string");
        }


        @Override
        public int compareTo(PreReleasePart other) {
            // If both parts are numbers, they are compared numerically
            if(this.isNum() && other.isNum()) return this.numValue() - other.numValue();
            // Numeric identifiers have lower precedence than text
            else if(this.isNum()) return -1;
            else if(other.isNum()) return 1;
            else return this.stringValue().compareTo(other.stringValue());
        }
    }

    public static SemanticVersionNumber parse(String value) throws SemanticVersionParsingException {
        return SemanticVersionParser.parseNumber(value);
    }

    public static Optional<SemanticVersionNumber> tryParse(String value) {
        try {
            SemanticVersionNumber number = parse(value);
            return Optional.of(number);
        } catch (SemanticVersionParsingException svpx){
            return Optional.empty();
        }
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(majorVersion);
        sb.append(".");
        sb.append(minorVersion);
        sb.append(".");
        sb.append(patchVersion);

        if(this.hasPreRelease()){
            sb.append("-");
            sb.append(preReleaseData);
        }

        if(this.hasBuildMetadata()){
            sb.append("+");
            sb.append(buildMetadata);
        }

        return sb.toString();
    }
}
