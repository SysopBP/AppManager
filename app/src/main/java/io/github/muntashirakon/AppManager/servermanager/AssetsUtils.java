// SPDX-License-Identifier: MIT AND GPL-3.0-or-later

package io.github.muntashirakon.AppManager.servermanager;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.github.muntashirakon.io.IoUtils;

// Copyright 2016 Zheng Li
@SuppressWarnings("ResultOfMethodCallIgnored")
class AssetsUtils {
    @WorkerThread
    public static void copyFile(@NonNull Context context, String fileName, File destFile, boolean force)
            throws IOException {
        File parent = destFile.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory())) {
            throw new IOException("Cannot create local server asset directory: " + destFile);
        }
        // Some build variants package jar assets compressed. openFd() only works
        // with uncompressed assets; AssetManager.open() supports either form.
        try (java.io.InputStream source = context.getAssets().open(fileName)) {
            File temp = new File(parent, destFile.getName() + ".tmp");
            try {
                try (FileOutputStream fos = new FileOutputStream(temp)) {
                    byte[] buff = new byte[IoUtils.DEFAULT_BUFFER_SIZE];
                    int len;
                    while ((len = source.read(buff)) != -1) {
                        fos.write(buff, 0, len);
                    }
                    fos.flush();
                    fos.getFD().sync();
                }
                if (temp.length() == 0) {
                    throw new IOException("Empty local server asset: " + fileName);
                }
                if (destFile.exists() && !destFile.delete()) {
                    throw new IOException("Cannot replace local server asset: " + destFile);
                }
                if (!temp.renameTo(destFile)) {
                    throw new IOException("Cannot finalize local server asset: " + destFile);
                }
            } finally {
                if (temp.exists()) temp.delete();
            }
        }
    }
}
