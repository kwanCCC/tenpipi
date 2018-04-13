package org.dora;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileOperate {


    public static byte[] getBytesByFile(File file) {
        FileInputStream fin = null;
        byte[] bs = new byte[new Long(file.length()).intValue()];
        try {
            fin = new FileInputStream(file);
            int readBytesLength = 0;
            int i;
            while ((i = fin.available()) > 0) {
                fin.read(bs, readBytesLength, i);
                readBytesLength += i;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bs;
    }
}