package com.codezhangborui.uniChatty.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

public class MessagesManager {
    private static Path messagesDirectory;
    private static Logger logger;
    private static String currentLang = "en_us";
    private static final Map<String, Map<String, Object>> loadedLanguages = new HashMap<>();
    private static final Map<String, Map<String, String>> languageComments = new HashMap<>();
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static Yaml yaml;

    private MessagesManager() {
        // 私有构造函数，防止实例化
    }

    public static void initialize(Path dataDirectory, Logger loggerInstance) {
        messagesDirectory = dataDirectory.resolve("messages");
        logger = loggerInstance;

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        yaml = new Yaml(options);

        try {
            if (!Files.exists(messagesDirectory)) {
                Files.createDirectories(messagesDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create messages directory", e);
        }
    }

    public static void loadLanguage(String langCode) {
        if (loadedLanguages.containsKey(langCode)) {
            return;
        }

        Path langFile = messagesDirectory.resolve(langCode + ".yml");

        try {
            if (!Files.exists(langFile)) {
                logger.warn("Language file not found: {}, attempting to copy from resources", langCode);
                copyLanguageFileFromResources(langCode);
            }

            try (InputStream input = Files.newInputStream(langFile)) {
                Object loaded = yaml.load(input);
                Map<String, Object> flatMap = new LinkedHashMap<>();
                if (loaded instanceof Map) {
                    flattenMap("", (Map<String, Object>) loaded, flatMap);
                }
                loadedLanguages.put(langCode, flatMap);
            }

            // 检测并补充缺失的键
            ensureAllKeysExist(langCode);

            logger.info("Language file loaded: {}", langCode);

        } catch (IOException e) {
            logger.error("Failed to load language file: {}", langCode, e);
        }
    }

    private static void ensureAllKeysExist(String langCode) throws IOException {
        Map<String, Object> defaultKeys = loadDefaultKeys(langCode);
        if (defaultKeys == null || defaultKeys.isEmpty()) {
            return;
        }

        Map<String, Object> currentMessages = loadedLanguages.get(langCode);
        if (currentMessages == null) {
            return;
        }

        boolean modified = false;
        for (Map.Entry<String, Object> entry : defaultKeys.entrySet()) {
            String key = entry.getKey();
            if (!currentMessages.containsKey(key)) {
                currentMessages.put(key, entry.getValue());
                modified = true;
            }
        }

        // 如果有修改，保存文件
        if (modified) {
            saveLanguageFile(langCode);
            logger.info("Updated language file with missing keys: {}", langCode);
        }
    }

    private static Map<String, Object> loadDefaultKeys(String langCode) {
        // 首先尝试加载对应语言的资源文件
        String resourcePath = "languages/" + langCode + ".yml";
        
        try (InputStream resourceStream = MessagesManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (resourceStream != null) {
                Object loaded = yaml.load(resourceStream);
                Map<String, Object> flatMap = new LinkedHashMap<>();
                if (loaded instanceof Map) {
                    flattenMap("", (Map<String, Object>) loaded, flatMap);
                }
                return flatMap;
            } else {
                // 如果对应语言的资源文件不存在，尝试使用 en_us 作为后备
                logger.debug("Resource file not found for {}, falling back to en_us", langCode);
                return loadDefaultKeysFromEnUs();
            }
        } catch (IOException e) {
            logger.error("Failed to load default keys from resources for {}", langCode, e);
            return loadDefaultKeysFromEnUs();
        }
    }

    private static Map<String, Object> loadDefaultKeysFromEnUs() {
        String resourcePath = "languages/en_us.yml";
        
        try (InputStream resourceStream = MessagesManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (resourceStream != null) {
                Object loaded = yaml.load(resourceStream);
                Map<String, Object> flatMap = new LinkedHashMap<>();
                if (loaded instanceof Map) {
                    flattenMap("", (Map<String, Object>) loaded, flatMap);
                }
                return flatMap;
            } else {
                logger.warn("Default language resource file not found: {}", resourcePath);
                return new LinkedHashMap<>();
            }
        } catch (IOException e) {
            logger.error("Failed to load default keys from en_us resources", e);
            return new LinkedHashMap<>();
        }
    }

    private static void copyLanguageFileFromResources(String langCode) throws IOException {
        Path langFile = messagesDirectory.resolve(langCode + ".yml");
        String resourcePath = "languages/" + langCode + ".yml";
        
        // 尝试从 resources 复制文件
        try (InputStream resourceStream = MessagesManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (resourceStream != null) {
                Files.copy(resourceStream, langFile);
                logger.info("Language file copied from resources: {}", langCode);
            } else {
                // 如果资源文件不存在，创建默认文件
                logger.warn("Resource file not found: {}, creating default language file", resourcePath);
                createDefaultLanguageFile(langCode);
            }
        }
    }

    private static void createDefaultLanguageFile(String langCode) throws IOException {
        // 创建空的语言文件，实际内容会通过 ensureAllKeysExist 从 resources 补充
        Path langFile = messagesDirectory.resolve(langCode + ".yml");
        
        Map<String, Object> messages = new LinkedHashMap<>();
        loadedLanguages.put(langCode, messages);

        saveLanguageFile(langCode);
        logger.info("Empty language file created, will be populated with default keys: {}", langCode);
    }

    private static void saveLanguageFile(String langCode) throws IOException {
        Path langFile = messagesDirectory.resolve(langCode + ".yml");
        Map<String, Object> flatMap = loadedLanguages.get(langCode);
        Map<String, String> comments = languageComments.getOrDefault(langCode, new HashMap<>());

        if (flatMap == null) {
            return;
        }

        // 构建嵌套的 Map 结构
        Map<String, Object> nestedMap = unflattenMap(flatMap);

        // 使用 StringBuilder 手动构建带注释的 YAML
        StringBuilder sb = new StringBuilder();
        writeMapWithComments(sb, nestedMap, "", comments, 0);

        Files.writeString(langFile, sb.toString());
    }

    private static void writeMapWithComments(StringBuilder sb, Map<String, Object> map, String prefix, Map<String, String> comments, int indent) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 构建完整的键路径
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            String comment = comments.get(fullKey);

            if (comment != null && !comment.isEmpty()) {
                for (String line : comment.split("\n")) {
                    sb.append("  ".repeat(indent)).append("# ").append(line).append("\n");
                }
            }

            sb.append("  ".repeat(indent)).append(key).append(": ");

            if (value instanceof Map) {
                sb.append("\n");
                writeMapWithComments(sb, (Map<String, Object>) value, fullKey, comments, indent + 1);
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

    private static void flattenMap(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenMap(key, (Map<String, Object>) value, result);
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

    public static void setLanguage(String langCode) {
        if (!loadedLanguages.containsKey(langCode)) {
            loadLanguage(langCode);
        }
        currentLang = langCode;
        logger.debug("Switched language to: {}", langCode);
    }

    public static String getRaw(String key) {
        return getRaw(currentLang, key);
    }

    public static String getRaw(String langCode, String key) {
        if (!loadedLanguages.containsKey(langCode)) {
            loadLanguage(langCode);
        }

        Map<String, Object> messages = loadedLanguages.get(langCode);
        if (messages == null) {
            logger.warn("Language {} is not loaded", langCode);
            return key;
        }

        Object value = messages.get(key);
        if (value == null) {
            logger.warn("Message key not found: {} (Language: {})", key, langCode);
            return key;
        }

        return value.toString();
    }

    public static Component get(String key) {
        return get(currentLang, key);
    }

    public static Component get(String langCode, String key) {
        String raw = getRaw(langCode, key);
        return miniMessage.deserialize(raw);
    }

    public static Component get(String key, Map<String, String> placeholders) {
        return get(currentLang, key, placeholders);
    }

    public static Component get(String langCode, String key, Map<String, String> placeholders) {
        String raw = getRaw(langCode, key);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace(entry.getKey(), entry.getValue());
        }

        return miniMessage.deserialize(raw);
    }

    public static void reload() {
        loadedLanguages.clear();
        loadLanguage(currentLang);
        logger.info("All language files reloaded");
    }

    public static void reloadLanguage(String langCode) {
        loadedLanguages.remove(langCode);
        loadLanguage(langCode);
        logger.info("Language file reloaded: {}", langCode);
    }
}
