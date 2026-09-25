package farn.recobbled.installer.version;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import farn.recobbled.installer.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class RecobbledVersion {
    public final String version;
    public final URL url;

    public static final TreeMap<String, RecobbledVersion> versions = new TreeMap<>();
    public static int META_VERSION = 0;

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
        URL MANIFEST_URL = new URI("https://github.com/Recobbled-Fabric/Vanilla-Installer/raw/refs/heads/resource/recobbled_version_v2.json").toURL();
        File cached = Utils.DIR.resolve("recobbled_version_v2.json").toFile();
        JsonObject elm = null;
        boolean needUpdate;
        if(cached.exists()) {
            elm = JsonParser.parseString(Utils.readString(cached.toPath())).getAsJsonObject();
            needUpdate = elm.get("meta_version").getAsInt() != META_VERSION;
        } else {
            needUpdate = true;
        }

        if(needUpdate) {
            try {
                elm = Utils.downloadFileAsJson(MANIFEST_URL, cached.toPath()).getAsJsonObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(elm != null)
            return elm.getAsJsonArray("brc_versions");
        throw  new RuntimeException("No meta version found");
    }
}
