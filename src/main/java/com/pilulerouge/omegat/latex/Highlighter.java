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

import org.omegat.core.data.SourceTextEntry;
import org.omegat.gui.editor.mark.IMarker;
import org.omegat.gui.editor.mark.Mark;
import org.omegat.util.gui.Styles;

import javax.swing.text.AttributeSet;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class Highlighter implements IMarker {

    private final Map<Pattern, AttributeSet> patternAttributes;

    Highlighter() {
        Map<String, String> options = Util.getFilterOptions();

        patternAttributes = new HashMap<>();

        Color color = Color.decode(
                options.getOrDefault(SettingsDialog.CONF_LATEX_COMMAND_COLOR,
                        SettingsDialog.DEFAULT_LATEX_COMMAND_COLOR));

        patternAttributes.put(Pattern.compile("(?<!\\\\)\\\\[a-z]{2,}\\*?"), // commands
                Styles.createAttributeSet(color, null, null, null));

        color = Color.decode(
                options.getOrDefault(SettingsDialog.CONF_CURLY_BRACE_COLOR,
                        SettingsDialog.DEFAULT_CURLY_BRACE_COLOR));

        patternAttributes.put(Pattern.compile("(?<!\\\\)[{}]"), // curly braces
                Styles.createAttributeSet(color, null, null, null));

    }

    public List<Mark> getMarksForEntry(final SourceTextEntry ste, final String sourceText,
                                       final String translationText, final boolean isActive) {

        if (translationText == null || !Util.currentlyUsingThisFilter()) {
            return null;
        }

        List<Mark> result = new ArrayList<>();

        for (Map.Entry<Pattern, AttributeSet> e: patternAttributes.entrySet()) {

            Matcher matcher = e.getKey().matcher(translationText);
            if (matcher.find()) {
                do {
                    Mark mark = new Mark(Mark.ENTRY_PART.TRANSLATION, matcher.start(), matcher.end());
                    mark.painter = null;
                    mark.attributes = e.getValue();
                    result.add(mark);
                } while (matcher.find());
            }
        }
        return result;
    }
}