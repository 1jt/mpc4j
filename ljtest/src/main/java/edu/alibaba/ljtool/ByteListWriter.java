package edu.alibaba.ljtool;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ByteListWriter {
    public static void writeByteArraysToFile(List<byte[]> dataList, int n, int entryBitLength) throws IOException {
        String filePath = "respondSize" + "_" + n + "_" + entryBitLength + ".output";
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(filePath))) {
            for (byte[] bytes : dataList) {
                outputStream.write(bytes);
            }
        }
    }
}
