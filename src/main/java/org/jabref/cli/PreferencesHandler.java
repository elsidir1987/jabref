package org.jabref.cli;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.l10n.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.prefs.BackingStoreException;

public class PreferencesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesHandler.class);

    private final CliPreferences cliPreferences;

    public PreferencesHandler(CliPreferences cliPreferences) {
        this.cliPreferences = cliPreferences;
    }

    /**
     * Resets preferences to their default values.
     *
     * @param resetOption Specifies which preferences to reset. Can be "all" or a comma-separated list of keys.
     */
    public void resetPreferences(String resetOption) {
        if ("all".equals(resetOption.trim())) {
            try {
                System.out.println(Localization.lang("Setting all preferences to default values."));
                cliPreferences.clear();
                new SharedDatabasePreferences().clear();
            } catch (BackingStoreException e) {
                System.err.println(Localization.lang("Unable to clear preferences."));
                LOGGER.error("Unable to clear preferences", e);
            }
        } else {
            String[] keys = resetOption.split(",");
            for (String key : keys) {
                try {
                    cliPreferences.deleteKey(key.trim());
                    System.out.println(Localization.lang("Resetting preference key '%0'", key.trim()));
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    /**
     * Imports preferences from a file.
     *
     * @param importFilePath The path to the file containing the preferences to import.
     */
    public void importPreferences(String importFilePath) {
        try {
            cliPreferences.importPreferences(Path.of(importFilePath));
            System.out.println(Localization.lang("Successfully imported preferences from %0", importFilePath));
        } catch (Exception ex) {
            System.err.println(Localization.lang("Error importing preferences from file: %0", importFilePath));
            LOGGER.error("Error importing preferences", ex);
        }
    }
}
