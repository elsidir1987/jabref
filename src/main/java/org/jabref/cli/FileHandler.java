package org.jabref.cli;

import org.jabref.logic.exporter.*;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileHandler.class);

    private final CliOptions cli;
    private final CliPreferences cliPreferences;
    private final FileUpdateMonitor fileUpdateMonitor;

    public FileHandler(
            CliOptions cli,
            CliPreferences cliPreferences,
            FileUpdateMonitor fileUpdateMonitor
    ) {
        this.cli = cli;
        this.cliPreferences = cliPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
    }

    /**
     * Imports files specified in CLI arguments and returns a list of parser results.
     */
    public List<ParserResult> importFiles(ArgumentProcessor.Mode startupMode) {
        List<ParserResult> loaded = new ArrayList<>();
        List<String> toImport = new ArrayList<>();

        if (!cli.isBlank() && (!cli.getLeftOver().isEmpty())) {
            for (String aLeftOver : cli.getLeftOver()) {
                boolean isBibFile = aLeftOver.toLowerCase().endsWith("bib");

                if (isBibFile) {
                    try {
                        ParserResult result = OpenDatabase.loadDatabase(
                                Path.of(aLeftOver),
                                cliPreferences.getImportFormatPreferences(),
                                fileUpdateMonitor);
                        loaded.add(result);
                    } catch (IOException ex) {
                        LOGGER.error("Error opening file '{}'", aLeftOver, ex);
                        loaded.add(ParserResult.fromError(ex));
                    }
                } else {
                    toImport.add(aLeftOver);
                }
            }
        }

        if (!cli.isBlank() && cli.isFileImport()) {
            toImport.add(cli.getFileImport());
        }

        for (String filename : toImport) {
            ParserResult importResult = importFile(filename);
            if (importResult != null) {
                loaded.add(importResult);
            }
        }

        return loaded;
    }

    /**
     * Exports loaded files to the specified export path.
     */
    public void exportFiles(List<ParserResult> loaded, String fileExportOption) {
        if (loaded.isEmpty()) {
            System.err.println(Localization.lang("The output option depends on a valid import option."));
            return;
        }

        String[] data = fileExportOption.split(",");
        if (data.length == 1) {
            saveDatabase(loaded.get(loaded.size() - 1).getDatabase(), data[0]);
        } else if (data.length == 2) {
            exportDatabase(loaded.get(loaded.size() - 1), data[0], data[1]);
        } else {
            System.err.println(Localization.lang("Invalid export option format."));
        }
    }

    /**
     * Saves a database to a specified file.
     */
    public void saveDatabase(BibDatabase database, String fileName) {
        try {
            System.out.println(Localization.lang("Saving") + ": " + fileName);
            Path outputPath = Path.of(fileName);

            try (AtomicFileWriter writer = new AtomicFileWriter(outputPath, StandardCharsets.UTF_8)) {
                BibWriter bibWriter = new BibWriter(writer, OS.NEWLINE);
                SelfContainedSaveConfiguration saveConfig = (SelfContainedSaveConfiguration) new SelfContainedSaveConfiguration()
                        .withReformatOnSave(cliPreferences.getLibraryPreferences().shouldAlwaysReformatOnSave());
                BibDatabaseWriter databaseWriter = new BibtexDatabaseWriter(
                        bibWriter,
                        saveConfig,
                        cliPreferences.getFieldPreferences(),
                        cliPreferences.getCitationKeyPatternPreferences(),
                        new BibEntryTypesManager());
                databaseWriter.saveDatabase(new BibDatabaseContext(database));

                if (writer.hasEncodingProblems()) {
                    System.err.println(Localization.lang("Warning") + ": " +
                            Localization.lang("UTF-8 could not encode some characters: %0", writer.getEncodingProblems()));
                }
            }
        } catch (IOException ex) {
            System.err.println(Localization.lang("Could not save file.") + "\n" + ex.getLocalizedMessage());
        }
    }

    private ParserResult importFile(String filename) {
        try {
            Path filePath = Path.of(filename);
            return OpenDatabase.loadDatabase(
                    filePath,
                    cliPreferences.getImportFormatPreferences(),
                    new DummyFileUpdateMonitor());
        } catch (Exception e) {
            LOGGER.error("Error importing file: {}", filename, e);
            return null;
        }
    }

    private void exportDatabase(ParserResult parserResult, String outputFileName, String formatName) {
        try {
            Path outputPath = Path.of(outputFileName);
            BibDatabaseContext databaseContext = parserResult.getDatabaseContext();
            databaseContext.setDatabasePath(outputPath);

            ExporterFactory exporterFactory = ExporterFactory.create(cliPreferences);
            Exporter exporter = exporterFactory.getExporterByName(formatName)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown export format: " + formatName));

            exporter.export(databaseContext, outputPath, parserResult.getDatabase().getEntries(), Collections.emptyList(), null);
            System.out.println(Localization.lang("Exported to") + ": " + outputFileName);
        } catch (Exception e) {
            LOGGER.error("Could not export file '{}'. Reason: {}", outputFileName, e.getMessage(), e);
        }
    }
}
