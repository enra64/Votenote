package de.oerntec.votenote.ImportExport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Writer {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void writeToFile(final String string, final String exportFilePath) throws IOException {
        File file = new File(exportFilePath);
        file.getParentFile().mkdirs();
        file.createNewFile();

        ByteBuffer buff = ByteBuffer.wrap(string.getBytes());
        FileChannel channel = new FileOutputStream(file).getChannel();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            channel.write(buff);
        } finally {
            channel.close();
        }
    }

    /**
     * Append a string to the log file
     *
     * @param text string to append
     */
    public static void appendLog(String text) {
        File logFile = new File("sdcard/Votenote/log.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.flush();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
