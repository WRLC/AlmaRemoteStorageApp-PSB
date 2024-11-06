package com.exlibris.restapis;

import org.apache.log4j.Logger;

public class BibApi {

    final private static Logger logger = Logger.getLogger(AlmaRestUtil.class);

    public static HttpResponse retrieveBibsbyNZ(String nzMmsId, String view, String expand, String baseurl,
            String apiKey) {

        logger.info("Starting to handle retrieve Bibs from NZ: " + nzMmsId + ".");
        logger.info("Network Number: " + nzMmsId + " - calling GET");

        String url = baseurl + "/almaws/v1/bibs?nz_mms_id=" + nzMmsId + "&view=" + view + "&expand=" + expand
                + "&apikey=" + apiKey;
        HttpResponse bibResponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return bibResponse;
    }

    public static HttpResponse getBib(String mmsId, String view, String expand, String baseurl, String apiKey) {

        logger.info("Starting to handle retrieve Bibs Mms Id: " + mmsId + ".");
        logger.info("Mms Id: " + mmsId + " - calling GET");

        String url = baseurl + "/almaws/v1/bibs/" + mmsId + "?view=" + view + "&expand=" + expand + "&apikey=" + apiKey;
        HttpResponse bibResponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return bibResponse;
    }

    public static HttpResponse createBibByNZ(String fromNzMmsId, String normalization, String validate,
            String overrideWarning, String body, String baseurl, String apiKey) {

        logger.info("Starting to handle creating Bibs: " + body + ".");
        logger.info("from NZ Mms Id: " + fromNzMmsId + " - calling POST");

        String url = baseurl + "/almaws/v1/bibs?from_nz_mms_id=" + fromNzMmsId + "&normalization=" + normalization
                + "&validate=" + validate + "&override_warning=" + overrideWarning + "&format=json&apikey=" + apiKey;

        HttpResponse bibResponse = AlmaRestUtil.sendHttpReq(url, "POST", body);

        return bibResponse;
    }

    public static HttpResponse retrieveBibsBySId(String localNumber, String view, String expand, String baseUrl,
            String remoteStorageApikey) {
        logger.info("Starting to handle retrieve Bibs from Other System Id: " + localNumber + ".");
        logger.info("additional ID: " + localNumber + " - calling GET");

        String url = baseUrl + "/almaws/v1/bibs?other_system_id=" + localNumber + "&view=" + view + "&expand=" + expand
                + "&apikey=" + remoteStorageApikey;
        HttpResponse bibResponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return bibResponse;
    }

    public static HttpResponse createBibBySId(String validate, String overrideWarning, String body, String baseurl,
            String apiKey) {

        logger.info("Starting to handle creating Bibs: " + body + ".");

        String url = baseurl + "/almaws/v1/bibs?validate=" + validate + "&override_warning=" + overrideWarning
                + "&format=xml&apikey=" + apiKey;

        HttpResponse bibResponse = AlmaRestUtil.sendHttpReq(url, "POST", body);

        return bibResponse;
    }
}
