package com.codezhangborui.uniChatty.managers;

import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    private static Path configPath;
    private static Logger logger;
    private static final Map<String, ConfigEntry> defaultConfig = new LinkedHashMap<>();
    private static Map<String, Object> config = new LinkedHashMap<>();
    private static Yaml yaml;

    private ConfigManager() {
        // 私有构造函数，防止实例化
    }

    public static void initialize(Path dataDirectory, String fileName, Logger loggerInstance) {
        configPath = dataDirectory.resolve(fileName);
        logger = loggerInstance;
        
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        yaml = new Yaml(options);
        
        initializeDefaults();
    }

    private static void initializeDefaults() {
        // 设置默认配置项（带注释）
        addDefault(
                "language",
                "en_us",
                "Available languages: en_us, zh_cn"
        );
        addDefault(
                "chat.format",
                "<aqua><bold>[%server%]</bold></aqua> %prefix%<%player%> %message%",
                "Chat message format with player name and message placeholders.\n" +
                        "Available placeholders: %server%, %player%, %prefix%, %message%"
        );
        addDefault(
                "chat.ignore",
                List.of("server1", "server2"),
                "List of servers that needn't relay messages."
        );
        addDefault(
                "connectors.luckperms",
                false,
                "Should we get player's prefix from LuckPerms?"
        );
        addDefault(
                "join-leave-server-broadcast.enable",
                false,
                "Broadcast message when players join or leave a sub-server"
        );
        addDefault(
                "join-leave-server-broadcast.join-message",
                "<bold><green>+ </green><gold>%player%</gold><gray> joined </gray><gold>%server%</gold></bold>",
                ""
        );
        addDefault(
                "join-leave-server-broadcast.leave-message",
                "<bold><red>- </red><gold>%player%</gold><gray> left </gray><gold>%server%</gold></bold>",
                ""
        );
        addDefault(
                "join-leave-proxy-broadcast.enable",
                true,
                "Broadcast message when players join or leave the proxy"
        );
        addDefault(
                "join-leave-proxy-broadcast.join-message",
                "<bold><green>+ </green><gold>%player%</gold><gray> joined the proxy</bold>",
                ""
        );
        addDefault(
                "join-leave-proxy-broadcast.leave-message",
                "<bold><red>- </red><gold>%player%</gold><gray> left the proxy</gray></bold>",
                ""
        );
        addDefault(
                "ban-words.enable",
                true,
                "Refuse to relay messages that contain banned words\n" +
                        "Use comma (,) to split between words"
        );
        addDefault(
                "ban-words.words",
                "this-is-your-banned-word-1,this-is-your-banned-word-2",
                ""
        );
        addDefault(
                "mention.enable",
                true,
                "The mention command (/@)"
        );
    }

    private static void addDefault(String key, Object value, String comment) {
        defaultConfig.put(key, new ConfigEntry(value, comment));
    }

    public static void load() {
        try {
            if (!Files.exists(configPath)) {
                logger.info("Configuration not found, creating default config: {}", configPath);
                createDefaultConfig();
                return;
            }

            try (InputStream input = Files.newInputStream(configPath)) {
                Object loaded = yaml.load(input);
                if (loaded instanceof Map) {
                    config = flattenMap((Map<String, Object>) loaded);
                } else {
                    config = new LinkedHashMap<>();
                }
            }

            if (addMissingKeys()) {
                save();
            }

        } catch (IOException e) {
            logger.error("Failed to load config file: {}", configPath, e);
            config = new LinkedHashMap<>();
            applyDefaults();
        }
    }

    private static void createDefaultConfig() throws IOException {
        config = new LinkedHashMap<>();
        applyDefaults();
        save();
        logger.info("Default config file created: {}", configPath);
    }

    private static void applyDefaults() {
        for (Map.Entry<String, ConfigEntry> entry : defaultConfig.entrySet()) {
            config.put(entry.getKey(), entry.getValue().value);
        }
    }

    private static boolean addMissingKeys() {
        boolean modified = false;
        for (Map.Entry<String, ConfigEntry> entry : defaultConfig.entrySet()) {
            if (!config.containsKey(entry.getKey())) {
                config.put(entry.getKey(), entry.getValue().value);
                modified = true;
            }
        }
        return modified;
    }

    public static void save() {
        try {
            // 创建父目录
            Files.createDirectories(configPath.getParent());
            
            // 构建嵌套的 Map 结构
            Map<String, Object> nestedConfig = unflattenMap(config);
            
            // 使用 StringBuilder 手动构建带注释的 YAML
            StringBuilder sb = new StringBuilder();
            writeMapWithComments(sb, nestedConfig, "", 0);
            
            Files.writeString(configPath, sb.toString());
            logger.debug("Config file saved: {}", configPath);
        } catch (IOException e) {
            logger.error("Failed to save config file: {}", configPath, e);
        }
    }

    private static void writeMapWithComments(StringBuilder sb, Map<String, Object> map, String prefix, int indent) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 构建完整的键路径
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            ConfigEntry configEntry = defaultConfig.get(fullKey);
            
            if (configEntry != null && configEntry.comment != null && !configEntry.comment.isEmpty()) {
                // 处理多行注释
                String[] commentLines = configEntry.comment.split("\n");
                for (String line : commentLines) {
                    sb.append("  ".repeat(indent)).append("# ").append(line).append("\n");
                }
            }
            
            sb.append("  ".repeat(indent)).append(key).append(": ");
            
            if (value instanceof Map) {
                sb.append("\n");
                writeMapWithComments(sb, (Map<String, Object>) value, fullKey, indent + 1);
            } else if (value instanceof List) {
                sb.append("\n");
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    sb.append("  ".repeat(indent + 1)).append("- ").append(formatValue(item)).append("\n");
                }
            } else {
                sb.append(formatValue(value)).append("\n");
            }
        }
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            String str = (String) value;
            if (str.contains("\n") || str.contains(":") || str.contains("#")) {
                return "\"" + str.replace("\"", "\\\"") + "\"";
            }
            return str;
        }
        return value.toString();
    }

    private static Map<String, Object> flattenMap(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        flattenMapRecursive("", map, result);
        return result;
    }

    private static void flattenMapRecursive(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                flattenMapRecursive(key, (Map<String, Object>) value, result);
            } else {
                result.put(key, value);
            }
        }
    }

    private static Map<String, Object> unflattenMap(Map<String, Object> flatMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            String[] keys = entry.getKey().split("\\.");
            Map<String, Object> current = result;
            
            for (int i = 0; i < keys.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(keys[i], k -> new LinkedHashMap<>());
            }
            
            current.put(keys[keys.length - 1], entry.getValue());
        }
        
        return result;
    }

    public static String getString(String key) {
        Object value = config.get(key);
        if (value != null) {
            return value.toString();
        }
        ConfigEntry defaultValue = defaultConfig.get(key);
        return defaultValue != null ? defaultValue.value.toString() : null;
    }

    public static int getInt(String key) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        ConfigEntry defaultValue = defaultConfig.get(key);
        return defaultValue != null && defaultValue.value instanceof Number
            ? ((Number) defaultValue.value).intValue() : 0;
    }

    public static boolean getBoolean(String key) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        ConfigEntry defaultValue = defaultConfig.get(key);
        return defaultValue != null && defaultValue.value instanceof Boolean
            && (Boolean) defaultValue.value;
    }

    public static List<String> getList(String key) {
        Object value = config.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item != null ? item.toString() : "");
            }
            return result;
        }
        ConfigEntry defaultValue = defaultConfig.get(key);
        if (defaultValue != null && defaultValue.value instanceof List) {
            List<?> list = (List<?>) defaultValue.value;
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item != null ? item.toString() : "");
            }
            return result;
        }
        return new ArrayList<>();
    }

    public static void set(String key, Object value) {
        config.put(key, value);
    }

    public static void set(String key, Object value, String comment) {
        config.put(key, value);
        defaultConfig.put(key, new ConfigEntry(value, comment));
    }

    public static boolean hasKey(String key) {
        return config.containsKey(key);
    }

    public static void reload() {
        load();
    }

    private static class ConfigEntry {
        final Object value;
        final String comment;

        ConfigEntry(Object value, String comment) {
            this.value = value;
            this.comment = comment;
        }
    }
}
