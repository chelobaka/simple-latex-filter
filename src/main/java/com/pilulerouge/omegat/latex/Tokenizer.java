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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {

    /**
     * Token patterns possibly canceling each other. Free order.
     */
    private static final Map<Pattern, TokenType> CONCURRENT_PATTERN_TOKENS = new HashMap<>();
    static {
        // URL in href and url commands may contain % symbol
        CONCURRENT_PATTERN_TOKENS.put(
                Pattern.compile("(?<=(?:^|[^\\\\])\\\\(?:href|url)\\{).+?(?=})"),
                TokenType.TEXT
        );
        // LaTeX comments
        CONCURRENT_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)%.*?$", Pattern.MULTILINE),
                TokenType.COMMENT
        );
        // Verbatim environment
        CONCURRENT_PATTERN_TOKENS.put(
                Pattern.compile("(?<=(?:^|[^\\\\])\\\\begin\\{verbatim\\*?}).+?(?=\\\\end\\{verbatim\\*?})"),
                TokenType.VERBATIM
        );
        // \verb command, split into subtokens later
        CONCURRENT_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)(\\\\verb)([^a-z])(.+?)(\\2)"),
                TokenType.TEMP_VERB
        );
    }

    /**
     * Token patterns matched by strict order.
     */
    private static final Map<Pattern, TokenType> ORDERED_PATTERN_TOKENS = new LinkedHashMap<>();
    static {
        // Early match for \~{} to avoid matching group boundaries
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)[\\\\]~[{][}]"),
                TokenType.TEXT
        );
        // Inline math
        ORDERED_PATTERN_TOKENS.put( // $...$
                Pattern.compile("(?<!\\\\)\\$.+?(?<!\\\\)\\$"),
                TokenType.INLINE_MATH
        );
        ORDERED_PATTERN_TOKENS.put( // \(...\)
                Pattern.compile("(?<!\\\\)\\\\[(].+?(?<!\\\\)\\\\[)]"),
                TokenType.INLINE_MATH
        );
        ORDERED_PATTERN_TOKENS.put( // \begin{math}...\end{math}
                Pattern.compile("(?<!\\\\)\\\\begin\\{math}.+?(?<!\\\\)\\\\end\\{math}"),
                TokenType.INLINE_MATH
        );
        // Display math
        ORDERED_PATTERN_TOKENS.put( // \[...\]
                Pattern.compile("(?<!\\\\)\\\\\\[.+?(?<!\\\\)\\\\]"),
                TokenType.DISPLAY_MATH
        );
        ORDERED_PATTERN_TOKENS.put( // \begin{displaymath}...\end{displaymath}
                Pattern.compile("(?<!\\\\)\\\\begin\\{displaymath}.+?(?<!\\\\)\\\\end\\{displaymath}"),
                TokenType.DISPLAY_MATH
        );
        ORDERED_PATTERN_TOKENS.put( // \begin{equation}...\end{equation} and starred version
                Pattern.compile("(?<!\\\\)\\\\begin\\{equation\\*?}.+?(?<!\\\\)\\\\end\\{equation\\*?}"),
                TokenType.DISPLAY_MATH
        );
        // Environment boundaries
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)\\\\begin\\*?\\{([a-z]{2,})}"),
                TokenType.ENV_BEGIN
        );
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)\\\\end\\*?\\{([a-z]{2,})}"),
                TokenType.ENV_END
        );
        // Command / environment options
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)\\[.+?]"),
                TokenType.OPTIONS
        );
        // Commands
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)\\\\([a-z]{2,})\\*?"),
                TokenType.COMMAND
        );
        // Group boundaries
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)\\{"),
                TokenType.GROUP_BEGIN
        );
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)}"),
                TokenType.GROUP_END
        );
        // Table column separator
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("(?<!\\\\)&"),
                TokenType.AMPERSAND
        );
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("\\\\\\\\"),
                TokenType.LINE_BREAK
        );
        ORDERED_PATTERN_TOKENS.put(
                Pattern.compile("\\n\\s*\\n"),
                TokenType.EMPTY_LINE
        );
    }

    // Pattern for STN token
    private static final Pattern STN_PATTERN = Pattern.compile("^[\\s\\t\\n]+$");

    static ListIterator<Token> tokenizeDocument(String sourceText) {
        LinkedList<Token> tokens = new LinkedList<>();
        int[] residency = new int[sourceText.length()]; // Tracks token overlap
        Arrays.fill(residency, -1);

        // Find concurrent tokens
        findConcurrentTokens(sourceText, tokens, residency);

        // Collect document tokens
        String tokenName;
        for (Map.Entry<Pattern, TokenType> entry : ORDERED_PATTERN_TOKENS.entrySet()) {
            Pattern pattern = entry.getKey();
            TokenType tokenType = entry.getValue();
            Matcher matcher = pattern.matcher(sourceText);
            while (matcher.find()) {

                int tokenBegin = matcher.start();
                int tokenEnd = matcher.end();

                // Check token overlap
                boolean vacant = true;
                for (int i = tokenBegin; i < tokenEnd; i++) {
                    if (residency[i] >= 0) {
                        vacant = false;
                        break;
                    }
                }

                if (!vacant) {
                    continue;
                }

                // Fill residency
                for (int i = tokenBegin; i < tokenEnd; i++) {
                    residency[i] = tokenType.ordinal();
                }

                if (matcher.groupCount() > 0) {
                    tokenName = matcher.group(1);
                } else {
                    tokenName = null;
                }

                tokens.add(new Token(tokenType, matcher.start(), matcher.end(), tokenName));
            }
        }
        Collections.sort(tokens);

        // Generate TEXT/STN tokens between collected tokens
        // Add DUMMY token at the end for convenience
        tokens.add(Token.getDummyToken(sourceText.length()));
        ArrayList<Token> textTokens = new ArrayList<>();
        Token prevToken = Token.getDummyToken(0); // Dummy token for starters
        TokenType newTokenType;
        int begin, end;
        for (Token thisToken : tokens) {
            begin = prevToken.getEnd();
            end = thisToken.getStart();
            if (begin < end) {
                if (STN_PATTERN.matcher(sourceText.substring(begin, end)).matches()) {
                    newTokenType = TokenType.STN;
                } else {
                    newTokenType = TokenType.TEXT;
                }
                textTokens.add(new Token(newTokenType, begin, end, null));
            }
            prevToken = thisToken;
        }
        tokens.addAll(textTokens);
        Collections.sort(tokens);

        // When all tokens are created and sorted make an iterator
        return tokens.listIterator();
    }

    /**
     * Some token types cannot be reliably detected purely by correct order of searching.
     * This method tries to find such tokens using order of appearance in source text.
     * @param text document string
     * @param tokens empty list of tokens which will be used by calling method further
     * @param residency residency array to be used by calling method further
     */
    private static void findConcurrentTokens(String text, LinkedList<Token> tokens, int[] residency) {
        Map<Token, MatchResult> verbMatches = new HashMap<>();
        List<Token> allTokens = new ArrayList<>();
        for (Map.Entry<Pattern, TokenType> entry : CONCURRENT_PATTERN_TOKENS.entrySet()) {
            Pattern pattern = entry.getKey();
            TokenType tokenType = entry.getValue();
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                Token token = new Token(tokenType, matcher.start(), matcher.end(), null);
                allTokens.add(token);
                // Keep \verb matches to split tokens later
                if (tokenType == TokenType.TEMP_VERB) {
                    verbMatches.put(token, matcher.toMatchResult());
                }
            }
        }

        Collections.sort(allTokens);

        int prevTokenEnd = 0;
        for (Token t: allTokens) {
            if (t.getStart() >= prevTokenEnd) {
                if (t.getType() == TokenType.TEMP_VERB) {
                    MatchResult mr = verbMatches.get(t);
                    tokens.add(new Token(TokenType.COMMAND, mr.start(1), mr.end(1), "verb"));
                    tokens.add(new Token(TokenType.GROUP_BEGIN, mr.start(2), mr.end(2), null));
                    tokens.add(new Token(TokenType.TEXT, mr.start(3), mr.end(3), null));
                    tokens.add(new Token(TokenType.GROUP_END, mr.start(4), mr.end(4), null));
                } else {
                    tokens.add(t);
                }
                prevTokenEnd = t.getEnd();
                // Fill residency
                for (int i = t.getStart(); i < t.getEnd(); i++) {
                    residency[i] = t.getType().ordinal();
                }
            }
        }
    }

    private Tokenizer() {
        // No instances allowed
    }
}
