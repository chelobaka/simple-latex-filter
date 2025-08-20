package org.omegat.filters;

import com.pilulerouge.omegat.latex.CommandCenter;
import com.pilulerouge.omegat.latex.Parser;
import com.pilulerouge.omegat.latex.Token;
import org.junit.jupiter.api.Test;

import static com.pilulerouge.omegat.latex.Tokenizer.tokenizeDocument;
import static com.pilulerouge.omegat.latex.Util.readBufferWithLinebreaks;
import static j2html.TagCreator.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class InternalTest {

    private final String TEST_DOCUMENT_PATH = "/test.tex";
    private final String REPORT_DIR = "build/reports/internal";
    private final String REPORT_PATH = REPORT_DIR + "/report.html";

    // Helper NVL function
    private <T> T nvl(T a, T b) {
        return (a == null) ? b : a;
    }

    @org.junit.jupiter.api.Tag("internal")
    @Test
    public void TokenizerAndParserReport() throws IOException {
        File documentFile = new File(this.getClass().getResource(TEST_DOCUMENT_PATH).getFile());
        BufferedReader reader = new BufferedReader(new FileReader(documentFile));
        String documentString = readBufferWithLinebreaks(reader);

        ListIterator<Token> tokenIterator = tokenizeDocument(documentString);

        CommandCenter commandCenter = new CommandCenter(true);
        Parser parser = new Parser(commandCenter);

        Token token;
        List<Token> tokenList = new ArrayList<>();

        while(tokenIterator.hasNext()) {
            token = tokenIterator.next();
            parser.processToken(token);
            tokenList.add(token);
        }

        // Build report HTML
        String report = html(
            head(
                title("SimpleLatexFilter Internal report"),
                link().withRel("stylesheet").withHref("/css/main.css"),
                style("td {border-top: solid 0.5px; border-left: solid 0.5px;} td {padding: 3px}}")
            ),
            body(
                h3("Tokenzier and parser report"),
                table(
                    tbody(
                        tr(
                            th("Name"),
                            th("Type"),
                            th("Text"),
                            th("Translate"),
                            th("Externality"),
                            th("TagID"),
                            th("Escape")
                        ),
                        each(
                            tokenList,
                            t -> tr(
                                td(nvl(t.getName(), "")),
                                td(t.getType().name()),
                                td(documentString.substring(t.getStart(), t.getEnd())),
                                td(String.valueOf(t.getParserMark().isTranslatable())),
                                td(String.valueOf(t.getParserMark().getExternality())),
                                td(String.valueOf(t.getParserMark().getTagId())),
                                td(String.valueOf(t.getParserMark().doEscape()))
                            )
                        )
                    )
                )
            )
        ).render();

        // Create report directory
        new File(REPORT_DIR).mkdirs();
        // Save report
        BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_PATH));
        writer.write(report);
        writer.close();
        System.out.println("Internal report: file://" + Paths.get(REPORT_PATH).toAbsolutePath());
    }
}
