package org.jabref.cli;

import org.jabref.logic.UiCommand;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import java.util.ArrayList;
import java.util.List;

public class ArgumentProcessingHandler {

    private final CliOptions cli;
    private final ArgumentProcessor.Mode startupMode;
    private final CliPreferences cliPreferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private boolean guiNeeded;
    private final List<UiCommand> uiCommands;

    public ArgumentProcessingHandler(CliOptions cli, ArgumentProcessor.Mode startupMode, CliPreferences cliPreferences, FileUpdateMonitor fileUpdateMonitor, BibEntryTypesManager entryTypesManager) {
        this.cli = cli;
        this.startupMode = startupMode;
        this.cliPreferences = cliPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.uiCommands = new ArrayList<>();
    }

    public void processArguments() {
        uiCommands.clear();

        if ((startupMode == ArgumentProcessor.Mode.INITIAL_START) && cli.isShowVersion()) {
            cli.displayVersion();
        }
        if ((startupMode == ArgumentProcessor.Mode.INITIAL_START) && cli.isHelp()) {
            CliOptions.printUsage(cliPreferences);
            guiNeeded = false;
            return;
        }

        guiNeeded = true;
        handlePreferences();
        handleFileOperations();
        handleMetadataOperations();

        if (cli.isBlank()) {
            uiCommands.add(new UiCommand.BlankWorkspace());
        }
    }

    private void handlePreferences() {
        if (cli.isPreferencesReset()) {
            new PreferencesHandler(cliPreferences).resetPreferences(cli.getPreferencesReset());
        }
        if (cli.isPreferencesImport()) {
            new PreferencesHandler(cliPreferences).importPreferences(cli.getPreferencesImport());
        }
    }

    private void handleFileOperations() {
        List<ParserResult> loaded = new FileHandler(cli, cliPreferences, fileUpdateMonitor).importFiles(startupMode);

        if (cli.isFileExport() && !loaded.isEmpty()) {
            new FileHandler(cli, cliPreferences, fileUpdateMonitor).exportFiles(loaded, cli.getFileExport());
        }

        if (!cli.isBlank() && cli.isAuxImport()) {
            performAuxImport(loaded, cli.getAuxImport());
        }
    }

    private void handleMetadataOperations() {
        if (cli.isWriteMetadataToPdf() || cli.isWriteXmpToPdf() || cli.isEmbedBibFileInPdf()) {
            List<ParserResult> loaded = new FileHandler(cli, cliPreferences, fileUpdateMonitor).importFiles(startupMode);
            if (!loaded.isEmpty()) {
                new PdfMetadataHandler(cliPreferences).writeMetadataToPdf(loaded, cli.getWriteMetadataToPdf(), cli.isWriteXmpToPdf(), cli.isEmbedBibFileInPdf());
            }
        }
    }

    private void performAuxImport(List<ParserResult> loaded, String auxImportArgument) {
        if (loaded.isEmpty()) {
            System.out.println(Localization.lang("No base-BibTeX file specified!"));
            System.out.println(Localization.lang("Usage") + " :");
            System.out.println("jabref --aux infile[.aux],outfile[.bib] base-BibTeX-file");
            return;
        }

        String[] auxArguments = auxImportArgument.split(",");
        if (auxArguments.length != 2) {
            System.out.println(Localization.lang("Invalid AUX import arguments! Expected format: infile[.aux],outfile[.bib]"));
            return;
        }

        ParserResult baseDatabaseResult = loaded.get(0); // Assuming the first loaded file is the base database.
        AuxCommandLine auxCommandLine = new AuxCommandLine(auxArguments[0], baseDatabaseResult.getDatabase());
        BibDatabase newDatabase = auxCommandLine.perform();

        if ((newDatabase != null) && newDatabase.hasEntries()) {
            String outputFileName = StringUtil.getCorrectFileName(auxArguments[1], "bib");
            new FileHandler(cli, cliPreferences, fileUpdateMonitor).saveDatabase(newDatabase, outputFileName);
        } else {
            System.out.println(Localization.lang("No library generated"));
        }
    }

    public boolean isGuiNeeded() {
        return guiNeeded;
    }

    public List<UiCommand> getUiCommands() {
        return uiCommands;
    }
}
