package com.troikoss.continuum_explorer;

import android.os.ParcelFileDescriptor;
import com.troikoss.continuum_explorer.model.ShizukuFileInfo;

interface IFileService {
    void destroy() = 16777114;

    List<ShizukuFileInfo> getDetailedList(String path) = 1;
    boolean isDirectory(String path) = 2;
    long getLength(String path) = 3;
    long getLastModified(String path) = 4;
    boolean exists(String path) = 5;
    boolean delete(String path) = 6;
    boolean rename(String path, String newName) = 7;
    boolean mkdir(String path) = 8;
    boolean createNewFile(String path) = 9;
    ParcelFileDescriptor openFile(String path, String mode) = 10;
    boolean copyFile(String sourcePath, String destPath) = 11;
    boolean moveFile(String sourcePath, String destPath) = 12;
}
