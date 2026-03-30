package org.tudo.sse.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.tudo.sse.model.pom.RawPomFeatures;

public class JsonExporter {

    public static JsonObject exportRawPomFeatures(RawPomFeatures features) {
        JsonObject json = new JsonObject();

        String parent = "null";
        if(features.getParent() != null){
            parent = features.getParent().getCoordinates();
        }

        json.add("parent", new JsonPrimitive(parent));

        String name = "null";
        if(features.getName() != null){
            name = features.getName();
        }

        json.add("name", new JsonPrimitive(name));

        String description = "null";
        if(features.getDescription() != null){
            description = features.getDescription();
        }

        json.add("description", new JsonPrimitive(description));

        String url = "null";
        if(features.getUrl() != null){
            url = features.getUrl();
        }

        json.add("url", new JsonPrimitive(url));

        String packaging = "null";
        if(features.getPackaging() != null){
            packaging = features.getPackaging();
        }

        json.add("packaging", new JsonPrimitive(packaging));

        String inceptionYear = "null";
        if(features.getInceptionYear() != null){
            inceptionYear = features.getInceptionYear();
        }

        json.add("inceptionYear", new JsonPrimitive(inceptionYear));

        if(!features.getProperties().isEmpty()){
            JsonArray properties = new JsonArray(features.getProperties().size());
            features.getProperties().forEach((k,v)->{
                JsonArray propArray = new JsonArray(2);
                propArray.add(new JsonPrimitive(k));
                propArray.add(new JsonPrimitive(v));
                properties.add(propArray);
            });
            json.add("properties", properties);
        }

        JsonArray dependencies = new JsonArray();
        features.getDependencies().forEach(dependency -> dependencies.add(new JsonPrimitive(dependency.getIdent().getCoordinates())));

        json.add("dependencies", dependencies);

        JsonArray licenses = new JsonArray();
        features.getLicenses().forEach(license -> licenses.add(new JsonPrimitive(license.getUrl())));
        json.add("licenses", licenses);

        if(features.getDependencyManagement() != null){
            JsonArray dependencyManagement = new JsonArray();
            features.getDependencyManagement().forEach(dependency -> dependencyManagement.add(new JsonPrimitive(dependency.getIdent().getCoordinates())));
            json.add("dependencyManagement", dependencyManagement);
        } else {
            json.add("dependencyManagement", new JsonPrimitive("null"));
        }

        return json;
    }
}
