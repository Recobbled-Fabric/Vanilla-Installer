package farn.recobbled.installer.gui.installer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import farn.recobbled.installer.util.Utils;

public class ProfileInstaller {
	private final Path mcDir;

	public ProfileInstaller(Path mcDir) {
		this.mcDir = mcDir;
	}

	public List<LauncherType> getInstalledLauncherTypes() {
		return Arrays.stream(LauncherType.values())
				.filter(launcherType -> Files.exists(mcDir.resolve(launcherType.profileJsonName)))
				.collect(Collectors.toList());
	}

	public void setupProfile(String name, String gameVersion, LauncherType launcherType) throws IOException {
		Path launcherProfiles = mcDir.resolve(launcherType.profileJsonName);

		if (!Files.exists(launcherProfiles)) {
			throw new FileNotFoundException("Could not find " + launcherType.profileJsonName);
		}

		System.out.println("Creating profile");

		JsonObject jsonObject = JsonParser.parseString(Utils.readString(launcherProfiles)).getAsJsonObject();

		JsonObject profiles = jsonObject.getAsJsonObject("profiles");

		if (profiles == null) {
			profiles = new JsonObject();
			jsonObject.add("profiles", profiles);
		}

		String profileName = "fabric-loader-brc" + gameVersion;

		JsonObject profile = profiles.getAsJsonObject(profileName);

		if (profile == null) {
			profile = createProfile(profileName);
			profiles.add(profileName, profile);
		}

		profile.addProperty("lastVersionId", name);

		Utils.writeToFile(launcherProfiles, jsonObject.toString());

		// Create the mods directory
		Files.createDirectories(mcDir.resolve("mods"));
	}

	private static JsonObject createProfile(String name) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("name", name);
		jsonObject.addProperty("type", "custom");
		jsonObject.addProperty("created", Utils.ISO_8601.format(new Date()));
		jsonObject.addProperty("lastUsed", Utils.ISO_8601.format(new Date()));
		jsonObject.addProperty("icon", Utils.getProfileIcon());
		return jsonObject;
	}

	public enum LauncherType {
		WIN32("launcher_profiles.json"),
		MICROSOFT_STORE("launcher_profiles_microsoft_store.json");

		public final String profileJsonName;

		LauncherType(String profileJsonName) {
			this.profileJsonName = profileJsonName;
		}
	}
}
