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


import java.awt.Window;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omegat.core.Core;

import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.filters2.AbstractFilter;
import org.omegat.filters2.FilterContext;
import org.omegat.filters2.Instance;
import org.omegat.util.Log;

import static com.pilulerouge.omegat.latex.Command.GROUP_COMMAND_NAME;
import static com.pilulerouge.omegat.latex.Tokenizer.tokenizeDocument;
import static com.pilulerouge.omegat.latex.Util.RB;
import static com.pilulerouge.omegat.latex.Util.readBufferWithLinebreaks;


public class SimpleLatexFilter extends AbstractFilter {

    private String sourceDocument;
    private BufferedWriter fileWriter;
    private final CommandCenter commandCenter;
    private final Parser parser;

    private ListIterator<Token> tokenIterator;

    private static final Pattern STN_HEAD = Pattern.compile("^[\\s\\t\\n]+");
    private static final Pattern STN_TAIL = Pattern.compile("[\\s\\t\\n]+$");

    /**
     * Unescape patterns with substitutions. ORDER IS IMPORTANT!
     */
    private static final Map<Pattern, String> UNESCAPE_MAP = new LinkedHashMap<>();
    static {
        UNESCAPE_MAP.put(Pattern.compile("(?<!\\\\)\\\\%"),"%");
        UNESCAPE_MAP.put(Pattern.compile("(?<!\\\\)\\\\[$]"),"\\$");
        UNESCAPE_MAP.put(Pattern.compile("(?<!\\\\)\\\\_"),"_");
        UNESCAPE_MAP.put(Pattern.compile("(?<!\\\\)\\\\#"),"#");
        UNESCAPE_MAP.put(Pattern.compile("(?<!\\\\)\\\\&"),"&");
        // UNESCAPE_MAP.put(Pattern.compile("(?<!\\\\)\\\\[{]"),"\\{");
        // UNESCAPE_MAP.put(Pattern.compile("(?<!\\\\)\\\\[}]"),"\\}");
        UNESCAPE_MAP.put(Pattern.compile("(?<!\\\\)~")," "); // Non-breaking space
        UNESCAPE_MAP.put(Pattern.compile("(?<!\\\\)[\\\\]~[{][}]"),"~");
    }

    /**
     * Escape patterns with substitutions. ORDER IS IMPORTANT!
     */
    private static final Map<Pattern, String> ESCAPE_MAP = new LinkedHashMap<>();
    static {
        ESCAPE_MAP.put(Pattern.compile("(?<!\\\\)%"), "\\\\%");
        ESCAPE_MAP.put(Pattern.compile("(?<!\\\\)[$]"), "\\\\\\$");
        ESCAPE_MAP.put(Pattern.compile("(?<!\\\\)_"), "\\\\_");
        ESCAPE_MAP.put(Pattern.compile("(?<!\\\\)#"), "\\\\#");
        ESCAPE_MAP.put(Pattern.compile("(?<!\\\\)&"), "\\\\&");
        // ESCAPE_MAP.put(Pattern.compile("(?<!\\\\)[{]"), "\\\\\\{");
        // ESCAPE_MAP.put(Pattern.compile("(?<!\\\\)[}]"), "\\\\\\}");
        ESCAPE_MAP.put(Pattern.compile("~"), "\\\\~\\{\\}");
        ESCAPE_MAP.put(Pattern.compile(" "), "~");
    }

    static {
        Core.registerMarker(new Highlighter());
    }

    public SimpleLatexFilter(final boolean useInternalConfig) throws IOException {
        commandCenter = new CommandCenter(useInternalConfig);
        parser = new Parser(commandCenter);
    }

    public SimpleLatexFilter() throws IOException {
        commandCenter = new CommandCenter(false);
        parser = new Parser(commandCenter);
    }

    @Override
    public void processFile(final BufferedReader reader, final BufferedWriter outfile,
                            final FilterContext fc) throws IOException {
        fileWriter = outfile;
        sourceDocument = readBufferWithLinebreaks(reader);
        // Clean up document level structures
        resetState();
        // Tokenize document
        tokenIterator = tokenizeDocument(sourceDocument);
        // Translate actual text
        translateDocument();
    }


    /**
     * Translate list of tokens. Call self for external parts.
     * @param tokens translatable tokens
     * @return translated string ready to be written out
     */
    private String translateSegment(List<Token> tokens) {

        Token firstToken = tokens.get(0);
        // Return early on STN content
        if (tokens.size() == 1 && firstToken.getType() == TokenType.STN) {
            return sourceDocument.substring(firstToken.getStart(), firstToken.getEnd());
        }

        ParserMark firstTokenMark = firstToken.getParserMark();
        int baseExternality = firstTokenMark.getExternality();
        boolean escape = firstTokenMark.doEscape();
        List<String> envs = firstTokenMark.getEnvironments();

        StringBuilder sb = new StringBuilder();
        List<Token> tagCache = new ArrayList<>();
        Map<String, List<Token>> tagToTokens = new HashMap<>();

        // Append dummy token to simplify code
        int endOfSequence = tokens.get(tokens.size() - 1).getEnd();
        tokens.add(Token.getDummyToken(endOfSequence, baseExternality));

        int firstTokenTagId = 0; // Only for tag generating segments
        Command firstTokenCommand = null;
        for (Token t: tokens) {
            ParserMark m = t.getParserMark();
            int tagId = m.getTagId();
            int externality = m.getExternality();
            String commandName = t.getName();

            boolean doFlushCache = !tagCache.isEmpty() && (
                    (tagId == 0 && externality == baseExternality) || // Text or dummy token usually
                    (tagId > firstTokenTagId && firstTokenTagId > 0 && externality == baseExternality) || // New tag right inside parent one
                    (firstTokenTagId < 0 && tagId != firstTokenTagId) || // New tag begin/end after end of previous tag
                    (firstTokenCommand != null && firstTokenCommand.hasPlainArgument() && tagId == -firstTokenTagId) // Empty command with tag pair like \emph{}
            );
            boolean doAddToCache = (tagId != 0) || // First and last tag case
                    (externality > baseExternality);  // || // External parts of tag

            if (doFlushCache) {
                String tag = requestTag(tagCache);
                sb.append(tag);
                tagToTokens.put(tag, tagCache);
                tagCache = new ArrayList<>();
                firstTokenTagId = 0;
                firstTokenCommand = null;
            }

            if (doAddToCache) {
                if (tagCache.isEmpty()) {
                    firstTokenTagId = tagId;
                    if (commandName != null) {
                        firstTokenCommand = commandCenter.getCommand(commandName);
                    }
                }
                tagCache.add(t);
            } else {
                sb.append(sourceDocument, t.getStart(), t.getEnd());
            }
        }

        String comment = null;
        if (!envs.isEmpty()) {
            StringBuilder csb = new StringBuilder();
            csb.append(RB.getString("ENVIRONMENTS")).append(": ").append(String.join(" > ", envs));
            comment = csb.toString();
        }

        // Fetch initial translation
        String translation = escapeAndProcessEntry(sb.toString(), escape, comment);

        // Reverse tags fetching external translations at the same time
        Map<String, String> translatedTags = new HashMap<>();
        for (Map.Entry<String, List<Token>> e: tagToTokens.entrySet()) {
            String tag = e.getKey();
            List<Token> tagTokens = e.getValue();
            int endPosition = tagTokens.get(tagTokens.size() - 1).getEnd();
            tagTokens.add(Token.getDummyToken(endPosition));

            StringBuilder tsb = new StringBuilder();
            List<Token> extTokens = new ArrayList<>();

            // Search for external parts in token sequence
            for (Token t: tagTokens) {
                ParserMark m = t.getParserMark();
                if (m.getExternality() > baseExternality) {
                    extTokens.add(t);  // This token belongs to external context
                } else {
                    if (!extTokens.isEmpty()) {
                        tsb.append(translateSegment(extTokens)); // Recursive translation
                        extTokens = new ArrayList<>();
                    }
                    tsb.append(sourceDocument, t.getStart(), t.getEnd());
                }
            }
            translatedTags.put(tag, tsb.toString());
        }

        // Reconstruct final translation
        for (Map.Entry<String, String> e: translatedTags.entrySet()) {
            String tag = e.getKey();
            String replacement = e.getValue();
            translation = translation.replace(tag, replacement);
        }

        return translation;
    }

    private String requestTag(List<Token> tokens) {
        Token firstToken = tokens.get(0);
        String commandName = firstToken.getName();
        Command command = commandCenter.getCommand(commandName);
        ParserMark mark = firstToken.getParserMark();
        int tagId = mark.getTagId();
        String tag;

        Token lastToken = tokens.get(tokens.size() - 1);
        String content = sourceDocument.substring(firstToken.getStart(), lastToken.getEnd());

        boolean noPlainArguments = command == null || !command.hasPlainArgument();

        // Avoid creating closed tokens for virtual group command
        if (noPlainArguments || (tokens.size() == 1 && !Objects.equals(commandName, GROUP_COMMAND_NAME))) {
            if (tagId > 0) { // Closed tag case
                tag = commandCenter.getFirstOrClosedTag(content, command, tagId, true);
            } else if (tagId < 0) { // Last tag case
                tag = commandCenter.getLastTag(-tagId);
            } else {
                String message = "Failed to create tag from string `" + content + "` at position " +
                        firstToken.getStart() + ".\nIt can be a bug or a bad document.";
                throw new SimpleLatexFilterException(message);
            }
        } else { // First tag case
            tag = commandCenter.getFirstOrClosedTag(content, command, tagId, false);
        }
        return tag;
    }

    private void translateDocument() throws IOException {
        Token token;
        List<Token> translationCache = new ArrayList<>();

        while(tokenIterator.hasNext()) {
            token = tokenIterator.next();
            parser.processToken(token);
            ParserMark mark = token.getParserMark();
            if (mark.isTranslatable()) {
                translationCache.add(token);
                continue;
            }

            // Flush translation cache
            if (!translationCache.isEmpty()) {
                String translation = translateSegment(translationCache);
                fileWriter.write(translation);
                translationCache.clear();
            }

            String tokenBody = sourceDocument.substring(token.getStart(), token.getEnd());
            fileWriter.write(tokenBody);
        }
    }

    private String escapeAndProcessEntry(String content, boolean escape, String comment) {
        // Unescape special characters
        if (escape) {
            for (Map.Entry<Pattern, String> e: UNESCAPE_MAP.entrySet()) {
                Pattern pattern = e.getKey();
                String replacement = e.getValue();
                Matcher matcher = pattern.matcher(content);
                content = matcher.replaceAll(replacement);
            }
        }

        content = trimAndProcessEntry(content, comment);
        // Escape special characters
        if (escape) {
            for (Map.Entry<Pattern, String> e: ESCAPE_MAP.entrySet()) {
                Pattern pattern = e.getKey();
                String replacement = e.getValue();
                Matcher matcher = pattern.matcher(content);
                content = matcher.replaceAll(replacement);
            }
        }
        return content;
    }

    private String trimAndProcessEntry(final String content, final String comment) {
        int headPad, tailPad;

        Matcher matcher = STN_HEAD.matcher(content);
        if (matcher.find()) {
            headPad = matcher.end();
        } else {
            headPad = 0;
        }

        matcher = STN_TAIL.matcher(content);
        if (matcher.find()) {
            tailPad = matcher.start();
        } else {
            tailPad = content.length();
        }

        return content.substring(0, headPad) + processEntry(content.substring(headPad, tailPad), comment) +
            content.substring(tailPad);
    }

    private void resetState() {
        commandCenter.reset();
        parser.reset();
    }

    private static IApplicationEventListener generateIApplicationEventListener() {
        return new IApplicationEventListener() {

            @Override
            public void onApplicationStartup() {
                Core.getEditor().registerPopupMenuConstructors(0, new PopupMenuConstructor());
            }

            @Override
            public void onApplicationShutdown() {
            }
        };
    }

    public static void loadPlugins() {
        Core.registerFilterClass(SimpleLatexFilter.class);
        CoreEvents.registerApplicationEventListener(generateIApplicationEventListener());
    }

    public static void unloadPlugins() {
    }

    @Override
    public String getFileFormatName() {
        return Util.FILTER_NAME;
    }

    @Override
    public boolean isSourceEncodingVariable() {
        return false;
    }

    @Override
    public boolean isTargetEncodingVariable() {
        return false;
    }

    @Override
    public Instance[] getDefaultInstances() {
        return new Instance[] {
                new Instance("*.tex", "UTF-8", "UTF-8"),
        };
    }

    @Override
    protected boolean requirePrevNextFields() {
        return true;
    }

    @Override
    protected boolean isFileSupported(final BufferedReader reader) {
        return true;
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    @Override
    public Map<String, String> changeOptions(final Window parent,
                                             final Map<String, String> config) {
        try {
            SettingsDialog dialog = new SettingsDialog(parent, config);
            dialog.setVisible(true);
            return dialog.getOptions();
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }
}
