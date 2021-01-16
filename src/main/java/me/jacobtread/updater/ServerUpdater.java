package me.jacobtread.updater;

import com.serverjars.api.JarDetails;
import com.serverjars.api.Response;
import com.serverjars.api.request.AllRequest;
import com.serverjars.api.request.JarRequest;
import com.serverjars.api.request.LatestRequest;
import com.serverjars.api.request.TypesRequest;
import com.serverjars.api.response.AllResponse;
import com.serverjars.api.response.LatestResponse;
import com.serverjars.api.response.TypesResponse;
import me.jacobtread.updater.logging.LogHandler;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerUpdater {

    public static final String VERSION = "1.0.0";
    private static final Logger LOGGER = Logger.getLogger("Updater");
    public static boolean VERBOSE = false;
    public static boolean FORCE_OFFLINE = false;
    public static boolean NO_INPUT = false;
    private final BufferedReader in;
    private final Path serverPath;
    private final Path configPath;
    private final Path jarPath;
    private final Path jarTmpPath;
    private final List<String> args;
    private boolean isOffline;
    private ServerConfig serverConfig;

    public ServerUpdater(List<String> args) {
        this.args = args;
        serverPath = Paths.get("server");
        configPath = Paths.get("config.properties");
        jarPath = serverPath.resolve("server.jar");
        jarTmpPath = serverPath.resolve("server-tmp.jar");
        in = new BufferedReader(new InputStreamReader(System.in));
    }

    private void checkConnection() {
        if (!FORCE_OFFLINE) {
            try {
                HttpsURLConnection connection = (HttpsURLConnection) new URL("https://serverjars.com").openConnection();
                connection.connect();
                isOffline = false;
            } catch (IOException e) {
                isOffline = true;
            }
        } else {
            isOffline = true;
        }
    }

    public void guide() {
        serverConfig = new ServerConfig(configPath);
        try {
            serverConfig.load();
        } catch (IOException ignored) {
        }
        final ServerConfig oldConfig = serverConfig;
        boolean useCustomType = false;
        TypesResponse typesResponse = null;
        if (isOffline) {
            LOGGER.info("It appears you are offline mode would you like to run the setup anyway? [Y/N]");
            useCustomType = InputUtils.awaitBoolean(in);
            if (!useCustomType) {
                System.exit(0);
            }
        } else {
            TypesRequest typesRequest = new TypesRequest("servers");
            while (true) {
                try {
                    LOGGER.info("Loading types from ServerJars.com");
                    typesResponse = typesRequest.send();
                    if (!FORCE_OFFLINE) isOffline = false;
                    if (typesResponse.isSuccess()) {
                        if (VERBOSE) LOGGER.info("Loaded types (" + typesResponse.getRequestedChildren().size() + ")");
                        break;
                    } else {
                        LOGGER.warning("Failed to retrieve types from ServeJars.com API.");
                        LOGGER.warning("Reason: " + typesResponse.getErrorTitle() + ": " + typesResponse.getErrorMessage());
                    }
                } catch (IOException e) {
                    isOffline = true;
                    LOGGER.warning("Unable to connect to ServerJars.com api.");
                    LOGGER.warning("Reason: " + e.getMessage());
                }
                LOGGER.info("Type \"retry\" to try again, \"manual\" to enter your own type, \"exit\" to exit");
                String response = InputUtils.awaitInput(in, s ->
                                s.equalsIgnoreCase("retry") || s.equalsIgnoreCase("manual") || s.equalsIgnoreCase("exit"),
                        s -> "You must choose \"retry\", \"manual\" or \"exit\""
                ).toLowerCase(Locale.ROOT);
                if (response.equals("exit")) {
                    System.exit(0);
                    break;
                } else if (response.equals("manual")) {
                    useCustomType = true;
                    break;
                }
            }
        }
        String type;
        if (useCustomType) {
            LOGGER.info("Please enter the type you would like to use:");
            type = InputUtils.awaitInput(in, s -> !s.isEmpty(), s -> "You must provide a type!");
        } else {
            LOGGER.info("The available types are: ");
            List<String> types = typesResponse.getRequestedChildren();
            LOGGER.info(types.toString());
            LOGGER.info("Please enter the name of the type you wish to use (case sensitive):");
            type = InputUtils.awaitInput(in, types::contains, s -> "That type was not listed in the types above.");
        }
        serverConfig.setType(type);
        if (VERBOSE) LOGGER.info("Selected type \"" + type + '"');
        String version;
        while (true) {
            LOGGER.info("Enter the version you would like to use (type \"latest\" or leave blank for latest version):");
            version = InputUtils.awaitInput(in, s -> true, s -> "");
            if (version.equalsIgnoreCase("latest") || version.isEmpty()) {
                break;
            } else {
                if (isOffline) {
                    break;
                } else {
                    AllRequest allRequest = new AllRequest(type);
                    AllResponse allResponse = null;
                    while (true) {
                        try {
                            allResponse = allRequest.send();
                            if (!FORCE_OFFLINE) isOffline = false;
                            if (allResponse.isSuccess()) {
                                break;
                            } else {
                                LOGGER.warning("Failed to check version with ServerJars.com API.");
                                LOGGER.warning("Reason: " + allResponse.getErrorTitle() + ": " + allResponse.getErrorMessage());
                            }
                        } catch (IOException e) {
                            LOGGER.warning("Failed to check version with ServerJars.com API.");
                            LOGGER.warning("Reason: " + e.getMessage());
                        }
                        LOGGER.info("Type \"retry\" to try again, \"continue\" to continue anyway, \"exit\" to exit");
                        String response = InputUtils.awaitInput(in, s ->
                                        s.equalsIgnoreCase("retry") || s.equalsIgnoreCase("continue") || s.equalsIgnoreCase("exit"),
                                s -> "You must choose \"retry\", \"continue\" or \"exit\""
                        ).toLowerCase(Locale.ROOT);
                        if (response.equals("exit")) {
                            System.exit(0);
                            break;
                        } else if (response.equals("continue")) {
                            allResponse = null;
                            break;
                        }
                    }
                    if (allResponse == null) {
                        break;
                    } else {
                        boolean foundVersion = false;
                        for (JarDetails jar : allResponse.getJars()) {
                            if (jar.getVersion().equals(version)) {
                                foundVersion = true;
                                if (VERBOSE) LOGGER.info("Found version " + jar.toString());
                                break;
                            }
                        }
                        if (foundVersion) {
                            if (!VERBOSE) LOGGER.info("Version found!");
                            break;
                        } else {
                            LOGGER.warning("We don't seem to have that version...");
                        }
                    }
                }
            }
        }
        serverConfig.setVersion(version);
        if (VERBOSE) LOGGER.info("Selected version \"" + version + '"');
        LOGGER.info("Setup Complete, saving config to: " + configPath.toAbsolutePath().toString());
        try {
            serverConfig.save();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save config file...", e);
        }
        if (!serverConfig.equals(oldConfig)) {
            if (Files.exists(jarPath)) {
                try {
                    Files.delete(jarPath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void init() {
        checkConnection();
        if (!Files.exists(serverPath)) {
            try {
                Files.createDirectories(serverPath);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create server directory", e);
                return;
            }
        }
        if (!Files.exists(configPath) && !NO_INPUT) {
            LOGGER.info("Would you like to run the setup guide? [Y/N]");
            if (InputUtils.awaitBoolean(in)) {
                guide();
            }
        }
        ServerConfig config = null;
        boolean useDefault = false;
        if (Files.exists(configPath)) {
            config = new ServerConfig(configPath);
            try {
                config.load();
            } catch (ServerConfig.InvalidConfigException e) {
                LOGGER.log(Level.SEVERE, "It appears there is there is an error in your configuration file please check it or generate a new one.", e);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Unable to load server config", e);
                if (NO_INPUT) {
                    useDefault = true;
                } else {
                    LOGGER.info("Should we use the default configuration instead? [Y/N]");
                    useDefault = InputUtils.awaitBoolean(in);
                }
            }
        }
        if (config == null || useDefault) {
            config = new ServerConfig(configPath);
            config.setType("paper");
            config.setVersion("latest");
            try {
                if (VERBOSE) LOGGER.info("Creating default config");
                config.save();
                if (VERBOSE) LOGGER.info("Created default config");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to create default server config", e);
            }
        }
        serverConfig = config;
        checkJar();
    }

    public void checkJar() {
        boolean hasUpdated = false;
        boolean isLatest = serverConfig.getVersion().equals("latest");
        if (Files.exists(jarPath)) {
            if (isOffline) {
                if (!FORCE_OFFLINE)
                    LOGGER.warning("Couldn't connect to ServerJars.com running as " + LogHandler.RED + "OFFLINE" + LogHandler.RESET);
            } else {
                String clientHash = md5(jarPath);
                String serverHash = null;
                while (true) {
                    LOGGER.info("Retrieving latest jar information from ServerJars.com");
                    if (isLatest) {
                        LatestRequest latestRequest = new LatestRequest(serverConfig.getType());
                        try {
                            LatestResponse latestResponse = latestRequest.send();
                            if (!FORCE_OFFLINE) isOffline = false;
                            if (latestResponse.isSuccess()) {
                                serverHash = latestResponse.getLatestJar().getHash();
                                break;
                            } else {
                                LOGGER.warning("Failed to check if the jar is the latest available.");
                                LOGGER.warning("Reason: " + latestResponse.getErrorTitle() + ": " + latestResponse.getErrorMessage());
                            }
                        } catch (IOException e) {
                            isOffline = true;
                            LOGGER.warning("Failed to check if the jar is the latest available.");
                            LOGGER.warning("Reason: " + e.getMessage());
                        }
                    } else {
                        AllRequest allRequest = new AllRequest(serverConfig.getType());
                        try {
                            AllResponse allResponse = allRequest.send();
                            if (!FORCE_OFFLINE) isOffline = false;
                            if (allResponse.isSuccess()) {
                                for (JarDetails jar : allResponse.getJars()) {
                                    if (jar.getVersion().equals(serverConfig.getVersion())) {
                                        serverHash = jar.getHash();
                                        break;
                                    }
                                }
                                if (serverHash != null) {
                                    break;
                                }
                            } else {
                                LOGGER.warning("Failed to check if the jar is the latest available.");
                                LOGGER.warning("Reason: " + allResponse.getErrorTitle() + ": " + allResponse.getErrorMessage());
                            }
                        } catch (IOException e) {
                            isOffline = true;
                            LOGGER.warning("Failed to check if the jar is the latest available.");
                            LOGGER.warning("Reason: " + e.getMessage());
                        }
                    }
                    if (NO_INPUT) {
                        break;
                    } else {
                        LOGGER.info("Type \"retry\" to try again, \"skip\" to skip the check.");
                        String response = InputUtils.awaitInput(in, s -> s.equalsIgnoreCase("retry") || s.equalsIgnoreCase("skip"), s -> "You must choose \"retry\" or \"skip\"");
                        if (response.equalsIgnoreCase("skip")) {
                            break;
                        }
                    }
                }
                if (clientHash == null) {
                    LOGGER.info("Failed to generate hash for server jar, skipping.");
                } else if (serverHash == null) {
                    LOGGER.info("Couldn't find a matching jar on ServerJars.com, skipping.");
                } else {
                    boolean isInvalid = serverConfig.getHash() == null || !serverConfig.getHash().equals(serverHash);
                    if (isInvalid) {
                        LOGGER.info("New version of \""+serverConfig.getType()+"\" found on ServerJars.com downloading update...");
                        if (VERBOSE) {
                            LOGGER.info(String.format("Current Hash: %s Expected Hash: %s", clientHash, serverHash));
                        }
                        while (true) {
                            if (downloadJar()) {
                                hasUpdated = true;
                                break;
                            } else {
                                if (NO_INPUT) {
                                    LOGGER.warning("Failed to download jar using current jar as fallback");
                                    break;
                                }
                                LOGGER.warning("Failed to download server jar file should we retry? [Y/N]");
                                if (!InputUtils.awaitBoolean(in)) {
                                    LOGGER.warning("Using current jar as fallback");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (isOffline) {
                LOGGER.severe("You must run the updater in online mode atleast once.");
            } else {
                LOGGER.info("Downloading jar for \"" + serverConfig.getType() + "\" version \"" + serverConfig.getVersion() + "\"");
                while (true) {
                    if (downloadJar()) {
                        hasUpdated = true;
                        break;
                    } else {
                        if (NO_INPUT) {
                            LOGGER.severe("Failed to download jar with no fallback. Exiting.");
                            return;
                        }
                        LOGGER.warning("Failed to download server jar file should we retry? [Y/N]");
                        if (!InputUtils.awaitBoolean(in)) {
                            System.exit(0);
                            return;
                        }
                    }
                }
            }
        }
        if (hasUpdated) {
            try {
                Files.move(jarTmpPath, jarPath, StandardCopyOption.REPLACE_EXISTING);
                String clientHash = md5(jarPath);
                if (serverConfig.getHash() == null || !serverConfig.getHash().equals(clientHash)) {
                    serverConfig.setHash(clientHash);
                    try {
                        serverConfig.save();
                    } catch (IOException ignored) {
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to move jar from tmp", e);
                return;
            } finally {
                try {
                    Files.delete(jarTmpPath);
                } catch (IOException ignored) {
                }
            }
            LOGGER.info("Jar download complete.");
        }
        LOGGER.info(String.format("Launching... [%s, %s]", serverConfig.getType(), serverConfig.getVersion()));
        runJar();
    }

    private boolean downloadJar() {
        boolean isLatest = serverConfig.getVersion().equals("latest");
        JarRequest jarRequest = new JarRequest(serverConfig.getType(), isLatest ? null : serverConfig.getVersion(), jarTmpPath);
        try {
            Response response = jarRequest.send();
            return response.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    private String getJavaExecutable() {
        final String javaHome = System.getProperty("java.home");
        Path bin = Paths.get(javaHome).resolve("bin");
        Path executable = bin.resolve("java");
        if (!Files.exists(executable)) {
            executable = bin.resolve("java.exe");
        }
        if (Files.exists(executable)) {
            return executable.toAbsolutePath().toString();
        } else {
            return "java";
        }
    }

    private void runJar() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.inheritIO();
            List<String> processArgs = new ArrayList<>();
            processArgs.add(getJavaExecutable());
            if (serverConfig.getJvmArgs() != null) {
                String[] jvmArgs = serverConfig.getJvmArgs().split(" ");
                if (VERBOSE) LOGGER.info("Using VM args: " + Arrays.toString(jvmArgs));
                if (jvmArgs.length > 0) processArgs.addAll(Arrays.asList(jvmArgs));
            }
            processArgs.add("-jar");
            processArgs.add(jarPath.toAbsolutePath().toString());
            processArgs.addAll(args);
            processBuilder.command(processArgs);
            processBuilder.directory(serverPath.toFile());
            serverConfig.unload();
            in.close();
            System.gc();
            Process process = processBuilder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
            try {
                process.waitFor();
            } catch (InterruptedException ignored) {
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start server process. Exiting", e);
        } finally {
            LOGGER.info("Shutting Down");
        }
    }

    private String md5(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(Files.readAllBytes(file));
            byte[] digest = md.digest();
            BigInteger rawHash = new BigInteger(1, digest);
            StringBuilder hash = new StringBuilder(rawHash.toString(16));
            while (hash.length() < 32) hash.insert(0, "0");
            return hash.toString().toLowerCase(Locale.ROOT);
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

}
