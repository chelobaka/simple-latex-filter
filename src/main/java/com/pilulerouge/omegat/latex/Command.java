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
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Command {

    final static String GROUP_COMMAND_NAME = "virtual-group-command";
    final static String GROUP_COMMAND_TAG = "G0";

    final static String INLINE_MATH_COMMAND_NAME = "inline-math";
    final static String INLINE_MATH_COMMAND_TAG = "Math";

    final static String UNKNOWN_COMMAND_NAME = "unknown-command";
    final static String UNKNOWN_COMMAND_TAG = "U";

    // Used to mask untranslatable tokens as formatting commands inside external context
    final static String MASK_COMMAND_NAME = "mask-command";
    final static String MASK_COMMAND_TAG = "M";

    final static HashSet<String> EXPLICIT_NUMBERED_TAG_COMMANDS = Stream.of(
            UNKNOWN_COMMAND_NAME,
            MASK_COMMAND_NAME,
            INLINE_MATH_COMMAND_NAME
    ).collect(Collectors.toCollection(HashSet::new));

    private final CommandType type;
    private final String name;
    private final String tag;
    private final boolean numberedTags;
    private final CommandArgument[] args;
    private final CommandArgument[] options;
    private final boolean hasPlainArgument;

    public Command(CommandType type, String name, String tag, CommandArgument[] args, CommandArgument[] options) {
        this.name = name;
        this.type = type;
        this.tag = tag;
        this.args = args;
        this.options = options;
        this.hasPlainArgument = Arrays.stream(args).anyMatch(a -> a.isTranslatable() && !a.isExternal());
        this.numberedTags = (type == CommandType.FORMAT &&
                Arrays.stream(args).anyMatch(CommandArgument::isExternal)) ||
                EXPLICIT_NUMBERED_TAG_COMMANDS.contains(name) ||
                (tag != null && tag.matches("[A-Za-z]+"));
     }

    public CommandType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public boolean hasNumberedTags() {
        return numberedTags;
    }

    public CommandArgument[] getArgs() {
        return args;
    }

    public CommandArgument[] getOptions() {
        return options;
    }

    public boolean isUnknown() {
        return name.equals(UNKNOWN_COMMAND_NAME);
    }

    /**
     * Plain argument as opposed to external requires tag pair for
     * formatting command. No such argument implies a closed tag.
     * @return result
     */
    public boolean hasPlainArgument() {
        return hasPlainArgument;
    }
}
