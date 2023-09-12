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
public class ParserLevel {

    private boolean optionsConsumer;               // Consume OPTIONS token or treat it as text?
    private boolean argumentConsumer;              // Consume all incoming arguments
    private final boolean translatable;            // How to treat tokens inside this level
    private Command command;                       // Name of current command in this level or null
    private int tagId;                             // Unique tag id for FORMAT command or group.
    private final int externality;                 // Externality level
    private final boolean escape;                  // Escape special sequences in this level
    private final boolean hidden;                  // Treat level as translatable but hide content from translation

    private LinkedList<CommandArgument> commandArguments; // Defined arguments of current command.

    public ParserLevel(final boolean translatable, final int externality, final boolean escape, final boolean hidden) {
        this.translatable = translatable;
        this.externality = externality;
        this.escape = escape;
        this.hidden = hidden;
        this.optionsConsumer = false;
        this.argumentConsumer = false;
        this.command = null;
        this.tagId = 0;
        this.commandArguments = new LinkedList<>();
    }

    public void registerCommand(Command command) {
        this.command = command;
        if (command.getType() == CommandType.CONTROL) {
            optionsConsumer = true;
            argumentConsumer = true;
            commandArguments = new LinkedList<>();
        } else {
            optionsConsumer = false;
            argumentConsumer = false;
            commandArguments = new LinkedList<>(Arrays.asList(command.getArgs()));
        }
    }

    public void unregisterCommand() {
        command = null;
        tagId = 0;
        argumentConsumer = false;
        optionsConsumer = false;
        commandArguments.clear();
    }

    public boolean hasArgumentsInQueue() {
        return !commandArguments.isEmpty();
    }

    public Command getCommand() {
        return command;
    }

    public boolean hasCommand() {
        return command != null;
    }

    public boolean isTranslatable() {
        return translatable;
    }

    public boolean isOptionsConsumer() {
        return optionsConsumer;
    }

    public boolean isHidden() {
        return hidden;
    }

    /**
     * Used in unusual cases like `figure` environment
     * @param optionsConsumer switch
     */
    public void setOptionsConsumer(boolean optionsConsumer) {
        this.optionsConsumer = optionsConsumer;
    }

    public boolean isArgumentConsumer() {
        return argumentConsumer;
    }

    /**
     * Used in unusual cases like `figure` environment
     * @param argumentConsumer switch
     */
    public void setArgumentConsumer(boolean argumentConsumer) {
        this.argumentConsumer = argumentConsumer;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public int getExternality() {
        return externality;
    }

    public CommandArgument fetchArgument() {
        return commandArguments.removeFirst();
    }

    public boolean doEscape() {
        return escape;
    }

}
