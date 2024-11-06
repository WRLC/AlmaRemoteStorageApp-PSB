
package com.exlibris.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.exlibris.configuration.ConfigurationHandler;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class SFTPUtil extends FTPClient {

    final static Logger logger = Logger.getLogger(SFTPUtil.class);
    private ChannelSftp sftpChannel;

    public ChannelSftp connect(String server, String username, final String password, int port, int timeOut)
            throws JSchException {

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, server, port);
            session.setTimeout(timeOut);
            session.setPassword(password);
            session.setUserInfo(new MyUserInfo(password));
            logger.info("[SFTP] connecting to server:" + server);
            // connection timeout
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect(timeOut);
            // be sure that the so timeout is set on the socket
            session.setTimeout(timeOut);

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(timeOut);
        } catch (JSchException e) {
            throw e;
        }
        return sftpChannel;
    }

    public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {

        String passwd;

        public MyUserInfo(String passwd) {
            super();
            this.passwd = passwd;
        }

        public String getPassword() {
            return passwd;
        }

        public boolean promptYesNo(String str) {
            return true;
        }

        public String getPassphrase() {
            return null;
        }

        public boolean promptPassphrase(String message) {
            return false;
        }

        public boolean promptPassword(String message) {
            return false;
        }

        public void showMessage(String message) {
            // System.out.println(message);
        }

        public String[] promptKeyboardInteractive(String arg0, String arg1, String arg2, String[] arg3,
                boolean[] arg4) {
            return new String[] { passwd };
        }
    }

    @Override
    protected void open() throws Exception {
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        JSONObject ftpProps = props.getJSONObject("ftp_server");
        String server = ftpProps.getString("host");
        String user = ftpProps.getString("user");
        String pass = ftpProps.getString("password");
        String ftpPort = ftpProps.getString("ftp_port");

        if (sftpChannel == null || !sftpChannel.isConnected()) {
            try {
                int port = Integer.valueOf(ftpPort);

                sftpChannel = connect(server, user, pass, port, 30000);

            } catch (Exception e) {
                logger.warn("Could not connect to the server: " + server + " Port = 22 " + e);
                System.out.println("Could not connect to the server: " + server + " Port = 22 " + e);
                return;
            }
        }
        try {
            sftpChannel.pwd();

        } catch (Exception e) {
            logger.warn("Could not login to the server.");
            System.out.println("Could not login to the server.");
            close();
            return;
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void getFiles(String ftpFolder, String localFolder) {
        try {
            logger.info("get files from ftp folder:" + ftpFolder + " local folder : " + localFolder);
            open();
            String mainDir = sftpChannel.pwd();
            ArrayList<String> fileList = new ArrayList<String>();
            for (LsEntry e : (Vector<LsEntry>) sftpChannel.ls(ftpFolder)) {
                if (isFile(e)) {
                    System.out.println(e.getFilename());
                    fileList.add(e.getFilename());
                }
            }
            if (fileList.size() > 0) {
                mkdir(ftpFolder + "/OLD");
            }

            // iterates over the files and moves to OLD folder
            sftpChannel.cd(ftpFolder);

            for (String file : fileList) {
                logger.info("Starting Hendle file " + file);
                File localfile = new File(localFolder + "/" + file);
                OutputStream outputStream = new FileOutputStream(localfile);
                // find out if a file is being used by another process? // only
                // unused file can
                // be renamed
                if (rename(file, "OLD/" + file)) {
                    try {
                        sftpChannel.get("OLD/" + file, outputStream);
                        logger.info("Success retrieving file :" + file);
                    } catch (Exception copyEx) {
                        logger.info("can't retrieve File move back to folder" + copyEx.getMessage());
                        rename("OLD/" + file, file);
                    }
                }
                outputStream.close();
            }
            sftpChannel.cd(mainDir);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                close();
            } catch (Exception e) {
            }
        }

    }

    private boolean rename(String fileName, String toFileName) {
        try {
            sftpChannel.rename(fileName, toFileName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean uploadSingleFile(String localFilePath, String remoteFilePath) throws Exception {
        File localFile = new File(localFilePath);
        FileInputStream fis = null;
        open();
        try {
            fis = new FileInputStream(localFile);
            sftpChannel.put(fis, remoteFilePath, ChannelSftp.OVERWRITE);

        } finally {
            fis.close();
            close();
        }
        return true;
    }

    @Override
    public void closeconn() throws Exception {
        close();

    }

    @Override
    public void close() throws Exception {
        try {
            if (sftpChannel != null && sftpChannel.getSession() != null) {
                sftpChannel.getSession().disconnect();
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        try {
            if (sftpChannel != null) {
                sftpChannel.disconnect();
            }
        } catch (Exception e) {
            logger.error("", e);
        }

    }

    private boolean isFile(LsEntry file) {
        if (!file.getFilename().equals(".") && !file.getFilename().equals("..") && !file.getAttrs().isDir()) {
            return true;
        }
        return false;
    }

    private void mkdir(String folder) {
        SftpATTRS attrs = null;
        try {
            attrs = sftpChannel.stat(folder);
        } catch (Exception e) {
            logger.debug(folder + " not found");
        }

        if (attrs != null) {
            logger.debug("Directory exists IsDir=" + attrs.isDir());
        } else {
            logger.debug("Creating dir " + folder);
            try {
                sftpChannel.mkdir(folder);
            } catch (Exception e) {
                logger.warn("Cant create Dir: " + folder);
            }
        }
    }

}
