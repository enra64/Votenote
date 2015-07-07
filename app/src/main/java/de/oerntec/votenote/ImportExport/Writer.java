package de.oerntec.votenote.ImportExport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;

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
     * @param text string to append
     */
    public static void appendLog(String text) {
        text = getDateAndTimeNow() + '\n' + text;
        File logFile = new File("sdcard/Votenote/log.txt");
        if (!logFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
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

    /**
     * Returns a loggable representation of *now*
     *
     * @return now as a string (22.4.2015_22:2)
     */
    private static String getDateAndTimeNow() {
        Calendar now = Calendar.getInstance();
        //noinspection StringBufferReplaceableByString
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(now.get(Calendar.DAY_OF_MONTH));
        stringBuilder.append(".");
        stringBuilder.append(now.get(Calendar.MONTH) + 1);
        stringBuilder.append(".");
        stringBuilder.append(now.get(Calendar.YEAR));
        stringBuilder.append("_");
        stringBuilder.append(now.get(Calendar.HOUR_OF_DAY));
        stringBuilder.append(":");
        stringBuilder.append(now.get(Calendar.MINUTE));
        return stringBuilder.toString();
    }
}
