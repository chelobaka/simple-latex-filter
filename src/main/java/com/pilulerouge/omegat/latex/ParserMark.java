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

import java.util.List;

public class ParserMark {
    private final boolean translatable;
    private final boolean escape;
    private final int externality;
    private final int tagId;
    private final List<String> environments;

    public ParserMark(boolean translatable, int externality, int tagId, boolean escape, List<String> environments) {
        this.translatable = translatable;
        this.escape = escape;
        this.tagId = tagId;
        this.externality = externality;
        this.environments = environments;
    }

    public boolean isTranslatable() {
        return translatable;
    }

    public boolean doEscape() {
        return escape;
    }

    public int getExternality() {
        return externality;
    }

    public int getTagId() {
        return tagId;
    }

    public List<String> getEnvironments() {
        return environments;
    }

}
