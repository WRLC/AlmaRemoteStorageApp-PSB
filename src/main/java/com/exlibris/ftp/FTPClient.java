package com.exlibris.ftp;

public abstract class FTPClient {

    protected abstract void open() throws Exception;

    protected abstract void close() throws Exception;

    public abstract void getFiles(String ftpFolder, String localFolder);

    public abstract boolean uploadSingleFile(String localFilePath, String remoteFilePath) throws Exception;

    public abstract void closeconn() throws Exception;
}
