package com.exlibris.restapis;

import org.apache.log4j.Logger;

public class ConfApi {

    final private static Logger logger = Logger.getLogger(ConfApi.class);

    public static HttpResponse retrieveLibraries(String baseurl, String apiKey) {

        logger.info("Starting to handle retrieve libraries ");

        String url = baseurl + "/almaws/v1/conf/libraries" + "?apikey=" + apiKey;
        HttpResponse librariessponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return librariessponse;
    }
    
    
    public static HttpResponse retrieveLibraryLocations(String libraryCode, String baseurl, String apiKey) {

        logger.info("Starting to handle retrieve libraries ");

        String url = baseurl + "/almaws/v1/conf/libraries/"+libraryCode+"/locations" + "?apikey=" + apiKey;
        HttpResponse librariessponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return librariessponse;
    }

}
