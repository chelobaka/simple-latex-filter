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
    private TokenType prevTokenType;

    public Parser(CommandCenter cc) {
        commandCenter = cc;
        levels = new LinkedList<>();
        lastTagId = 0;
        maskingTokens = false;
        environments = new LinkedList<>();
        prevTokenType = null;
    }

    public void reset() {
        lastTagId = 0;
        maskingTokens = false;
        levels.clear();
        levels.add(new ParserLevel(true, 0, true, false)); // Root level
        environments.clear();
        currentEnvironments = Collections.emptyList();
        prevTokenType = null;
    }

    public void processToken(final Token token) {

        Command command;
        int tagId = 0;

        ParserLevel currentLevel = getCurrentLevel();
        int currentExternality = currentLevel.getExternality();
        boolean tokenTranslatable = currentLevel.isTranslatable();
        boolean tokenEscapeContent = currentLevel.doEscape();

        TokenType tt = token.getType();

        // Unregister command if this token is not options or an argument start
        if (tt != TokenType.GROUP_BEGIN && tt != TokenType.OPTIONS && currentLevel.hasCommand()) {
            currentLevel.unregisterCommand();
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
            case OPTIONS:
                if (currentLevel.isHidden()) {
                    break;
                }
                if (currentLevel.isOptionsConsumer()) {
                    currentLevel.setOptionsConsumer(false);
                    tokenTranslatable = false;
                    break;
                }
                // Handle command options
                if (prevTokenType == TokenType.COMMAND) {
                    command = currentLevel.getCommand(); // Shouldn't fail I guess
                    CommandType ct = command.getType();
                    if (ct == CommandType.FORMAT) {
                        tagId = currentLevel.getTagId();
                    } else {
                        tokenTranslatable = false;
                    }
                    break;
                }
                // Table row hints like [1ex]
                if (!environments.isEmpty() && commandCenter.isTableEnvironment(environments.getLast())) {
                    tokenTranslatable = false;
                    break;
                }
                break;
            case GROUP_BEGIN:
                boolean newLevelTranslatable = currentLevel.isTranslatable() && !currentLevel.isArgumentConsumer();
                boolean newLevelEscapeContent = currentLevel.doEscape();
                int newLevelExternality = currentExternality;
                boolean newLevelIsHidden = currentLevel.isHidden();

                if (currentLevel.hasArgumentsInQueue() && !newLevelIsHidden && newLevelTranslatable) {
                    tagId = currentLevel.getTagId();
                    CommandArgument currentArg = currentLevel.fetchArgument();
                    // Flag for non-translatable argument of FORMAT command
                    newLevelIsHidden = (!currentArg.isTranslatable() &&
                            currentLevel.getCommand().getType() == CommandType.FORMAT);
                    newLevelTranslatable = (currentArg.isTranslatable() && newLevelTranslatable) || newLevelIsHidden;
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
                addLevel(newLevelTranslatable, newLevelExternality, newLevelEscapeContent, newLevelIsHidden);
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
                ParserLevel parentLevel = getCurrentLevel();
                currentExternality = parentLevel.getExternality();
                tagId = parentLevel.getTagId();

                if (parentLevel.hasCommand() && parentLevel.getCommand().getType() != CommandType.FORMAT) {
                    tokenTranslatable = false;
                }

                if (parentLevel.hasCommand() && !parentLevel.hasArgumentsInQueue() && !parentLevel.isArgumentConsumer()) {
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
                    currentLevel.setOptionsConsumer(true);
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

        // Mark this token
        token.addParserMark(tokenTranslatable, currentExternality, tagId, tokenEscapeContent, currentEnvironments);

        // Remember token type for next iteration
        prevTokenType = tt;
    }

    private void addLevel(final boolean translatable, final int externality, final boolean escape, final boolean hidden) {
        levels.addLast(new ParserLevel(translatable, externality, escape, hidden));
    }

    private void removeLevel() {
        levels.removeLast();
    }

    private ParserLevel getCurrentLevel() {
        return levels.getLast();
    }

    private boolean onRootLevel() {
        return levels.size() == 1;
    }
}