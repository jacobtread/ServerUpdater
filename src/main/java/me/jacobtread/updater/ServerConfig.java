package me.jacobtread.updater;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ServerConfig {

    private final Path file;
    private final Map<String, String> properties;

    public ServerConfig(Path file) {
        this.file = file;
        properties = new HashMap<>();
    }

    public void save() throws IOException {
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
        if (properties.isEmpty()) {
            properties.put("type", "paper");
            properties.put("version", "latest");
            properties.put("hash", null);
            properties.put("jvm_args", null);
        }
        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            content.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        Files.write(file, content.toString().getBytes(StandardCharsets.UTF_8));
    }

    public void load() throws IOException {
        if (Files.exists(file)) {
            List<String> lines = Files.readAllLines(file);
            for (int lineIndex = 0, linesSize = lines.size(); lineIndex < linesSize; lineIndex++) {
                String line = lines.get(lineIndex);
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                String[] parts = line.split("=");
                if (parts.length >= 2) {
                    if (parts.length > 2) {
                        StringBuilder argJoiner = new StringBuilder();
                        for (int i1 = 2; i1 < parts.length; i1++) {
                            argJoiner.append(parts[i1]);
                        }
                        parts = new String[]{parts[0], argJoiner.toString()};
                    }
                    if (parts[1].equals("null")) {
                        parts[1] = null;
                    }
                    properties.put(parts[0], parts[1]);
                } else {
                    throw new InvalidConfigException("Bad config value at line " + lineIndex + ": " + line + " < Missing value!");
                }
            }
            if (!properties.containsKey("type") || !properties.containsKey("version")) {
                throw new InvalidConfigException("Configuration missing type and/or version");
            }
        } else {
            throw new FileNotFoundException(file.toAbsolutePath().toString());
        }
    }

    public String getType() {
        return properties.getOrDefault("type", null);
    }

    public void setType(String type) {
        properties.put("type", type);
    }

    public String getVersion() {
        return properties.getOrDefault("version", null);
    }

    public void setVersion(String version) {
        properties.put("version", version);
    }

    public String getHash() {
        return properties.getOrDefault("hash", null);
    }

    public void setHash(String hash) {
        properties.put("hash", hash);
    }

    public String getJvmArgs() {
        return properties.get("jvm_args");
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "file=" + file +
                ", properties=" + properties +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerConfig that = (ServerConfig) o;
        return Objects.equals(getType(), that.getType()) && Objects.equals(getVersion(), that.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getVersion());
    }

    public void unload() {
        properties.clear();
    }

    public static class InvalidConfigException extends IOException {

        private final String reason;

        public InvalidConfigException(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

}
