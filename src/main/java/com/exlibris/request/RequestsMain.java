package com.exlibris.request;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.exlibris.configuration.ConfigurationHandler;
import com.exlibris.ftp.FTPClient;
import com.exlibris.ftp.FTPUtil;
import com.exlibris.ftp.SFTPUtil;
import com.exlibris.items.ItemData;
import com.exlibris.logger.ReportUtil;

public class RequestsMain {

    final static String MAINLOCALFOLDER = "files/requests/";

    final static Logger logger = Logger.getLogger(RequestsMain.class);

    public static synchronized void sendRequestsToSCF(String institution) {
        try {
            FTPClient ftpUtil;
            logger.info("Starting Send Requests To SCF For Institution: " + institution);

            JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
            String ftpFolder = props.getJSONObject("ftp_server").getString("main_folder");
            String ftpPort = props.getJSONObject("ftp_server").getString("ftp_port");
            String mainLocalFolder = MAINLOCALFOLDER;
            if (props.has("main_local_folder") && props.get("main_local_folder") != null) {
                mainLocalFolder = props.getString("main_local_folder") + "/" + MAINLOCALFOLDER;
            }
            // empty the local folder
            File xmlFolder = new File(mainLocalFolder + "xml/");
            if (xmlFolder.isDirectory()) {
                FileUtils.cleanDirectory(xmlFolder);
            } else {
                xmlFolder.mkdirs();
            }
            if (ftpPort.equals("22")) {
                ftpUtil = new SFTPUtil();
            } else {
                ftpUtil = new FTPUtil();
            }
            // get files from ftp
            ftpUtil.getFiles(ftpFolder + "/" + institution + "/requests/", mainLocalFolder + "xml/");

            // loop over xml files and convert them to records
            File[] xmlFiles = xmlFolder.listFiles();
            int totalRecords = 0;
            for (File xmlFile : xmlFiles) {
                String content = new String(Files.readAllBytes(xmlFile.toPath()));
                List<ItemData> requestList = new ArrayList<ItemData>();
                try {
                	requestList = ItemData.xmlStringToRequestData(content, institution);
                }catch (Exception e) {
                	logger.info("Failed to handle file " + xmlFile.getName() + " " + e.getMessage());
				}
                for (ItemData request : requestList) {
                    boolean success = false;
                    try {
                        if (request.getBarcode().contains(" ") || request.getBarcode().contains(":")) {
                            if (request.getUserId() != null
                                    && request.getRequestType().equals("PHYSICAL_TO_DIGITIZATION")) {
                                success = RequestHandler.createDigitizationUserRequest(request);
                            } else {
                                success = RequestHandler.createBibRequest(request);
                            }
                        } else if (request.getBarcode() != null && !request.getBarcode().isEmpty()) {
                            if (request.getRequestType().equals("PHYSICAL_TO_DIGITIZATION")) {
                                success = RequestHandler.createDigitizationItemRequest(request);
                            } else {
                                success = RequestHandler.createItemRequest(request);
                            }
                        } else {
                            success = RequestHandler.createBibRequest(request);
                        }
                    } catch (Exception e) {
                        ReportUtil.getInstance().appendReport("RequestHandler", "", institution,
                                "Failed to handle request ");
                        logger.info("Failed to handle request " + e.getMessage());
                    }
                    if (!success) {
                        RequestHandler.cancelRequest(request);
                    }

                }

                totalRecords++;
            }
            logger.info("Total Records from FTP: " + totalRecords);

        } catch (Exception e) {
            ReportUtil.getInstance().appendReport("RequestHandler", "", institution, "Failed to handle requests");
            logger.error(e.getMessage());
        }
    }
}
