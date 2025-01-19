package org.jabref.logic.importer.fileformat;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MedlineBookParser {

    public BibEntry parse(XMLStreamReader reader) throws Exception {
        Map<Field, String> fields = new HashMap<>();

        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                String elementName = reader.getLocalName();
                switch (elementName) {
                    case "BookTitle" -> {
                        reader.next();
                        putIfValueNotNull(fields, StandardField.TITLE, reader.getText());
                    }
                    case "PublisherName" -> {
                        reader.next();
                        putIfValueNotNull(fields, StandardField.PUBLISHER, reader.getText());
                    }
                    case "Abstract" -> {
                        reader.next();
                        putIfValueNotNull(fields, StandardField.ABSTRACT, reader.getText());
                    }
                }
            }

            if (reader.isEndElement() && "PubmedBookArticle".equals(reader.getLocalName())) {
                break;
            }
        }

        BibEntry entry = new BibEntry(StandardEntryType.Book);
        entry.setField(fields);
        return entry;
    }

    private void putIfValueNotNull(Map<Field, String> fields, Field field, String value) {
        if (value != null && !value.isBlank()) {
            fields.put(field, value);
        }
    }
}
