package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

public class BackupManager {

    private static final Set<BackupManager> runningInstances = new CopyOnWriteArraySet<>();
    private static final BackupFileHandler FILE_HANDLER = new BackupFileHandler();

    private final BackupExecutor backupExecutor;
    private final BackupFileHandler fileHandler;
    private final BibDatabaseContext bibDatabaseContext;

    public BackupManager(
            BibDatabaseContext bibDatabaseContext
    ) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.fileHandler = new BackupFileHandler();
        this.backupExecutor = new BackupExecutor();
    }

    // Start method matching the original design
    public static void start(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) {
        BackupManager backupManager = new BackupManager(bibDatabaseContext);
        backupManager.startBackup(preferences.getFilePreferences().getBackupDirectory());
        runningInstances.add(backupManager);
    }

    private void startBackup(Path backupDir) {
        backupExecutor.scheduleBackupTask(() -> {
            Optional<Path> backupPath = fileHandler.determineBackupPathForNewBackup(backupDir, bibDatabaseContext);
            backupPath.ifPresent(this::performBackup);
        });
    }

    private void performBackup(Path backupPath) {
        try {
            fileHandler.addBackupToQueue(backupPath);
        } catch (IOException e) {
        }
    }

    public static void shutdown(BibDatabaseContext bibDatabaseContext, Path backupDir, boolean createBackup) {
        runningInstances.stream()
                .filter(instance -> instance.bibDatabaseContext == bibDatabaseContext)
                .forEach(backupManager -> backupManager.shutdownInstance(backupDir, createBackup));
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == bibDatabaseContext);
    }

    private void shutdownInstance(Path backupDir, boolean createBackup) {
        if (createBackup) {
            Optional<Path> backupPath = fileHandler.determineBackupPathForNewBackup(backupDir, bibDatabaseContext);
            backupPath.ifPresent(this::performBackup);
        }
        backupExecutor.shutdown();
    }

    public static void restoreBackup(Path originalPath, Path backupDir) {
        Optional<Path> latestBackupPath = FILE_HANDLER.getLatestBackupPath(originalPath, backupDir);

        if (latestBackupPath.isEmpty()) {
            return;
        }

        try {
            FILE_HANDLER.restoreBackup(originalPath, latestBackupPath.get());
        } catch (IOException e) {
        }
    }

    public static boolean backupFileDiffers(Path originalPath, Path backupDir) {
        Optional<Path> latestBackupPath = FILE_HANDLER.getLatestBackupPath(originalPath, backupDir);

        return latestBackupPath.map(latestPath -> FILE_HANDLER.isBackupDifferent(originalPath, latestPath))
                .orElse(false);
    }

    public static void discardBackup(BibDatabaseContext bibDatabaseContext, Path backupDir) {
        bibDatabaseContext.getDatabasePath().ifPresent(originalPath -> {
            Path discardedFile = FILE_HANDLER.determineDiscardedFile(originalPath, backupDir);
            try {
                Files.createFile(discardedFile);
            } catch (IOException e) {
            }
        });
    }
}
