/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package farn.recobbled.installer.gui.installer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import farn.recobbled.installer.gui.ClientTab;
import farn.recobbled.installer.version.LoaderVersion;
import farn.recobbled.installer.version.RecobbledVersion;
import farn.recobbled.installer.util.Utils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public class ClientInstaller {
	public static String install(Path mcDir, String brcVersion, LoaderVersion loaderVersion, ClientTab progress) throws IOException {
		System.out.println("Installing " + brcVersion + " with fabric " + loaderVersion.version);

		String profileName = String.format("%s-%s-%s", "fabric-loader", loaderVersion.version, "brc-" + brcVersion);

		Path versionsDir = mcDir.resolve("versions");
		Path profileDir = versionsDir.resolve(profileName);
		Path profileJson = profileDir.resolve(profileName + ".json");

		if (Files.notExists(profileDir)) {
			Files.createDirectories(profileDir);
		}

		Path profileJar = profileDir.resolve(profileName + ".jar");
		Files.deleteIfExists(profileJar);

		Path vanillaJar = versionsDir.resolve("b1.7.3/b1.7.3.jar");
		if(Files.notExists(vanillaJar)) {
			throw new IOException("Beta 1.7.3 jar file not found!, Please start Beta 1.7.3 once before installing!");
		}

		Path vanillaJson = versionsDir.resolve("b1.7.3/b1.7.3.json");
		if(Files.notExists(vanillaJson)) {
			throw new IOException("Beta 1.7.3 json file not found!, Please start Beta 1.7.3 once before installing!");
		}

		JsonObject mcJson = JsonParser.parseString(Utils.readString(vanillaJson)).getAsJsonObject();

		RecobbledVersion brcMeta = RecobbledVersion.of(brcVersion);

		JsonObject json = modifiedMCJson(mcJson, loaderVersion.getInfo(), profileName);
		Utils.writeJson(json, profileJson.toFile());

		createBRCJar(Files.newInputStream(vanillaJar), brcMeta.downloadJar(), profileJar);
		progress.updateProgress(Utils.BUNDLE.getString("progress.done"));

		return profileName;
	}

	private static JsonObject modifiedMCJson(JsonObject json, LoaderVersion.Info meta, String profileName) {
		json.addProperty("id", profileName);
		json.addProperty("mainClass", meta.mainClass);
		JsonArray libraries = json.getAsJsonArray("libraries");
		libraries.addAll(meta.libraries);
		json.remove("downloads");
		for(JsonElement jElm : libraries.asList()) {
			if(jElm.getAsJsonObject().get("name").getAsString().equals("org.ow2.asm:asm-all:4.1")) {
				libraries.remove(jElm);
				break;
			}
		}

		JsonObject args = new JsonObject();
		args.add("jvm", Utils.jsonArrayOf("-Dfabric.gameVersion=1.0.0-beta.7.3", "-cp", "${classpath}", "-Djava.library.path=${natives_directory}"));
		args.add("game", Utils.jsonArrayOf("${auth_player_name}", "${auth_session}", "--gameDir", "${game_directory}", "--assetsDir", "${game_assets}"));
		json.add("arguments", args);
		json.remove("minecraftArguments");
		return json;
	}

	private static void createBRCJar(InputStream mc, InputStream brc, Path profileJar) throws IOException {
		File mcJar = Utils.placeFile(Utils.TEMP.resolve("temp_minecraft.jar"), mc).toFile();
		File recobbledJar = Utils.placeFile(Utils.TEMP.resolve("temp_recobbled.jar"), brc).toFile();
		File temp = Utils.TEMP.resolve("temp_combined.jar").toFile();
		Files.copy(Utils.createModdedMcJar(mcJar, recobbledJar, temp), profileJar, StandardCopyOption.REPLACE_EXISTING);
	}
}
