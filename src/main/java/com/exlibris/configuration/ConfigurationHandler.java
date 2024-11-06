package com.exlibris.configuration;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class ConfigurationHandler {

    final private static Logger logger = Logger.getLogger(ConfigurationHandler.class);

    final private static String configurationFile = "conf.json";

    private static ConfigurationHandler instance = null;

    private JSONObject props = null;

    private ConfigurationHandler() {
        JSONObject jsonObject = null;
        String content = null;
        try {
            if (System.getenv("CONFIG_FILE") != null) {
                URLConnection urlc = new URL(System.getenv("CONFIG_FILE")).openConnection();
                content = IOUtils.toString(urlc.getInputStream(), "UTF-8");
                logger.info("Success geting file from :" + System.getenv("CONFIG_FILE"));
            } else {
                try {
                    URL resource = getClass().getClassLoader().getResource(configurationFile);
                    System.out.println(resource.getPath());
                    content = new String(Files.readAllBytes(Paths.get(resource.toURI())));
                    logger.info("Success geting file from :" + resource.getPath());
                } catch (FileSystemNotFoundException e) {
                    // in jar file
                    InputStream resource = getClass().getClassLoader().getResourceAsStream(configurationFile);
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(resource, writer, "UTF-8");
                    content = writer.toString();
                }
            }
            logger.info("loading conf.json");
            jsonObject = new JSONObject(content);
        } catch (Exception e) {
            logger.error("Unable to load propeties from " + configurationFile + " file", e);
        }
        this.props = jsonObject;
    }

    public static synchronized ConfigurationHandler getInstance() {
        if (instance == null)
            instance = new ConfigurationHandler();
        return instance;
    }

    public JSONObject getConfiguration() {
        return this.props;
    }

    public static synchronized void updateInstance() {
        instance = new ConfigurationHandler();
    }

}
