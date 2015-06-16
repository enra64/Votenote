package de.oerntec.votenote.ImportExport;

import java.io.File;
import java.io.FileOutputStream;
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
}
