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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.pilulerouge.omegat.latex.Command.*;

public class Parser {

    private final LinkedList<ParserLevel> levels;
    private final CommandCenter commandCenter;
    private final LinkedList<String> environments;
    private List<String> currentEnvironments;
    private int lastTagId; // New tags get ID from this one
    private boolean maskingTokens;

    public Parser(CommandCenter cc) {
        commandCenter = cc;
        levels = new LinkedList<>();
        levels.add(new ParserLevel(true, 0, true, false, false));
        lastTagId = 0;
        maskingTokens = false;
        environments = new LinkedList<>();
        currentEnvironments = Collections.emptyList();
    }

    public void reset() {
        lastTagId = 0;
        maskingTokens = false;
        levels.clear();
        levels.add(new ParserLevel(true, 0, true, false, false)); // Root level
        environments.clear();
        currentEnvironments = Collections.emptyList();
    }

    public void processToken(final Token token) {

        Command command;
        ParserLevel parentLevel;
        int tagId = 0;

        boolean newLevelTranslatable;
        boolean newLevelEscapeContent;
        int newLevelExternality;
        boolean newLevelIsHidden;

        // Inherit properties from current level
        ParserLevel currentLevel = getCurrentLevel();
        int currentExternality = currentLevel.getExternality();
        boolean tokenTranslatable = currentLevel.isTranslatable();
        boolean tokenEscapeContent = currentLevel.doEscape();

        TokenType tt = token.getType();

        // Unregister command if this token is not an option/argument start
        if (currentLevel.hasCommand() && tt != TokenType.GROUP_BEGIN && tt != TokenType.OPTION_BEGIN) {
            currentLevel.unregisterCommand();
        }

        // Remove consumer flags on environment definition end
        if ((currentLevel.isOptionConsumer() || currentLevel.isArgumentConsumer()) &&
                tt != TokenType.GROUP_BEGIN && tt != TokenType.OPTION_BEGIN) {
            currentLevel.setArgumentConsumer(false);
            currentLevel.setOptionConsumer(false);
        }

        switch (tt) {
            case COMMAND:
                if (currentLevel.isHidden()) {
                    break;
                }
                command = commandCenter.getCommand(token.getName());
                currentLevel.registerCommand(command);
                if (command.getType() == CommandType.FORMAT) {
                    tagId = ++lastTagId;
                    currentLevel.setTagId(tagId);
                } else {
                    tokenTranslatable = false;
                }
                if (command.isUnknown()) {
                    token.setName(UNKNOWN_COMMAND_NAME);
                }
                break;
            case INLINE_MATH:
                if (currentLevel.isHidden()) {
                    break;
                }
                token.setName(INLINE_MATH_COMMAND_NAME);
                tagId = ++lastTagId;
                break;
            case OPTION_BEGIN:
                // Break early if token is not an option begin but a text
                if (!currentLevel.hasCommand() && !currentLevel.isOptionConsumer())
                    break;

                // Initialize new level properties from parent level
                newLevelTranslatable = currentLevel.isTranslatable() && !currentLevel.isOptionConsumer();
                newLevelEscapeContent = currentLevel.doEscape();
                newLevelExternality = currentExternality;
                newLevelIsHidden = currentLevel.isHidden();

                if (currentLevel.isOptionConsumer()) {
                    tokenTranslatable = false;
                    newLevelTranslatable = false;
                } else {
                    tagId = currentLevel.getTagId();
                    // Adjust level properties for configured option
                    if (currentLevel.hasOptionInQueue()) {
                        CommandArgument currentOption = currentLevel.fetchOption();

                        // Flag for non-translatable option of FORMAT command
                        newLevelIsHidden = !currentOption.isTranslatable() && newLevelTranslatable &&
                                currentLevel.getCommand().getType() == CommandType.FORMAT;
                        newLevelTranslatable = newLevelTranslatable && (currentOption.isTranslatable() || newLevelIsHidden);
                        newLevelEscapeContent = currentOption.doEscape() && !newLevelIsHidden;

                        if (currentOption.isExternal() && newLevelTranslatable && !newLevelIsHidden) {
                            newLevelExternality += 1;
                        }
                    } else { // No configured option.
                        newLevelIsHidden = newLevelTranslatable && currentLevel.getCommand().getType() == CommandType.FORMAT;
                    }
                    // Token should be translatable only as part of a FORMAT command
                    if (currentLevel.getCommand().getType() != CommandType.FORMAT) {
                        tokenTranslatable = false;
                    }
                }
                addLevel(newLevelTranslatable, newLevelExternality, newLevelEscapeContent, newLevelIsHidden, true);

                // Inherit tagId for hidden levels
                if (newLevelIsHidden) {
                    ParserLevel newLevel = getCurrentLevel();
                    newLevel.setTagId(currentLevel.getTagId());
                }
                break;
            case OPTION_END:
                if (!currentLevel.isOptionValue())
                    break;

                removeLevel();

                parentLevel = getCurrentLevel();
                currentExternality = parentLevel.getExternality();
                tagId = parentLevel.getTagId();

                if (parentLevel.hasCommand() && parentLevel.getCommand().getType() != CommandType.FORMAT) {
                    tokenTranslatable = false;
                }
                break;
            case GROUP_BEGIN:
                newLevelTranslatable = currentLevel.isTranslatable() && !currentLevel.isArgumentConsumer();
                newLevelEscapeContent = currentLevel.doEscape();
                newLevelExternality = currentExternality;
                newLevelIsHidden = currentLevel.isHidden();

                if (currentLevel.hasArgumentInQueue() && !newLevelIsHidden && newLevelTranslatable) {
                    tagId = currentLevel.getTagId();
                    CommandArgument currentArg = currentLevel.fetchArgument();
                    // Flag for non-translatable argument of FORMAT command
                    newLevelIsHidden = !currentArg.isTranslatable() &&
                            currentLevel.getCommand().getType() == CommandType.FORMAT;
                    newLevelTranslatable = currentArg.isTranslatable() || newLevelIsHidden;
                    newLevelEscapeContent = currentArg.doEscape() && !newLevelIsHidden;

                    if (currentArg.isExternal() && newLevelTranslatable && !newLevelIsHidden) {
                        newLevelExternality += 1;
                    }

                    // Group begin should be translatable only as part of a FORMAT command
                    if (currentLevel.getCommand().getType() != CommandType.FORMAT) {
                        tokenTranslatable = false;
                    }

                // Create virtual group command on orphan group begin
                } else if (newLevelTranslatable && !newLevelIsHidden) {
                    token.setName(GROUP_COMMAND_NAME);
                    currentLevel.registerCommand(commandCenter.getGroupCommand());
                    currentLevel.fetchArgument(); // Remove argument right away
                    tagId = ++lastTagId;
                    currentLevel.setTagId(tagId);
                } else if (currentLevel.isArgumentConsumer()) {
                    tokenTranslatable = false;
                }
                addLevel(newLevelTranslatable, newLevelExternality, newLevelEscapeContent, newLevelIsHidden, false);

                // Inherit tagId for hidden levels
                if (newLevelIsHidden) {
                    ParserLevel newLevel = getCurrentLevel();
                    newLevel.setTagId(currentLevel.getTagId());
                }
                break;
            case GROUP_END:
                if (!onRootLevel()) {
                    removeLevel();
                }
                parentLevel = getCurrentLevel();
                currentExternality = parentLevel.getExternality();
                tagId = parentLevel.getTagId();

                if (parentLevel.hasCommand() && parentLevel.getCommand().getType() != CommandType.FORMAT) {
                    tokenTranslatable = false;
                }

                if (parentLevel.hasCommand() && !parentLevel.hasArgumentInQueue() && !parentLevel.isArgumentConsumer()) {
                    parentLevel.unregisterCommand();
                    tagId = -tagId;  // Inverse tag id indicates last tag
                }
                break;
            case ENV_BEGIN:
                String envName = token.getName();
                environments.addLast(envName);

                if (commandCenter.isArgumentConsumer(envName)) {
                    currentLevel.setArgumentConsumer(true);
                }
                if (commandCenter.isOptionConsumer(envName)) {
                    currentLevel.setOptionConsumer(true);
                }

                currentEnvironments = Collections.unmodifiableList(environments);
                tokenTranslatable = false;
                break;
            case ENV_END:
                String tokenEnvName = token.getName();
                String stackEnvName;
                if (environments.isEmpty()) {
                    stackEnvName = "not present";
                } else {
                    stackEnvName = environments.getLast();
                }
                if (tokenEnvName.equals(stackEnvName)) {
                    environments.removeLast();
                    currentEnvironments = Collections.unmodifiableList(environments);
                } else {
                    String sb = "Found end of environment " + tokenEnvName +
                            " when last open environment was " + stackEnvName;
                    throw new SimpleLatexFilterException(sb);
                }
                tokenTranslatable = false;
                break;
            case AMPERSAND:
                if (!environments.isEmpty() && commandCenter.isTableEnvironment(environments.getLast())) {
                    tokenTranslatable = false;
                }
                break;
            case VERBATIM:
                tokenEscapeContent = false;
                break;
            case DISPLAY_MATH:
            case EMPTY_LINE:
            case COMMENT:
            case LINE_BREAK:
            case DUMMY:
                tokenTranslatable = false;
                break;
            case TEXT:
            case STN:
            default:  // Nothing to do here
        }

        // Mask untranslatable tokens inside external context
        if (!tokenTranslatable && currentExternality > 0) {
            tokenTranslatable = true;
            token.setName(MASK_COMMAND_NAME);
            if (!maskingTokens) {
                lastTagId += 1; // New masking tag
            }
            tagId = lastTagId;
            maskingTokens = true;
        } else {
            maskingTokens = false;
        }

        // Override properties for hidden tokens
        currentLevel = getCurrentLevel();
        if (currentLevel.isHidden()) {
            tagId = currentLevel.getTagId();
            tokenTranslatable = true;
            tokenEscapeContent = false;
        }

        token.addParserMark(tokenTranslatable, currentExternality, tagId, tokenEscapeContent, currentEnvironments);
    }

    private void addLevel(final boolean translatable, final int externality, final boolean escape,
                          final boolean hidden, final boolean is_option) {
        levels.addLast(new ParserLevel(translatable, externality, escape, hidden, is_option));
    }

    private void removeLevel() {
        levels.removeLast();
    }

    public ParserLevel getCurrentLevel() {
        return levels.getLast();
    }

    private boolean onRootLevel() {
        return levels.size() == 1;
    }
}