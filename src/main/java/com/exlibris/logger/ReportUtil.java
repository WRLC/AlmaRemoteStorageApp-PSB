package com.exlibris.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.exlibris.configuration.ConfigurationHandler;

public class ReportUtil {

    private static ReportUtil instance = null;
    private static final String HEADER = "Barcode,Institution,Message,\n";

    private ReportUtil() {
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        if (props.has("report_file_folder") && props.get("report_file_folder") != null) {
            reportFileFolder = props.getString("report_file_folder") + File.separator;
        } else {
            reportFileFolder = "";
        }
    }

    final static Logger logger = Logger.getLogger(ReportUtil.class);

    public static String reportFileFolder = null;

    public synchronized void appendReport(String reportName, String barcode, String institution, String message) {
        try {

            String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String filePath = reportFileFolder + reportName + "_log_" + date + ".csv";
            String line = "\"" + encodeCsv(barcode) + "\"," + encodeCsv(institution) + "," + encodeCsv(message) + ",\n";
            if (Files.notExists(Paths.get(filePath), LinkOption.NOFOLLOW_LINKS)) {
                Files.write(Paths.get(filePath), HEADER.getBytes(), StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            }
            Files.write(Paths.get(filePath), line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.warn("Can't append to file . Barcode : " + barcode + "message: " + message);
        }
    }

    public static synchronized ReportUtil getInstance() {
        if (instance == null)
            instance = new ReportUtil();
        return instance;
    }

    public static String encodeCsv(String value) {
        // Super CSV escapes double-quotes with a preceding double-quote.
        // Please note that the sometimes-used convention of escaping
        // double-quotes as \" (instead of "") is not supported.
        return value.replaceAll("\"", "\"\"");
    }

}
