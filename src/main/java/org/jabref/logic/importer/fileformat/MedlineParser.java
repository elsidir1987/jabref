package org.jabref.logic.importer.fileformat;

import org.jabref.model.entry.BibEntry;

import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles parsing logic for Medline entries.
 */
public class MedlineParser {

    private final MedlineArticleParser articleParser;
    private final MedlineBookParser bookParser;

    public MedlineParser(MedlineArticleParser articleParser, MedlineBookParser bookParser) {
        this.articleParser = articleParser;
        this.bookParser = bookParser;
    }

    public List<BibEntry> parse(XMLStreamReader reader) {
        List<BibEntry> bibEntries = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                reader.next();

                if (isStartElement(reader, "PubmedArticle")) {
                    bibEntries.add(articleParser.parse(reader));
                } else if (isStartElement(reader, "PubmedBookArticle")) {
                    bibEntries.add(bookParser.parse(reader));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing Medline entries", e);
        }
        return bibEntries;
    }

    private boolean isStartElement(XMLStreamReader reader, String elementName) {
        return reader.isStartElement() && reader.getLocalName().equals(elementName);
    }
}
