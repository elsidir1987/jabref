package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupFileHandler.class);
    private static final int MAXIMUM_BACKUP_FILE_COUNT = 10;
    private final Queue<Path> backupFilesQueue = new LinkedBlockingQueue<>();

    public Optional<Path> determineBackupPathForNewBackup(Path backupDir, BibDatabaseContext bibDatabaseContext) {
        return bibDatabaseContext.getDatabasePath()
                .map(path -> BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(path, BackupFileType.BACKUP, backupDir));
    }

    public Optional<Path> getLatestBackupPath(Path originalPath, Path backupDir) {
        return BackupFileUtil.getPathOfLatestExistingBackupFile(originalPath, BackupFileType.BACKUP, backupDir);
    }

    public void addBackupToQueue(Path backupPath) throws IOException {
        while (backupFilesQueue.size() >= MAXIMUM_BACKUP_FILE_COUNT) {
            Path oldestBackupFile = backupFilesQueue.poll();
            Files.delete(oldestBackupFile);
        }
        backupFilesQueue.add(backupPath);
    }

    public void restoreBackup(Path originalPath, Path backupPath) throws IOException {
        Files.copy(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean isBackupDifferent(Path originalPath, Path latestBackupPath) {
        try {
            FileTime originalLastModified = Files.getLastModifiedTime(originalPath);
            FileTime backupLastModified = Files.getLastModifiedTime(latestBackupPath);

            if (backupLastModified.compareTo(originalLastModified) <= 0) {
                return false;
            }

            return Files.mismatch(originalPath, latestBackupPath) != -1L;
        } catch (IOException e) {
            LOGGER.error("Error comparing files: {} and {}", originalPath, latestBackupPath, e);
            return true;
        }
    }

    public Path determineDiscardedFile(Path file, Path backupDir) {
        return backupDir.resolve(BackupFileUtil.getUniqueFilePrefix(file) + "--" + file.getFileName() + "--discarded");
    }
}
