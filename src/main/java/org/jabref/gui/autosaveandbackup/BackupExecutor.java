package org.jabref.gui.autosaveandbackup;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BackupExecutor {
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);

    public void scheduleBackupTask(Runnable backupTask) {
        executor.scheduleAtFixedRate(backupTask, 19, 19, TimeUnit.SECONDS);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
