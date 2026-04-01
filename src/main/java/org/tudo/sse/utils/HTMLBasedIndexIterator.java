package org.tudo.sse.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.resolution.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Stack;

/**
 * An iterator over all artifacts in a maven repository that is based on parsing the HTML pages of the repository.
 * This is a fallback solution for repositories that do not provide an index or if the index is not accessible for some reason.
 */
public class HTMLBasedIndexIterator implements Iterator<ArtifactIdent> {


    private final Stack<String> queue = new Stack<>();
    private final Stack<ArtifactIdent> foundGAVs = new Stack<>();
    private final String baseURL;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public HTMLBasedIndexIterator(String baseUrl) {
        baseURL = baseUrl;
        queue.push("");
    }

    /**
     * Finds all URLs on the page of the given local part of the url (e.g. "com/example/library/1.0/") and
     * adds them to the queue if they are directories or to the list of found GAVs if they are artifact files.
     *
     * @param url local part of the URL to find URLs on (e.g. "com/example/library/1.0/")
     */
    private void findURLs(String url) throws IOException {
        try (InputStream content = ResourceConnections.openInputStream(new URI(baseURL + url))) {
            if (content != null) {
                Document doc = Jsoup.parse(content, "UTF-8", "https://www.example.com");
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String href = link.attr("href");
                    if (href.isBlank() || href.contains("../")) continue;
                    if (href.endsWith("/")) {
                        queue.push(url + href);
                    } else if (href.endsWith(".pom") || href.endsWith(".jar")) {
                        foundGAVs.add(urlToArtifactIdent(url + href));
                        //There is maximally one artifact per directory, so we can stop after finding one
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            log.error("Could not MAVEN Repository URL {}: {}", url, e.getMessage());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts the local part of a  URL of a maven artifact (e.g. "com/example/library/1.0/library-1.0.pom") to an artifact identifier
     *
     * @param url local part of URL of a maven artifact (e.g. "com/example/library/1.0/library-1.0.pom")
     * @return An artifact identifier represented by the given URL
     * @throws IllegalArgumentException If the given URL does not contain enough parts to identify an artifact
     */
    private ArtifactIdent urlToArtifactIdent(String url) {
        String[] parts = url.split("/");
        if (parts.length < 3)
            throw new IllegalArgumentException("URL does not contain enough parts to be identify an artifact");
        String version = parts[parts.length - 2];
        String artifactId = parts[parts.length - 3];
        StringBuilder groupId = new StringBuilder();
        for (int i = 0; i < parts.length - 3; i++) {
            groupId.append(parts[i]);
            if (i < parts.length - 4) groupId.append(".");
        }
        ArtifactIdent ident = new ArtifactIdent(groupId.toString(), artifactId, version);
        ident.setRepository(baseURL);
        return ident;
    }

    @Override
    public boolean hasNext() {
        while (foundGAVs.isEmpty()) {
            if (queue.isEmpty()) return false;
            try {
                findURLs(queue.pop());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    @Override
    public ArtifactIdent next() {
        if (!hasNext()) throw new IllegalStateException("No libraries left on iterator");
        return foundGAVs.pop();
    }

    public static void main(String[] args) {
        HTMLBasedIndexIterator iterator = new HTMLBasedIndexIterator("https://maven.repository.redhat.com/ga/");
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
    }
}