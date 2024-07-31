package org.tudo.sse.model.index;

/**
*  The Package represents each package stored under a single GAV.
*  So multiple packages can be stored under a single GAV, so each are stored as a Package object.
*/

public class Package {
    private final String packaging;
    private final long lastModified;
    private final long size;
    private final String sha1checksum;
    private final int sourcesExist;
    private final int javadocExists;
    private final long signatureExists;

    public Package(String packaging, long lastModified, long size, int sourcesExist, int javadocExists, long signatureExists, String sha1checksum) {
        this.lastModified = lastModified;
        this.packaging = packaging;
        this.size = size;
        this.sha1checksum = sha1checksum;
        this.sourcesExist = sourcesExist;
        this.javadocExists = javadocExists;
        this.signatureExists = signatureExists;
    }

    /**
     * Retrieves the last modified value
     * @return long representing the last modified value of the artifact
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Retrieves the numerical sources exist value
     * @return int representing sources exist metadata value
     */
    public int getSourcesExist() {
        return sourcesExist;
    }

    /**
     * Retrieves the numerical javadoc exist value
     * @return int representing source exist metadata value
     */
    public int getJavadocExists() {
        return javadocExists;
    }

    /**
     * Retrieves the numerical signature exist value
     * @return int representing signature exist metadata value
     */
    public long getSignatureExists() {
        return signatureExists;
    }

    /**
     * Retrieves the packaging type of the artifact
     * @return string representing the packaging of the current artifact
     */
    public String getPackaging() {
        return packaging;
    }

    /**
     * Retrieves the size of the current artifact
     * @return long representing the size of the current artifact
     */
    public long getSize() {
        return size;
    }

    /**
     * Retrieves the Sha1checksum of the current artifact
     * @return string representing the Sha1checksum of the current artifact
     */
    public String getSha1checksum() {
        return sha1checksum;
    }
}
