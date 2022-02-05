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

import org.omegat.core.Core;
import org.omegat.gui.editor.IPopupMenuConstructor;
import org.omegat.gui.editor.SegmentBuilder;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.pilulerouge.omegat.latex.Util.RB;


public class PopupMenuConstructor implements IPopupMenuConstructor {

    private static final Pattern TAG_PAIR = Pattern.compile("(<([a-zA-Z]\\d+)>).+?(</\\2>)");
    private static final Pattern SINGLE_TAG = Pattern.compile("<([a-zA-Z]\\d+)/>");

    private static final Map<String, String> latexCommands = new LinkedHashMap<>();
    static {
        latexCommands.put("POPUP_MENU_FORMAT_BOLD", "textbf");
        latexCommands.put("POPUP_MENU_FORMAT_EMPHASIS", "emph");
        latexCommands.put("POPUP_MENU_FORMAT_SUPERSCRIPT", "textsuperscript");
        latexCommands.put("POPUP_MENU_FORMAT_SUBSCRIPT", "textsubscript");
        latexCommands.put("POPUP_MENU_INSERT_FOOTNOTE", "footnote");
    }

    @Override
    public void addItems(final JPopupMenu menu,
                         final JTextComponent comp,
                         final int mousepos,
                         final boolean isInActiveEntry,
                         final boolean isInActiveTranslation,
                         final SegmentBuilder sb) {

        if (!Util.currentlyUsingThisFilter()) {
            return;
        }

        String selection = Core.getEditor().getSelectedText();
        if (selection == null) {
            selection = "";
        }

        JMenu pluginSubMenu = new JMenu();
        pluginSubMenu.setText(RB.getString("POPUP_MENU_NAME"));

        /* Found tags */
        String src = Core.getEditor().getCurrentEntry().getSrcText();
        Set<String> foundTags = new HashSet<>();
        Matcher matcher = TAG_PAIR.matcher(src);
        while (matcher.find()) {

            if (foundTags.contains(matcher.group(2))) {
                continue;
            } else {
                foundTags.add(matcher.group(2));
            }

            JMenuItem item = new JMenuItem();
            item.setText(matcher.group(1) + "…" + matcher.group(3));
            String insertion = matcher.group(1) + selection + matcher.group(3);
            item.addActionListener(e -> Core.getEditor().insertText(insertion));
            pluginSubMenu.add(item);
        }

        matcher = SINGLE_TAG.matcher(src);
        while (matcher.find()) {
            String singleTag = matcher.group(0);
            JMenuItem item = new JMenuItem();
            item.setText(singleTag);
            item.addActionListener(e -> Core.getEditor().insertText(singleTag));
            pluginSubMenu.add(item);
        }
        pluginSubMenu.addSeparator();

        /* Original formatting items */
        for (Map.Entry<String, String> entry : latexCommands.entrySet()) {
            JMenuItem item = new JMenuItem();
            item.setText(RB.getString(entry.getKey()));
            String insertion = "\\" + entry.getValue() + "{" + selection + "}";
            item.addActionListener(e -> Core.getEditor().insertText(insertion));
            pluginSubMenu.add(item);
        }

        menu.addSeparator();
        menu.add(pluginSubMenu);
        menu.addSeparator();
    }
}
