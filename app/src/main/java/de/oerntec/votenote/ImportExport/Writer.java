/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
package de.oerntec.votenote.ImportExport;

import android.os.Environment;

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
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Votenote/");
        //noinspection ResultOfMethodCallIgnored
        directory.mkdirs();
        File logFile = new File(directory, "log.txt");
        try {
            //noinspection ResultOfMethodCallIgnored
            logFile.createNewFile();
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
