package com.onedictprojects.soundrecorder;

/**
 * Created by kiencbui on 21/04/2017.
 */

public class AudioItem {
    private String fileType;
    private String filename;
    private String path;

    public AudioItem(String fileType, String filename, String path) {
        this.fileType = fileType;
        this.filename = filename;
        this.path = path;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return this.filename;
    }
}
