package com.exlibris.restapis;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;

public class RequestApi {

    final private static Logger logger = Logger.getLogger(RequestApi.class);

    public static HttpResponse createRequest(String mmsId, String holdingId, String itemId, String baseurl,
            String apiKey, String body, String userId) {

        logger.info("Starting to handle creating Request: " + body + ".");
        logger.info("Item Id: " + itemId + " - calling POST");

        String url = baseurl + "/almaws/v1/bibs/" + mmsId + "/holdings/" + holdingId + "/items/" + itemId
                + "/requests?user_id=" + encodeValue(userId) + "&allow_same_request=true&apikey=" + apiKey;
        HttpResponse requestResponse = AlmaRestUtil.sendHttpReq(url, "POST", body);

        return requestResponse;
    }

    public static HttpResponse getRequest(String mmsId, String holdingId, String itemId, String requestId,
            String baseurl, String apiKey) {

        logger.info("Starting to handle get Request Id: " + requestId + ".");
        logger.info("item Id: " + itemId + " - calling GET");

        String url = baseurl + "/almaws/v1/bibs/" + mmsId + "/holdings/" + holdingId + "/items/" + itemId + "/requests/"
                + requestId + "?apikey=" + apiKey;
        HttpResponse requestResponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return requestResponse;
    }

    public static HttpResponse createBibRequest(String mmsId, String baseUrl, String apiKey, String body,
            String userId) {
        logger.info("Starting to handle creating Request: " + body + ".");
        logger.info("Mms Id: " + mmsId + " - calling POST");

        String url = baseUrl + "/almaws/v1/bibs/" + mmsId + "/requests?user_id=" + encodeValue(userId)
                + "&allow_same_request=true&apikey=" + apiKey;
        HttpResponse bibResponse = AlmaRestUtil.sendHttpReq(url, "POST", body);

        return bibResponse;
    }

    public static HttpResponse getRequests(String mmsId, String holdingId, String itemId, String baseurl,
            String apiKey) {

        logger.info("Starting to handle retrieve item requests mms id: " + mmsId + ".");
        logger.info("item Id: " + itemId + " - calling GET");

        String url = baseurl + "/almaws/v1/bibs/" + mmsId + "/holdings/" + holdingId + "/items/" + itemId
                + "/requests?apikey=" + apiKey;
        HttpResponse requestResponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return requestResponse;
    }

    public static HttpResponse cancelRequest(String mmsId, String holdingId, String itemPid, String requestId,
            String reason, String note, String baseUrl, String apiKey) {

        logger.info("Starting to handle cancel request, request id: " + requestId + ".");
        logger.info("item Id: " + itemPid + " - calling DELETE");

        String url = baseUrl + "/almaws/v1/bibs/" + mmsId + "/holdings/" + holdingId + "/items/" + itemPid
                + "/requests/" + requestId + "?reason=" + reason + "&note=" + encodeValue(note) + "&apikey=" + apiKey;

        HttpResponse requestResponse = AlmaRestUtil.sendHttpReq(url, "DELETE", null);
        return requestResponse;
    }

    private static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception ex) {
            return "";
        }
    }

    public static HttpResponse getRequest(String mmsId, String requestId, String baseUrl, String apiKey) {
        logger.info("Starting to handle retrieve request mms id: " + mmsId + ".");
        logger.info("request Id: " + requestId + " - calling GET");

        String url = baseUrl + "/almaws/v1/bibs/" + mmsId + "/requests/" + requestId + "?apikey=" + apiKey;
        HttpResponse requestResponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return requestResponse;
    }

    public static HttpResponse cancleTitleRequest(String mmsId, String requestId, String reason, String note,
            String notifyUser, String baseUrl, String apiKey) {
        logger.info("Starting to handle cancel request, request id: " + requestId + ".");
        logger.info("Mms Id: " + mmsId + " - calling DLETE");

        String url = baseUrl + "/almaws/v1/bibs/" + mmsId + "/requests/" + requestId + "?reason=" + reason
                + "&notify_user=" + notifyUser + "&note=" + encodeValue(note) + "&apikey=" + apiKey;

        HttpResponse requestResponse = AlmaRestUtil.sendHttpReq(url, "DELETE", null);
        return requestResponse;
    }

    public static HttpResponse cancleUserRequest(String userId, String requestId, String reason, String note,
            String notifyUser, String baseUrl, String apiKey) {
        logger.info("Starting to handle cancel request, request id: " + requestId + ".");
        logger.info("User Id: " + userId + " - calling DLETE");

        String url = baseUrl + "/almaws/v1/users/" + userId + "/requests/" + requestId + "?reason=" + reason
                + "&notify_user=" + notifyUser + "&note=" + encodeValue(note) + "&apikey=" + apiKey;

        HttpResponse requestResponse = AlmaRestUtil.sendHttpReq(url, "DELETE", null);
        return requestResponse;
    }

}
