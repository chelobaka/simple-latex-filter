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

import org.junit.Test;
import com.pilulerouge.omegat.latex.SimpleLatexFilter;

import static org.junit.Assert.*;

public class SimpleLatexFilterTest extends TestFilterBase {

    String testDocument = "/test.tex";

    @Test
    public void testFilterFeatures() throws Exception {
        List<String> entries = parse(new SimpleLatexFilter(), testDocument);
        int i = 0;
        assertEquals("Hello, world!", entries.get(i++));
        assertEquals("Example of <b1>bold</b1> and <e1>emphasis</e1>.", entries.get(i++));
        assertEquals("Down the rabbit hole", entries.get(i++));
        assertEquals("Example of closed tag <e1/> and <G0>virtual group command</G0>.", entries.get(i++));
        assertEquals("Let's try a footnote with a hyperlink inside<f1/>", entries.get(i++));
        assertEquals("<e1>See:</e1> <h1>Wikipedia</h1>", entries.get(i++));
        assertEquals("https://wikipedia.org", entries.get(i++));
        assertEquals("We can split <e1>tag pairs between segments.", entries.get(i++));
        assertEquals("And it should work</e1> as expected<f2/>.", entries.get(i++));
        assertEquals("Another footnote for tag numbering check.", entries.get(i++));
        assertEquals("Here is an <U1>unknown command</U1>.", entries.get(i++));
        assertEquals("Next <U2>unknown command</U2><G0>should</G0><G0>receive</G0> next numbered tag.", entries.get(i++));
        assertEquals("Now let's make empty footnote <f3/> and emphasis <e1></e1>", entries.get(i++));
        assertEquals("<f4/>", entries.get(i++));
        assertEquals("External content<M1/> with control<M2/>sequence.", entries.get(i++));
        assertEquals("A tag <b1><u1>at the beginning</u1></b1> a of parent tag.", entries.get(i++));
        assertEquals("Testing environments", entries.get(i++));
        assertEquals("You should see only this", entries.get(i++));
        assertEquals("Option consumer example", entries.get(i++));
        assertEquals("Argument consumer example", entries.get(i++));
        assertEquals("Let's test escaping: % $ _ # & \\{ \\} ` ` ~", entries.get(i++));
        assertEquals("URL with escaped characters <url1/>", entries.get(i++));
        assertEquals("http://foo.bar?a=1&b=%20%40", entries.get(i++));
        // Table cells (spaces are trimmed by OmegaT)
        assertEquals("Country List", entries.get(i++));
        assertEquals("\n    Country Name or Area Name ", entries.get(i++));
        assertEquals(" ISO ALPHA 2 Code ", entries.get(i++));
        assertEquals("ISO ALPHA 3 Code ", entries.get(i++));
        assertEquals(" ISO numeric Code", entries.get(i++));
        assertEquals("\n    Afghanistan ", entries.get(i++));
        assertEquals(" AF ", entries.get(i++));
        assertEquals(" AFG ", entries.get(i++));
        assertEquals(" 004 ", entries.get(i++));
        assertEquals("Inline math types: <Math1/>, <Math2/>, <Math3/>.", entries.get(i++));
        assertEquals("This is verbatim \\emph{text} % Not a comment \\verb+HERE+", entries.get(i++));
        assertEquals("A `verb` command test: <verb1/>", entries.get(i++));
        assertEquals("This~is~unescaped", entries.get(i++));
        assertEquals("A <ls1>slight</ls1> and <ls2>heavy</ls2> letterspaced text.", entries.get(i++));
    }

    @Test
    public void testOutputIntegrity() throws Exception {
        translate(new SimpleLatexFilter(), testDocument, Collections.emptyMap());
        compareBinary(new File("src/test/resources" + testDocument), outFile);
    }


    @Test
    public void testTranslateNew() throws Exception {
        translateText(new SimpleLatexFilter(), testDocument);
    }

}
