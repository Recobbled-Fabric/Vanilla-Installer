package farn.recobbled.installer.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class LoaderVersion {
    public final String version;
    public final String url;
    private Info info;

    public static final Map<String, LoaderVersion> verToLink = new LinkedHashMap<>();

    public LoaderVersion(String version, String url) {
        this.version = version;
        this.url = url;
    }

    public Info getInfo() {
        if (info == null) info = new Info(version, url);
        return info;
    }

    public static class Info {
        public final String mainClass;
        public final JsonArray libraries;

        private Info(String flVer, String mavenLink) {
            try {
                URL jsonUrl = new URI(mavenLink).toURL();
                JsonObject json = JsonParser.parseReader(new InputStreamReader(jsonUrl.openStream())).getAsJsonObject();

                mainClass = json.getAsJsonObject("mainClass").get("client").getAsString();

                libraries = json.getAsJsonObject("libraries").getAsJsonArray("common").deepCopy();
                JsonObject loaderLib = new JsonObject();
                loaderLib.addProperty("name", "net.fabricmc:fabric-loader:" + flVer);
                loaderLib.addProperty("url", "https://maven.fabricmc.net/");
                libraries.add(loaderLib);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static {
        try {
            List<LoaderVersion> loaderList = new ArrayList<>();
            URL fabricMeta = new URI("https://meta.fabricmc.net/v2/versions/loader").toURL();
            JsonArray metaJson = JsonParser.parseReader(new InputStreamReader(fabricMeta.openStream())).getAsJsonArray();
            for(int index = 0; index < metaJson.size(); ++index) {
                JsonObject entry = metaJson.get(index).getAsJsonObject();
                String mavenLink = toUrl(entry.getAsJsonObject().get("maven").getAsString());
                String version = entry.getAsJsonObject().get("version").getAsString();
                loaderList.add(new LoaderVersion(version, mavenLink));
            }
            loaderList.forEach(loader -> verToLink.put(loader.version, loader));
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toUrl(String coordinate) {
        String[] parts = Objects.requireNonNull(coordinate, "Coordinate cannot be null").split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid Maven coordinate format. Expected 'groupId:artifactId:version'");
        }
        String groupPath = parts[0].replace('.', '/');

        String ext = (parts.length == 4) ? parts[3] : "json";

        return String.format("%s%s/%s/%s/%s-%s.%s", "https://maven.fabricmc.net/", groupPath, parts[1], parts[2], parts[1], parts[2], ext);
    }

    public static LoaderVersion get(String flVer) {
        return verToLink.get(flVer);
    }
}
