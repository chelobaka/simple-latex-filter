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

package org.omegat.filters;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import com.pilulerouge.omegat.latex.SimpleLatexFilter;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleLatexFilterTest extends TestFilterBase {

    private final static String[] TEST_STRINGS = new String[] {
        "Hello, world!",
        "Example of <b1>bold</b1> and <e1>emphasis</e1>.",
        "Down the rabbit hole",
        "Example of closed tag <e1/> and <G0>virtual group command</G0>.",
        "Let's try a footnote with a hyperlink inside<f1/><f0/>",
        "<e1>See:</e1> <h1>Wikipedia</h1>",
        "https://wikipedia.org",
        "We can split <e1>tag pairs between segments.",
        "And it should work</e1> as expected<f2/>.",
        "Another footnote for tag numbering check.",
        "Here is an <U1>unknown command</U1>.",
        "Next <U2>unknown command</U2><G0>should</G0><G0>receive</G0> next numbered tag.",
        "Now let's make empty footnote <f3/> and emphasis <e1></e1>",
        "<f4/>",
        "External content<M1/> with control<M2/>sequence.",
        "A tag <b1><u1>at the beginning</u1></b1> a of parent tag.",
        "Testing environments",
        "You should see only this",
        "Option consumer example",
        "Argument consumer example",
        "Let's test escaping: % $ _ # & \\{ \\} ` ` ~",
        "URL with escaped characters <url1/>",
        "http://foo.bar?a=1&b=%20%40",
        // Table cells (spaces are trimmed by OmegaT)
        "Country List",
        "Country Name or Area Name",
        "ISO ALPHA 2 Code",
        "ISO ALPHA 3 Code",
        "ISO numeric Code",
        "Afghanistan",
        "AF",
        "AFG",
        "004",
        "Inline math types: <Math1/>, <Math2/>, <Math3/>.",
        "This is verbatim \\emph{text} % Not a comment \\verb+HERE+",
        "A `verb` command test: <verb1/>",
        "This~is~unescaped",
        "A <ls1>slight</ls1> and <ls2>heavy</ls2> letterspaced text.",
        "See figures <r1/> and <r2/> on page <pr1/>.",
        "☣️",
        "List item text",
        "Another list item text",
        "Test [square brackets] text",
        "short name",
        "long name"
    };

    String testDocument = "/test.tex";

    @Test
    public void testFilterFeatures() throws Exception {
        List<String> entries = parse(new SimpleLatexFilter(true), testDocument);
        for (int i = 0; i < TEST_STRINGS.length; i++) {
            assertEquals(TEST_STRINGS[i], entries.get(i));
        }
    }

    @Test
    public void testOutputIntegrity() throws Exception {
        translate(new SimpleLatexFilter(true), testDocument, Collections.emptyMap());
        compareBinary(new File("src/test/resources" + testDocument), outFile);
    }

    @Test
    public void testTranslateNew() throws Exception {
        translateText(new SimpleLatexFilter(), testDocument);
    }

}
