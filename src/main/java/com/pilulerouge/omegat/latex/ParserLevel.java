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

import java.util.Arrays;
import java.util.LinkedList;

/**
 * This class describes the state of particular depth level in document parser.
 */
class ParserLevel {

    private boolean optionConsumer;                // Consume OPTIONS token or treat it as text?
    private boolean argumentConsumer;              // Consume all incoming arguments
    private final boolean translatable;            // How to treat tokens inside this level
    private Command command;                       // Name of current command in this level or null
    private int tagId;                             // Unique tag id for FORMAT command or group.
    private final int externality;                 // Externality level
    private final boolean escape;                  // Escape special sequences in this level
    private final boolean hidden;                  // Treat level as translatable but hide content from translation.
                                                   // Used in FORMAT commands without translatable content.
    private final boolean isOptionValue;           // True, if level is created for optional command/env argument.
                                                   // Used for detection of an optional argument end.

    private LinkedList<CommandArgument> commandArguments; // Defined arguments of current command.
    private LinkedList<CommandArgument> commandOptions;   // Defined arguments of current command.

    ParserLevel(final boolean translatable, final int externality, final boolean escape, final boolean hidden,
                final boolean isOptionValue) {
        this.translatable = translatable;
        this.externality = externality;
        this.escape = escape;
        this.hidden = hidden;
        this.isOptionValue = isOptionValue;
        this.optionConsumer = false;
        this.argumentConsumer = false;
        this.command = null;
        this.tagId = 0;
        this.commandArguments = new LinkedList<>();
        this.commandOptions = new LinkedList<>();
    }

    void registerCommand(Command command) {
        this.command = command;
        if (command.getType() == CommandType.CONTROL) {
            optionConsumer = true;
            argumentConsumer = true;
            commandArguments = new LinkedList<>();
            commandOptions = new LinkedList<>();
        } else {
            optionConsumer = false;
            argumentConsumer = false;
            commandArguments = new LinkedList<>(Arrays.asList(command.getArgs()));
            commandOptions = new LinkedList<>(Arrays.asList(command.getOptions()));
        }
    }

    void unregisterCommand() {
        command = null;
        tagId = 0;
        argumentConsumer = false;
        optionConsumer = false;
        commandArguments.clear();
        commandOptions.clear();
    }

    boolean hasArgumentInQueue() {
        return !commandArguments.isEmpty();
    }

    boolean hasOptionInQueue() {
        return !commandOptions.isEmpty();
    }

    Command getCommand() {
        return command;
    }

    boolean hasCommand() {
        return command != null;
    }

    boolean isOptionValue() {
        return this.isOptionValue;
    }

    boolean isTranslatable() {
        return translatable;
    }

    boolean isOptionConsumer() {
        return optionConsumer;
    }

    boolean isHidden() {
        return hidden;
    }

    /**
     * Used in unusual cases like `figure` environment
     * @param optionConsumer switch
     */
    void setOptionConsumer(boolean optionConsumer) {
        this.optionConsumer = optionConsumer;
    }

    boolean isArgumentConsumer() {
        return argumentConsumer;
    }

    /**
     * Used in unusual cases like `figure` environment
     * @param argumentConsumer switch
     */
    void setArgumentConsumer(boolean argumentConsumer) {
        this.argumentConsumer = argumentConsumer;
    }

    int getTagId() {
        return tagId;
    }

    void setTagId(int tagId) {
        this.tagId = tagId;
    }

    int getExternality() {
        return externality;
    }

    CommandArgument fetchArgument() {
        return commandArguments.removeFirst();
    }

    CommandArgument fetchOption() {
        return commandOptions.removeFirst();
    }

    boolean doEscape() {
        return escape;
    }

}
