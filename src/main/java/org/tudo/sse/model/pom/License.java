package org.tudo.sse.model.pom;

import java.util.Objects;

/**
 * This class holds license information that can be found in pom files.
 */
public class License {
    private final String name;
    private final String url;

    /**
     * Creates a new License object with the given name and URL.
     * @param name The license name
     * @param url The license's URL
     */
    public License(String name, String url) {
        this.name = name;
        this.url = url;
    }

    /**
     * Gets the name of the license.
     * @return license name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the url to the license
     * @return license url
     */
    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        License that = (License) o;
        return Objects.equals(that.url, url) &&
                Objects.equals(that.name, name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }

}
