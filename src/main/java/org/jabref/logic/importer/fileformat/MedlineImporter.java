package org.jabref.logic.importer.fileformat;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Importer for the Medline/Pubmed format.
 * <p>
 * check here for details on the format https://www.nlm.nih.gov/bsd/licensee/elements_descriptions.html
 */
public class MedlineImporter extends Importer implements Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MedlineImporter.class);
    private final XMLInputFactory xmlInputFactory;
    private final MedlineParser medlineParser;

    public MedlineImporter() {
        this.xmlInputFactory = XMLInputFactory.newInstance();
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
        this.medlineParser = new MedlineParser(new MedlineArticleParser(), new MedlineBookParser());
    }

    @Override
    public String getName() {
        return "Medline/PubMed";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.MEDLINE;
    }

    @Override
    public String getId() {
        return "medline";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for the Medline format.");
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        return MedlineFormatChecker.isMedlineFormat(reader);
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        Objects.requireNonNull(input);

        try (input) {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(input);
            List<BibEntry> bibEntries = medlineParser.parse(reader);
            return new ParserResult(bibEntries);
        } catch (Exception e) {
            LOGGER.error("Error during import", e);
            return ParserResult.fromError(e);
        }
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return importDatabase(reader).getDatabase().getEntries();
        } catch (IOException e) {
            LOGGER.error("Error parsing entries", e);
        }
        return List.of();
    }
}
