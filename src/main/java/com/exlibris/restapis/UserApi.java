package com.exlibris.restapis;

import org.apache.log4j.Logger;

public class UserApi {

    final private static Logger logger = Logger.getLogger(UserApi.class);

    public static HttpResponse createLinkedUser(String userId, String institutionCode, String body, String baseUrl,
            String apiKey) {

        logger.info("Starting to handle create Linked User: " + body + ".");
        logger.info("User Id: " + userId + " ,Source institution code: " + institutionCode + "- calling POST");

        String url = baseUrl + "/almaws/v1/users?source_institution_code=" + institutionCode + "&source_user_id="
                + userId + "&apikey=" + apiKey;
        HttpResponse userResponse = AlmaRestUtil.sendHttpReq(url, "POST", body);

        return userResponse;

    }

    public static HttpResponse getLinkedUser(String userId, String institutionCode, String baseUrl,
            String remoteStorageApikey) {
        logger.info("Starting to handle get Linked User.");
        logger.info("User Id: " + userId + " ,Source institution code: " + institutionCode + "- calling GET");

        String url = baseUrl + "/almaws/v1/users/" + userId + "?source_institution_code=" + institutionCode + "&apikey="
                + remoteStorageApikey;
        HttpResponse userResponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return userResponse;
    }

    public static HttpResponse getUser(String userId, String userIdType, String baseUrl, String apiKey) {
        logger.info("Starting to handle get User.");
        logger.info("User Id: " + userId + "- calling GET");

        String url = baseUrl + "/almaws/v1/users/" + userId + "?user_id_type=" + userIdType + "&apikey=" + apiKey;
        HttpResponse userResponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return userResponse;
    }

    public static HttpResponse getUserRequest(String userId, String requestId, String baseUrl, String apiKey) {
        logger.info("Starting to handle get User Request.");
        logger.info("Request Id: " + requestId + "- calling GET");

        String url = baseUrl + "/almaws/v1/users/" + userId + "/requests/" + requestId + "?apikey=" + apiKey;
        HttpResponse userResponse = AlmaRestUtil.sendHttpReq(url, "GET", null);

        return userResponse;
    }

    public static HttpResponse refreshLinkedUser(String userId, String baseUrl, String apiKey) {

        logger.info("Starting to handle refresh Linked User.");
        logger.info("User Id: " + userId + "- calling POST");

        String url = baseUrl + "/almaws/v1/users/" + userId + "?op=refresh&apikey=" + apiKey;
        HttpResponse userResponse = AlmaRestUtil.sendHttpReq(url, "POST", null);

        return userResponse;

    }

}