package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Provides a timer that periodically generates file system sync events.
 *
 * @author TransfictionRailways
 */
public class SyncTimer extends TimerTask {
    private final FileSystemObserver server;
    private static Timer timerThread = new Timer(true);
    private final FileSystemManager fileSystemManager;

    public SyncTimer(FileSystemObserver server, FileSystemManager fileSystemManager) {
        this.server = server;
        this.fileSystemManager = fileSystemManager;
    }

    /**
     * Start the sync events timer. Every so often (interval controlled by the syncInterval config option),
     * asks the file system manager for sync events and feeds to the main server object.
     *
     * @param server            The servermain instance controlling IO threads
     * @param fileSystemManager An instance of the file system manager
     */
    public static void startEvents(FileSystemObserver server, FileSystemManager fileSystemManager) {
        TimerTask task = new SyncTimer(server, fileSystemManager);
        timerThread.schedule(task, 0,
                1000 * Long.parseLong(Configuration.getConfigurationValue(Commands.SYNC_INTERVAL)));
    }

    /**
     * The task to be periodically executed. Query file system manager then send each event to serverMain.
     */
    @Override
    public void run() {
        fileSystemManager.generateSyncEvents().forEach(server::processFileSystemEvent);
    }
}
