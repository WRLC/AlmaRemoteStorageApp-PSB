package com.exlibris.logger;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.exlibris.configuration.ConfigurationHandler;
import com.exlibris.ftp.FTPClient;
import com.exlibris.ftp.FTPUtil;
import com.exlibris.ftp.SFTPUtil;

public class LoggerMain {

    final static Logger logger = Logger.getLogger(LoggerMain.class);

    @DisallowConcurrentExecution
    public static class LoggerJob implements Job {

        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("LoggerJob executed");
            FTPClient ftpUtil = null;
            String ftpFolder = "";
            try {
                JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
                ftpFolder = props.getJSONObject("ftp_server").getString("main_folder");
                String ftpPort = props.getJSONObject("ftp_server").getString("ftp_port");
                if (ftpPort.equals("22")) {
                    ftpUtil = new SFTPUtil();
                } else {
                    ftpUtil = new FTPUtil();
                }
                String backupFile = "logs//application.log_" + new SimpleDateFormat("yyyy-MM-dd")
                        .format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)) + ".log";
                logger.info("backup File is - " + backupFile);
                boolean ok = ftpUtil.uploadSingleFile(backupFile, ftpFolder + "/" + backupFile);
                logger.info("LoggerJob ended - " + ok);
            } catch (Exception e) {
                logger.info(e.getMessage());
                String file = "logs//application.log";
                boolean ok = false;
                try {
                    logger.info("backup File is - " + file);
                    ok = ftpUtil.uploadSingleFile(file, ftpFolder + "/" + file);
                } catch (Exception e1) {
                    logger.info(e1.getMessage());
                    logger.info("LoggerJob ended - " + "no backup log file copied log file- " + ok);
                }

            }
            logger.info("LoggerJob ended");
        }

    }
}
