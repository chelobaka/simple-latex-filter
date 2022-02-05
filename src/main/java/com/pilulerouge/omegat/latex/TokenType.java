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

/**
 * Types of tokens recognized in LaTeX document.
 */
public enum TokenType {
    COMMENT,      // % LaTeX comment
    ENV_BEGIN,    // \begin{name}
    ENV_END,      // \end{name}
    COMMAND,      // Any LaTeX command except begin/end
    GROUP_BEGIN,  // {
    GROUP_END,    // }
    OPTIONS,      // [foo=bar,boo=moo]
    AMPERSAND,    // Column separator in table environments
    INLINE_MATH,  // All flavors of inline math become FORMAT commands without translatable content
    DISPLAY_MATH, // These are copied without translation
    VERBATIM,     // Text inside verbatim environment
    TEMP_VERB,    // Temporary token for \verb command split into subtokens later
    LINE_BREAK,   // \\
    EMPTY_LINE,   // Self-descriptive
    TEXT,         // Text between between previous tokens
    STN,          // A TEXT token which contain only spaces, tabs or newlines. Not really used.
    DUMMY         // Zero length token used to simplify logic
}
