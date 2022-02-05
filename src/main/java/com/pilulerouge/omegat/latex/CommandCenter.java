/**************************************************************************
 Simple LaTeX filter for OmegaT

 Copyright (C) 2022 Lev Abashkin

 This file is NOT a part of OmegaT.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package com.pilulerouge.omegat.latex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.pilulerouge.omegat.latex.SettingsDialog.CONF_LOAD_USER_CONFIG;
import static com.pilulerouge.omegat.latex.Util.getFilterOptions;
import static com.pilulerouge.omegat.latex.Util.logLocalRB;
import static org.omegat.util.StaticUtils.getConfigDir;

/**
 * Manage specifics of LaTeX commands and environments.
 */
public class CommandCenter {

    private final static String resourceConfigFileName = "config.json";

    private final static Map<String, Command> commandsByName = new HashMap<>();
    private final static Set<String> optionConsumers = new HashSet<>();
    private final static Set<String> argumentConsumers = new HashSet<>();
    private final static Set<String> tableEnvironments = new HashSet<>();

    private final Map<String, Integer> tagCounters;
    private final Map<String, String> firstOrClosedTags; // Source string to tag
    private final Map<Integer, String> lastTags; // Tag ID to tag
    private final Map<String, String> firstToLastTags; // First to last tag for a pair

    static {
        try {
            loadAndCopyConfig();
        } catch (IOException e) {
            // Shouldn't happen with internal config
        }
    }

    public CommandCenter() {
        tagCounters = new HashMap<>();
        firstOrClosedTags = new HashMap<>();
        lastTags = new HashMap<>();
        firstToLastTags = new HashMap<>();
    }

    public void reset() {
        tagCounters.clear();
        firstOrClosedTags.clear();
        lastTags.clear();
        firstToLastTags.clear();
    }

    public Command getCommand(String name) {
        if (name == null) {
            return null;
        }
        if (commandsByName.containsKey(name)) {
            return commandsByName.get(name);
        }
        return commandsByName.get(Command.UNKNOWN_COMMAND_NAME);
    }

    /**
     * Get virtual command for LaTeX group.
     * @return group command
     */
    public Command getGroupCommand() {
        return getCommand(Command.GROUP_COMMAND_NAME);
    }

    /**
     * Get last tag created earlier with a first tag of this pair.
     * @param tagId tag id from parser mark
     * @return tag string
     */
    public String getLastTag(int tagId) {
        return lastTags.get(tagId);
    }

    /**
     * Get closed or first tag and store last tag for later request.
     * @param content source string
     * @param command command
     * @param tagId tag id from parser mark
     * @return tag string
     */
    public String getFirstOrClosedTag(String content, Command command, int tagId, boolean closed) {
        String firstOrClosedTag, lastTag;

        if (firstOrClosedTags.containsKey(content)) {
            firstOrClosedTag = firstOrClosedTags.get(content);
            if (!closed) {
                lastTag = firstToLastTags.get(firstOrClosedTag);
                lastTags.put(tagId, lastTag);
            }
        } else {
            String tagName = command.getTag();
            if (command.hasNumberedTags()) {
                int tagNumber;
                if (tagCounters.containsKey(tagName)) {
                    tagNumber = tagCounters.get(tagName) + 1;
                } else {
                    tagNumber = 1;
                }
                tagCounters.put(tagName, tagNumber);
                tagName += String.valueOf(tagNumber);
            }
            if (closed) {
                firstOrClosedTag = "<" + tagName + "/>";
            } else {
                firstOrClosedTag = "<" + tagName + ">";
                lastTag = "</" + tagName + ">";
                firstToLastTags.put(firstOrClosedTag, lastTag);
                lastTags.put(tagId, lastTag);
            }
            firstOrClosedTags.put(content, firstOrClosedTag);
        }
        return firstOrClosedTag;
    }

    private static void loadAndCopyConfig() throws IOException {
        String pluginClassName = SimpleLatexFilter.class.getSimpleName();
        Path userConfigPath = Paths.get(getConfigDir(), pluginClassName + ".json");
        URL userConfigUrl = userConfigPath.toUri().toURL();
        URL internalConfigUrl = CommandCenter.class.getClassLoader().getResource(resourceConfigFileName);

        Map<String, String> options = getFilterOptions();
        boolean loadUserConfig = false;
        if (options.containsKey(CONF_LOAD_USER_CONFIG)) {
            loadUserConfig = Boolean.parseBoolean(options.get(CONF_LOAD_USER_CONFIG));
        }

        if (loadUserConfig) {
            if (!Files.exists(userConfigPath)) {
                copyConfig();
            }
            try {
                loadConfig(userConfigUrl);
                logLocalRB("LOG_USER_CONFIG_LOADED", pluginClassName,  userConfigPath);
            } catch (Exception e) {
                logLocalRB("LOG_USER_CONFIG_LOAD_FAILED", pluginClassName);
                loadConfig(internalConfigUrl);
            }
        } else {
            loadConfig(internalConfigUrl);
        }
    }

    /**
     * Copy config file to user config directory if it's not there.
     */
    static void copyConfig() {
        String pluginClassName = SimpleLatexFilter.class.getSimpleName();
        Path userConfigPath = Paths.get(getConfigDir(), pluginClassName + ".json");
        if (!Files.exists(userConfigPath)) {
            try (InputStream is = CommandCenter.class.getClassLoader().getResourceAsStream(resourceConfigFileName)) {
                Files.copy(is, userConfigPath);
            } catch (IOException e) {
                logLocalRB("LOG_USER_CONFIG_COPY_FAILED", pluginClassName, userConfigPath);
            }
        }
    }

    private static void loadConfig(URL configFileUrl) throws IOException {

        JsonNode root;
        ObjectMapper mapper = new ObjectMapper();
        root = mapper.readTree(configFileUrl);

        JsonNode envNode = root.get("environments");
        for (JsonNode node: envNode.get("consumeOptions")) {
            optionConsumers.add(node.asText());
        }
        for (JsonNode node: envNode.get("consumeArguments")) {
            argumentConsumers.add(node.asText());
        }
        for (JsonNode node: envNode.get("table")) {
            tableEnvironments.add(node.asText());
        }

        for (JsonNode tcNode : root.get("allCommands")) {
            CommandType commandType = CommandType.valueOf(tcNode.get("type").asText());
            for (JsonNode commandNode : tcNode.get("commands")) {
                List<CommandArgument> args = new ArrayList<>();
                String commandName, tagName;
                if (commandNode.isTextual()) {
                    commandName = commandNode.asText();
                    tagName = null;
                    if (commandType == CommandType.CONTENT) {
                        args.add(new CommandArgument(true, false, true));
                    }
                } else {
                    commandName = commandNode.get("name").asText();
                    tagName = getJSONStringValue(commandNode, "tag");
                    JsonNode argsNode = commandNode.get("args");
                    if (argsNode == null && commandType != CommandType.CONTROL) {
                        args.add(new CommandArgument(true, false, true));
                    } else if (argsNode != null) {
                        for (JsonNode argNode : argsNode) {
                            boolean translatable = argNode.get("translate").asBoolean();
                            boolean external = getJSONBooleanValue(argNode, "external", false);
                            boolean escape = getJSONBooleanValue(argNode, "escape", true);
                            args.add(new CommandArgument(translatable, external, escape));
                        }
                    }
                }

                addCommand(commandName, commandType, tagName, args);
            }
        }
        // Add virtual group command
        addCommand(
                Command.GROUP_COMMAND_NAME,
                CommandType.FORMAT,
                Command.GROUP_COMMAND_TAG,
                Collections.singletonList(new CommandArgument(true, false, true))
        );

        // Add unknown command
        addCommand(
                Command.UNKNOWN_COMMAND_NAME,
                CommandType.FORMAT,
                Command.UNKNOWN_COMMAND_TAG,
                Collections.singletonList(new CommandArgument(true, false, true))
        );

        // Add mask command
        addCommand(
                Command.MASK_COMMAND_NAME,
                CommandType.FORMAT,
                Command.MASK_COMMAND_TAG,
                Collections.emptyList()
        );

        // Add inline math command
        addCommand(
                Command.INLINE_MATH_COMMAND_NAME,
                CommandType.FORMAT,
                Command.INLINE_MATH_COMMAND_TAG,
                Collections.emptyList()
        );
    }

    private static void addCommand(String commandName, CommandType commandType, String tagName,
                                   List<CommandArgument> args) {
        commandsByName.put(
                commandName,
                new Command(commandType, commandName, tagName, args.toArray(new CommandArgument[0]))
        );

    }

    private static boolean getJSONBooleanValue(JsonNode node, String field, boolean defaultValue) {
        if (node.has(field)) {
            return node.get(field).asBoolean();
        } else {
            return defaultValue;
        }
    }

    private static String getJSONStringValue(JsonNode node, String field) {
        if (node.has(field)) {
            return node.get(field).asText();
        }
        return null;
    }

    public boolean isOptionConsumer(String envName) {
        return optionConsumers.contains(envName);
    }

    public boolean isArgumentConsumer(String envName) {
        return argumentConsumers.contains(envName);
    }

    public boolean isTableEnvironment(String envName) {
        return tableEnvironments.contains(envName);
    }
}
