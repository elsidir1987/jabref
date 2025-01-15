package org.jabref.cli;

import org.jabref.logic.UiCommand;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import java.util.List;

public class ArgumentProcessor {

    public enum Mode { INITIAL_START, REMOTE_START }

    private final CliOptions cli;
    private final Mode startupMode;
    private final CliPreferences cliPreferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final ArgumentProcessingHandler handler;

    public ArgumentProcessor(String[] args, Mode startupMode, CliPreferences cliPreferences, FileUpdateMonitor fileUpdateMonitor, BibEntryTypesManager entryTypesManager) throws org.apache.commons.cli.ParseException {
        this.cli = new CliOptions(args);
        this.startupMode = startupMode;
        this.cliPreferences = cliPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;

        this.handler = new ArgumentProcessingHandler(cli, startupMode, cliPreferences, fileUpdateMonitor, entryTypesManager);
    }

    public void processArguments() {
        handler.processArguments();
    }

    public boolean shouldShutDown() {
        return cli.isDisableGui() || cli.isShowVersion() || !handler.isGuiNeeded();
    }

    public List<UiCommand> getUiCommands() {
        return handler.getUiCommands();
    }
}
