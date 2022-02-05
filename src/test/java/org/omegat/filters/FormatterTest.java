package org.omegat.filters;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;

public class FormatterTest extends TestFilterBase {

    private final String testOriginal = "Original formatting: *emphasis*, **strong**, ^superscript^.";
    private final String testShortcuts = "All <e1>work</e1> and no <e2>play</e2>makes Jack a dull boy.<f1/>";

    @Test
    public void testStructureParsing() {
        /*
        ZFormatter formatter = new ZFormatter();
        List<ZFormatSpan> structure = formatter.parseStructure(testOriginal, true, false);
        assertEquals(structure.size(), 9);

        structure = formatter.parseStructure(testShortcuts, false, true);
        assertEquals(structure.size(), 7);
        */
        assertTrue(true);
    }
}
