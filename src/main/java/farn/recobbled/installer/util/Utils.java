package farn.recobbled.installer.util;

import com.google.gson.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Utils {
	public static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	public static final Path DIR = findDefaultInstallDir().resolve("brc_fabric");
	public static final Path TEMP = DIR.resolve("temp");

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("lang/installer", Locale.getDefault(), new ResourceBundle.Control() {
		@Override
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
			final String bundleName = toBundleName(baseName, locale);
			final String resourceName = toResourceName(bundleName, "properties");

			try (InputStream stream = loader.getResourceAsStream(resourceName)) {
				if (stream != null) {
					try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
						return new PropertyResourceBundle(reader);
					}
				}
			}

			return super.newBundle(baseName, locale, format, loader, reload);
		}
	});

	public static Path findDefaultInstallDir() {
		Path dir;

		if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS && System.getenv("APPDATA") != null) {
			dir = Paths.get(System.getenv("APPDATA")).resolve(".minecraft");
		} else {
			String home = System.getProperty("user.home", ".");
			Path homeDir = Paths.get(home);

			if (OperatingSystem.CURRENT == OperatingSystem.MACOS) {
				dir = homeDir.resolve("Library").resolve("Application Support").resolve("minecraft");
			} else {
				dir = homeDir.resolve(".minecraft");

				if (OperatingSystem.CURRENT == OperatingSystem.LINUX && !Files.exists(dir)) {
					// https://github.com/flathub/com.mojang.Minecraft
					final Path flatpack = homeDir.resolve(".var").resolve("app").resolve("com.mojang.Minecraft").resolve(".minecraft");

					if (Files.exists(flatpack)) {
						dir = flatpack;
					}
				}
			}
		}

		return dir.toAbsolutePath().normalize();
	}

	public static String readString(Path path) throws IOException {
		return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
	}
	public static String readString(InputStream stream) throws IOException {
		return new String(readAll(stream), StandardCharsets.UTF_8);
	}


	public static void writeToFile(Path path, String string) throws IOException {
		Files.write(path, string.getBytes(StandardCharsets.UTF_8));
	}

	private static final int HTTP_TIMEOUT_MS = 8000;

	private static InputStream openUrl(URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setConnectTimeout(HTTP_TIMEOUT_MS);
		conn.setReadTimeout(HTTP_TIMEOUT_MS);
		conn.connect();

		int responseCode = conn.getResponseCode();
		if (responseCode < 200 || responseCode >= 300) throw new IOException("HTTP request to "+url+" failed: "+responseCode);

		return conn.getInputStream();
	}

	public static String getProfileIcon() {
		try (InputStream is = Utils.class.getClassLoader().getResourceAsStream("profile_icon.png")) {
			byte[] ret = new byte[4096];
			int offset = 0;
			int len;

			while ((len = is.read(ret, offset, ret.length - offset)) != -1) {
				offset += len;
				if (offset == ret.length) ret = Arrays.copyOf(ret, ret.length * 2);
			}

			return "data:image/png;base64," + Base64.getEncoder().encodeToString(Arrays.copyOf(ret, offset));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "TNT"; // Fallback to TNT icon if we cant load Fabric icon.
	}

	private static MessageDigest sha1Digest() {
		try {
			return MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Something has gone really wrong", e);
		}
	}

	public static JsonArray jsonArrayOf(String... strs) {
		JsonArray args = new JsonArray();
		for (String str : strs) {
			args.add(str);
		}
		return args;
	}

	public static void writeJson(JsonObject json, File file) {
		try(FileWriter fw = new FileWriter(file)) {
			GSON.toJson(json, fw);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Path createModdedMcJar(File minecraftJar, File modJar, File output) {
		List<Closeable> closeables = new ArrayList<>();

		ZipOutputStream out;

		List<File> files = new ArrayList<>();
		files.add(minecraftJar);
		files.add(modJar);

		try {
			out = new ZipOutputStream(Files.newOutputStream(output.toPath()));
			closeables.add(out);

			Set<String> addedEntries = new HashSet<>();

			for(int i = files.size() - 1; i >= 0; i--) {
				File file = files.get(i);
				ZipFile zip = new ZipFile(file);
				closeables.add(zip);

				Enumeration<? extends ZipEntry> entries = zip.entries();

				while(entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String entryName = entry.getName();

					if(i == 0 && entryName.startsWith("META-INF")) {
						continue;
					}

					if(addedEntries.contains(entryName)) {
						continue;
					}

					byte[] allBytes = readAll(zip.getInputStream(entry));
					out.putNextEntry(new ZipEntry(entryName));
					out.write(allBytes);
					addedEntries.add(entryName);
				}
			}
		}catch (Exception e) {
			throw new RuntimeException(e);
		}finally {
			for(int i=0; i < closeables.size(); i++) {
				try {
					closeables.get(i).close();
				}catch (Exception e) {}
			}
		}
		return output.toPath();
	}

	public static byte[] readAll(InputStream inputStream) throws IOException {
		final int bufLen = 1024;
		byte[] buf = new byte[bufLen];
		int readLen;
		IOException exception = null;

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
				outputStream.write(buf, 0, readLen);

			return outputStream.toByteArray();
		} catch (IOException e) {
			exception = e;
			throw e;
		} finally {
			if (exception == null) inputStream.close();
			else try {
				inputStream.close();
			} catch (IOException e) {
				exception.addSuppressed(e);
			}
		}
	}

	public static Path placeFile(Path file, InputStream stream) {
		try {
			Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return file;
	}

	public static JsonElement downloadFileAsJson(URL url, Path path) throws IOException {
		try (InputStream in = url.openStream()) {
			Files.createDirectories(path.getParent());
			Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
			return JsonParser.parseReader(new InputStreamReader(in));
		} catch (Throwable t) {
			try {
				Files.deleteIfExists(path);
			} catch (Throwable t2) {
				t.addSuppressed(t2);
			}

			throw t;
		}
	}
}
