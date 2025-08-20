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
import java.util.List;

/**
 * Token is a basic structure block used for parsing document.
 */
public class Token implements Comparable<Token> {
    private final int start;
    private final int end;
    private final TokenType type;
    private String name; // Name can be used by command or environment
    private ParserMark parserMark;

    Token(final TokenType type, final int start, final int end, final String name) {
        this.type = type;
        this.start = start;
        this.end = end;
        this.name = name;
        this.parserMark = null;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public TokenType getType() {
        return type;
    }

    /**
     * Parser might decide to change token command name
     * @param name new token name
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ParserMark getParserMark() {
        return parserMark;
    }

    public void addParserMark(boolean translatable, int externality, int tagId, boolean escape,
                              List<String> environments) {
        this.parserMark = new ParserMark(translatable, externality, tagId, escape, environments);
    }

    @Override
    public int compareTo(final Token that) {
        return Integer.compare(this.start, that.start);
    }

    /**
     * Create marked dummy token.
     * @param position token position
     * @return Dummy token with ParserMark attached
     */
    public static Token getDummyToken(int position, int externality) {
        Token token = new Token(TokenType.DUMMY, position, position, null);
        token.addParserMark(false, externality,0, true, Collections.emptyList());
        return token;
    }

    public static Token getDummyToken(int position) {
        return getDummyToken(position, 0);
    }
}