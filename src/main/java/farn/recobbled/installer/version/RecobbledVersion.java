package farn.recobbled.installer.version;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import farn.recobbled.installer.util.LinkReference;
import farn.recobbled.installer.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class RecobbledVersion {
    public final String version;
    public final URL url;

    public static final TreeMap<String, RecobbledVersion> versions = new TreeMap<>();

    private RecobbledVersion(String version, String url) {
        try {
            this.version = version;
            this.url = new URI(url).toURL();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream downloadJar() {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RecobbledVersion of(String version) {
        return versions.get(version);
    }

    static {
        try {
            List<JsonElement> manifestJson = getRecobbledMetaJson().asList();
            for (JsonElement jsonElement : manifestJson) {
                JsonObject entry = jsonElement.getAsJsonObject();
                String version = entry.get("id").getAsString();
                String url = entry.get("url").getAsString();
                versions.put(version, new RecobbledVersion(version, url));
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonArray getRecobbledMetaJson() throws IOException, URISyntaxException {
        URL MANIFEST_URL = new URI(LinkReference.RECOBBLED_MANIFEST).toURL();
        String jsonStr = null;

        try {
            InputStream in = MANIFEST_URL.openStream();
            jsonStr = Utils.readString(in);
        } catch (Exception e) {
            throw new RuntimeException();
        }

        JsonObject manifestJson = JsonParser.parseString(jsonStr).getAsJsonObject();
        return manifestJson.getAsJsonArray("brc_versions");
    }
}
