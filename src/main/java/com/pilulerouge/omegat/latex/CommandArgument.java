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

public class CommandArgument {

    private final boolean translatable;
    private final boolean external;
    private final boolean escape;

    public CommandArgument(final boolean translatable, final boolean external, final boolean escape) {
        this.translatable = translatable;
        this.external = external;
        this.escape = escape;
    }

    public boolean isTranslatable() {
        return translatable;
    }

    public boolean isExternal() {
        return external;
    }

    public boolean doEscape() {
        return escape;
    }
}
