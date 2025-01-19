package org.jabref.logic.importer.fileformat;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MedlineArticleParser {

    public BibEntry parse(XMLStreamReader reader) throws Exception {
        Map<Field, String> fields = new HashMap<>();

        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                String elementName = reader.getLocalName();

                switch (elementName) {
                    case "MedlineCitation":
                        parseMedlineCitation(reader, fields);
                        break;
                    case "PubmedData":
                        parsePubmedData(reader, fields);
                        break;
                }
            }

            if (reader.isEndElement() && reader.getLocalName().equals("PubmedArticle")) {
                break;
            }
        }

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(fields);
        return entry;
    }

    private void parseMedlineCitation(XMLStreamReader reader, Map<Field, String> fields) throws Exception {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "PMID" -> {
                        reader.next();
                        putIfValueNotNull(fields, StandardField.PMID, reader.getText());
                    }
                    case "Article" -> parseArticle(reader, fields);
                }
            }

            if (reader.isEndElement() && reader.getLocalName().equals("MedlineCitation")) {
                break;
            }
        }
    }

    private void parsePubmedData(XMLStreamReader reader, Map<Field, String> fields) throws Exception {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement() && "PublicationStatus".equals(reader.getLocalName())) {
                reader.next();
                putIfValueNotNull(fields, StandardField.PUBSTATE, reader.getText());
            }

            if (reader.isEndElement() && "PubmedData".equals(reader.getLocalName())) {
                break;
            }
        }
    }

    private void parseArticle(XMLStreamReader reader, Map<Field, String> fields) throws Exception {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "ArticleTitle" -> {
                        reader.next();
                        putIfValueNotNull(fields, StandardField.TITLE, reader.getText());
                    }
                    case "Abstract" -> {
                        reader.next();
                        putIfValueNotNull(fields, StandardField.ABSTRACT, reader.getText());
                    }
                }
            }

            if (reader.isEndElement() && "Article".equals(reader.getLocalName())) {
                break;
            }
        }
    }

    private void putIfValueNotNull(Map<Field, String> fields, Field field, String value) {
        if (value != null && !value.isBlank()) {
            fields.put(field, value);
        }
    }
}
