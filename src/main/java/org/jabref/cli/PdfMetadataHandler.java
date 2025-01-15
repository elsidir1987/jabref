package org.jabref.cli;

import com.airhacks.afterburner.injection.Injector;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.exporter.XmpPdfExporter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PdfMetadataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfMetadataHandler.class);

    private final CliPreferences cliPreferences;

    public PdfMetadataHandler(CliPreferences cliPreferences) {
        this.cliPreferences = cliPreferences;
    }

    /**
     * Writes metadata to PDF files for a given list of ParserResults.
     */
    public void writeMetadataToPdf(List<ParserResult> loaded,
                                   String filesAndCiteKeys,
                                   boolean writeXmp,
                                   boolean embedBibFile) {
        if (loaded.isEmpty()) {
            LOGGER.error("The write metadata option depends on a valid import option.");
            return;
        }

        ParserResult parserResult = loaded.get(loaded.size() - 1); // Use the last imported result
        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();

        // Fetch preferences from CliPreferences
        XmpPdfExporter xmpPdfExporter = new XmpPdfExporter(cliPreferences.getXmpPreferences());
        EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter = new EmbeddedBibFilePdfExporter(
                cliPreferences.getLibraryPreferences().getDefaultBibDatabaseMode(),
                cliPreferences.getCustomEntryTypesRepository(),
                cliPreferences.getFieldPreferences());

        if ("all".equals(filesAndCiteKeys)) {
            for (BibEntry entry : databaseContext.getEntries()) {
                writeMetadataToEntry(databaseContext, entry, cliPreferences.getFilePreferences(),
                        xmpPdfExporter, embeddedBibFilePdfExporter,
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class), writeXmp, embedBibFile);
            }
        } else {
            List<String> citeKeys = new ArrayList<>();
            List<String> pdfFiles = new ArrayList<>();

            for (String item : filesAndCiteKeys.split(",")) {
                if (item.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    pdfFiles.add(item);
                } else {
                    citeKeys.add(item);
                }
            }

            writeMetadataByCiteKeys(databaseContext, citeKeys, cliPreferences.getFilePreferences(),
                    xmpPdfExporter, embeddedBibFilePdfExporter,
                    Injector.instantiateModelOrService(JournalAbbreviationRepository.class), writeXmp, embedBibFile);
            writeMetadataByPdfFiles(databaseContext, pdfFiles, cliPreferences.getFilePreferences(),
                    xmpPdfExporter, embeddedBibFilePdfExporter,
                    Injector.instantiateModelOrService(JournalAbbreviationRepository.class), writeXmp, embedBibFile);
        }
    }

    private void writeMetadataToEntry(BibDatabaseContext databaseContext,
                                      BibEntry entry,
                                      FilePreferences filePreferences,
                                      XmpPdfExporter xmpPdfExporter,
                                      EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter,
                                      JournalAbbreviationRepository abbreviationRepository,
                                      boolean writeXmp,
                                      boolean embedBibFile) {
        try {
            String citeKey = entry.getCitationKey().orElse("<no cite key defined>");
            if (writeXmp) {
                if (xmpPdfExporter.exportToAllFilesOfEntry(databaseContext, filePreferences, entry, List.of(entry), abbreviationRepository)) {
                    System.out.printf("Successfully written XMP metadata for entry %s%n", citeKey);
                } else {
                    System.err.printf("Failed to write XMP metadata for entry %s%n", citeKey);
                }
            }
            if (embedBibFile) {
                if (embeddedBibFilePdfExporter.exportToAllFilesOfEntry(databaseContext, filePreferences, entry, List.of(entry), abbreviationRepository)) {
                    System.out.printf("Successfully embedded metadata for entry %s%n", citeKey);
                } else {
                    System.err.printf("Failed to embed metadata for entry %s%n", citeKey);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error writing metadata for entry '{}'", entry, e);
        }
    }

    private void writeMetadataByCiteKeys(BibDatabaseContext databaseContext,
                                         List<String> citeKeys,
                                         FilePreferences filePreferences,
                                         XmpPdfExporter xmpPdfExporter,
                                         EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter,
                                         JournalAbbreviationRepository abbreviationRepository,
                                         boolean writeXmp,
                                         boolean embedBibFile) {
        for (String citeKey : citeKeys) {
            List<BibEntry> entries = databaseContext.getDatabase().getEntriesByCitationKey(citeKey);
            if (entries.isEmpty()) {
                System.err.printf("No entries found for cite key %s%n", citeKey);
                continue;
            }
            for (BibEntry entry : entries) {
                writeMetadataToEntry(databaseContext, entry, filePreferences, xmpPdfExporter, embeddedBibFilePdfExporter, abbreviationRepository, writeXmp, embedBibFile);
            }
        }
    }

    private void writeMetadataByPdfFiles(BibDatabaseContext databaseContext,
                                         List<String> pdfFiles,
                                         FilePreferences filePreferences,
                                         XmpPdfExporter xmpPdfExporter,
                                         EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter,
                                         JournalAbbreviationRepository abbreviationRepository,
                                         boolean writeXmp,
                                         boolean embedBibFile) {
        for (String pdfFile : pdfFiles) {
            Path pdfPath = Path.of(pdfFile);
            if (!Files.exists(pdfPath)) {
                LOGGER.error("PDF file '{}' does not exist", pdfFile);
                continue;
            }
            try {
                if (writeXmp) {
                    if (xmpPdfExporter.exportToFileByPath(databaseContext, filePreferences, pdfPath, abbreviationRepository)) {
                        System.out.printf("Successfully written XMP metadata to %s%n", pdfFile);
                    } else {
                        System.err.printf("Failed to write XMP metadata to %s%n", pdfFile);
                    }
                }
                if (embedBibFile) {
                    if (embeddedBibFilePdfExporter.exportToFileByPath(databaseContext, filePreferences, pdfPath, abbreviationRepository)) {
                        System.out.printf("Successfully embedded metadata to %s%n", pdfFile);
                    } else {
                        System.err.printf("Failed to embed metadata to %s%n", pdfFile);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error writing metadata to PDF file '{}'", pdfFile, e);
            }
        }
    }
}
