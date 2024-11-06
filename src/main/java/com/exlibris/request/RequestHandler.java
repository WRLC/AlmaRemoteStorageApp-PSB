package com.exlibris.request;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.exlibris.items.ItemData;
import com.exlibris.logger.ReportUtil;
import com.exlibris.restapis.HttpResponse;
import com.exlibris.util.SCFUtil;

public class RequestHandler {

    final private static Logger logger = Logger.getLogger(RequestHandler.class);

    public static boolean createItemRequest(ItemData requestData) {
        logger.info("Create Item Request. Barcode: " + requestData.getBarcode());
        JSONObject jsonItemObject = SCFUtil.getSCFItem(requestData);
        if (jsonItemObject != null) {
            String processType = jsonItemObject.getJSONObject("item_data").getJSONObject("process_type")
                    .getString("value");
            if ("LOAN".equals(processType)) {
                logger.info("Item is on loan - Canceling Item Request. Barcode: " + requestData.getBarcode());
                if (!requestData.getSourceInstitution().isEmpty()) {
                    requestData.setInstitution(requestData.getSourceInstitution());
                }
                JSONObject jsonINSTItemObject = SCFUtil.getINSItem(requestData);
                if (jsonINSTItemObject == null) {
                    String message = "Can't cancel SCF Item Request - Failed to retrieve institution item. Barcode : "
                            + requestData.getBarcode();
                    logger.error(message);
                    ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                            requestData.getInstitution(), message);
                }
                HttpResponse requestResponce = SCFUtil.cancelItemRequest(jsonINSTItemObject, requestData,
                        requestData.getRequestId(), "Item is on loan");
                if (requestResponce.getResponseCode() != HttpsURLConnection.HTTP_NO_CONTENT) {
                    String message = "Can't cancel SCF Item Requests. Barcode : " + requestData.getBarcode();
                    logger.error(message);
                    ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                            requestData.getInstitution(), message);
                    return false;
                }
                logger.info("SCF item is on loan - successfully canceled institution " + requestData.getInstitution()
                        + " item request. Barcode: " + requestData.getBarcode());
                return true;
            }
            HttpResponse requestResponse = SCFUtil.createSCFRequest(jsonItemObject, requestData);
            if (requestResponse == null) {
                return false;
            }
        } else {
            String message = "Create Request Failed. Barcode: " + requestData.getBarcode() + "X Does not exist in SCF";
            logger.error(message);
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            return false;
        }
        return true;
    }

    public static boolean createBibRequest(ItemData itemData) {
        logger.info("Create Bib Request.get Institution : " + itemData.getSourceInstitution() + " Request : "
                + itemData.getRequestId());
        JSONObject jsonRequestObject = null;
        if (itemData.getMmsId() != null) {
            logger.info("Create Bib Request.get Institution  : " + itemData.getSourceInstitution() + " Request : "
                    + itemData.getRequestId() + " By Mms Id : " + itemData.getMmsId());
            jsonRequestObject = SCFUtil.getINSRequest(itemData);
        } else {
            logger.info("Create Bib Request.get Institution : " + itemData.getSourceInstitution() + " Request : "
                    + itemData.getRequestId() + " By User Id : " + itemData.getUserId());
            jsonRequestObject = SCFUtil.getINSUserRequest(itemData);
            if (jsonRequestObject == null) {
                String message = "Can't get institution : " + itemData.getSourceInstitution()
                        + " Requests. Failed to create Bib Request. Barcode: " + itemData.getBarcode();
                logger.error(message);
                ReportUtil.getInstance().appendReport("RequestHandler", itemData.getBarcode(),
                        itemData.getSourceInstitution(), message);
                return false;
            }
            itemData.setMmsId(jsonRequestObject.getString("mms_id"));
        }
        logger.info("Create Bib Request. Mms Id : " + itemData.getMmsId());
        logger.debug("get Institution Bib to get NZ MMS ID");
        JSONObject jsonINSBibObject = SCFUtil.getINSBib(itemData);
        if (jsonINSBibObject == null) {
            String message = "Can't get institution : " + itemData.getInstitution()
                    + " Bib - Create Request Failed. Mms Id : " + itemData.getMmsId();
            logger.error(message);
            ReportUtil.getInstance().appendReport("RequestHandler", itemData.getBarcode(), itemData.getInstitution(),
                    message);
            return false;
        }
        String networkNumber = getNetworkNumber(jsonINSBibObject.getJSONArray("network_number"));
        JSONObject jsonBibObject = null;
        if (networkNumber != null) {
            itemData.setNetworkNumber(networkNumber);
            logger.debug("get SCF Bibbased on NZ MMS ID");
            HttpResponse bibResponse = SCFUtil.getSCFBibByNZ(itemData);
            if (bibResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
                logger.debug(
                        "No bib found for NZ :" + itemData.getNetworkNumber() + ". Barcode : " + itemData.getBarcode());
                String message = "Create Request Failed. Mms Id : " + itemData.getMmsId() + bibResponse.getBody();
                logger.error(message);
                ReportUtil.getInstance().appendReport("RequestHandler", itemData.getBarcode(),
                        itemData.getInstitution(), message);
                return false;
            }
            try {
                jsonBibObject = new JSONObject(bibResponse.getBody());
                if (jsonBibObject.has("total_record_count")
                        && "0".equals(jsonBibObject.get("total_record_count").toString())) {
                    logger.debug("No bib found for NZ :" + itemData.getNetworkNumber() + ". Barcode : "
                            + itemData.getBarcode());
                    String message = "Create Request Failed. Mms Id : " + itemData.getMmsId() + bibResponse.getBody();
                    ReportUtil.getInstance().appendReport("RequestHandler", itemData.getBarcode(),
                            itemData.getInstitution(), message);
                    return false;
                }
            } catch (Exception e) {
                logger.debug(
                        "No bib found for NZ :" + itemData.getNetworkNumber() + ". Barcode : " + itemData.getBarcode());
                String message = "Create Request Failed. Mms Id : " + itemData.getMmsId() + bibResponse.getBody();
                ReportUtil.getInstance().appendReport("RequestHandler", itemData.getBarcode(),
                        itemData.getInstitution(), message);
                return false;
            }
        } else {
            logger.debug("get SCF Bibbased on Local Institution MMS ID");
            jsonBibObject = SCFUtil.getSCFBibByINST(itemData);
        }
        if (jsonBibObject == null) {
            String message = "Create Request Failed. Mms Id : " + itemData.getMmsId();
            logger.error(message);
            ReportUtil.getInstance().appendReport("RequestHandler", itemData.getBarcode(), itemData.getInstitution(),
                    message);
            return false;
        }

        HttpResponse requestResponse = SCFUtil.createSCFBibRequest(jsonBibObject, jsonRequestObject, itemData);
        if (requestResponse.getResponseCode() != HttpsURLConnection.HTTP_OK) {
            String message = "Can't create SCF request. Bib Id : " + itemData.getMmsId() + ". "
                    + requestResponse.getBody();
            logger.error(message);
            ReportUtil.getInstance().appendReport("RequestHandler", itemData.getBarcode(), itemData.getInstitution(),
                    message);
            //if (requestResponse.getBody().toLowerCase().contains("patron has active request for selected item")) {
            //    return true;
            //}
            return false;
        }
        return true;
    }

    private static String getNetworkNumber(JSONArray networkNumbers) {
        for (int i = 0; i < networkNumbers.length(); i++) {
            String networkNumber = networkNumbers.getString(i);
            if (networkNumber != null && networkNumber.contains("EXLNZ")) {
                return networkNumber.replaceAll("^\\(EXLNZ(.*)\\)", "");
            }
        }
        return null;
    }

    public static boolean createDigitizationItemRequest(ItemData requestData) {
        try {
            logger.info("Create Digitization Item Request. Barcode: " + requestData.getBarcode());
            JSONObject jsonRequestObject = SCFUtil.getINSRequest(requestData);
            if (jsonRequestObject == null) {
                String message = "Can't get institution : " + requestData.getInstitution()
                        + " Requests. Failed to create Digitization Item Request. Barcode: " + requestData.getBarcode();
                logger.error(message);
                ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                        requestData.getInstitution(), message);
                return false;
            }
            String userId = jsonRequestObject.getString("user_primary_id");
            JSONObject jsonINSUserObject = SCFUtil.getINSUser(requestData, userId, "all_unique",
                    requestData.getInstitution());
            JSONObject jsonUserObject = null;
            if (jsonINSUserObject == null) {
                String message = "Failed to create Digitization Item Request. Barcode: " + requestData.getBarcode();
                logger.error("Failed to create Digitization Item Request. Barcode: " + requestData.getBarcode());
                ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                        requestData.getInstitution(), message);
                return false;
            }
            String userLinkingId;
            String userSourceInstitution = requestData.getInstitution();
            if (!JSONObject.NULL.equals(jsonINSUserObject.opt("source_link_id")) && !JSONObject.NULL.equals(jsonINSUserObject.opt("source_institution_code"))) {
                userLinkingId = jsonINSUserObject.getString("source_link_id");
                userSourceInstitution = jsonINSUserObject.getString("source_institution_code");
                jsonUserObject = SCFUtil.getSCFUser(requestData, userLinkingId, userSourceInstitution);
                if (jsonUserObject == null) {
                    JSONObject jsonSourceUserObject = SCFUtil.getINSUser(requestData, userLinkingId, "linking_id",
                            userSourceInstitution);
                    if (jsonSourceUserObject != null) {
                        userId = jsonSourceUserObject.getString("primary_id");
                    } else {
                        String message = "Failed to create Digitization Item Request. Barcode: "
                                + requestData.getBarcode();
                        logger.error(message);
                        ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                                requestData.getInstitution(), message);
                        // if the user cannot be created we don't want the
                        // request cancelled
                        return true;
                    }
                }
            } else if (!JSONObject.NULL.equals(jsonINSUserObject.opt("linking_id") )) {
                userLinkingId = jsonINSUserObject.getString("linking_id");
                jsonUserObject = SCFUtil.getSCFUser(requestData, userLinkingId, requestData.getInstitution());
            }
            if (jsonUserObject == null) {
                jsonUserObject = SCFUtil.createSCFUser(requestData, userId, userSourceInstitution);
            }
            if (jsonUserObject == null) {
                String message = "Failed to create Digitization Item Request. Barcode: " + requestData.getBarcode();
                logger.error(message);
                ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                        requestData.getInstitution(), message);
                // if the user cannot be created we don't want the request
                // cancelled
                return true;
            }
            JSONObject jsonItemObject = SCFUtil.getSCFItem(requestData);
            JSONObject jsonDigitizationRequestObject = null;
            if (jsonItemObject == null) {
                String message = "Failed to create Digitization Item Request. Barcode: " + requestData.getBarcode();
                logger.error(message);
                ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                        requestData.getInstitution(), message);
                return false;
            }
            logger.debug("Refresh SCF user. User Id: " + jsonUserObject.getString("primary_id"));
            SCFUtil.refreshLinkedUser(requestData, jsonUserObject.getString("primary_id"));

            jsonDigitizationRequestObject = SCFUtil.createSCFDigitizationRequest(jsonUserObject, jsonRequestObject,
                    jsonItemObject, requestData);
            if (jsonDigitizationRequestObject != null) {
                SCFUtil.cancelTitleRequest(requestData);
                return true;
            }
            return false;

        } catch (Exception e) {
            String message = "Create Request Failed. Barcode: " + requestData.getBarcode();
            logger.warn("Create Request Failed. Barcode: " + requestData.getBarcode());
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            return false;
        }

    }

    public static boolean createDigitizationUserRequest(ItemData requestData) {
        logger.info("Create Digitization User Request. User Id : " + requestData.getUserId());
        JSONObject jsonRequestObject = SCFUtil.getINSUserRequest(requestData);
        if (jsonRequestObject == null) {
            String message = "Can't get institution User Requests - Failed to create Digitization User Request. User Id : "
                    + requestData.getUserId();
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            logger.error(message);
            return false;
        }
        requestData.setMmsId(jsonRequestObject.getString("mms_id"));
        String userId = jsonRequestObject.getString("user_primary_id");
        JSONObject jsonINSUserObject = SCFUtil.getINSUser(requestData, userId, "all_unique",
                requestData.getInstitution());
        JSONObject jsonUserObject = null;
        if (jsonINSUserObject == null) {
            String message = "Failed to create Digitization User Request. User Id : " + requestData.getUserId();
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            logger.error(message);
            return false;
        }
        String userLinkingId;
        String userSourceInstitution = requestData.getInstitution();
        if (!JSONObject.NULL.equals(jsonINSUserObject.opt("source_link_id")) && !JSONObject.NULL.equals(jsonINSUserObject.opt("source_institution_code"))) {
            userLinkingId = jsonINSUserObject.getString("source_link_id");
            userSourceInstitution = jsonINSUserObject.getString("source_institution_code");
            jsonUserObject = SCFUtil.getSCFUser(requestData, userLinkingId, userSourceInstitution);
            if (jsonUserObject == null) {
                JSONObject jsonSourceUserObject = SCFUtil.getINSUser(requestData, userLinkingId, "linking_id",
                        userSourceInstitution);
                if (jsonSourceUserObject != null) {
                    userId = jsonSourceUserObject.getString("primary_id");
                } else {
                    String message = "Failed to create Digitization User Request. User Id : " + requestData.getUserId();
                    ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                            requestData.getInstitution(), message);
                    logger.error(message);
                    // if the user cannot be created we don't want the request
                    // cancelled
                    return true;
                }
            }
        } else if (!JSONObject.NULL.equals(jsonINSUserObject.opt("linking_id"))) {
            userLinkingId = jsonINSUserObject.getString("linking_id");
            jsonUserObject = SCFUtil.getSCFUser(requestData, userLinkingId, requestData.getInstitution());
        }
        if (jsonUserObject == null) {
            jsonUserObject = SCFUtil.createSCFUser(requestData, userId, userSourceInstitution);
        }
        if (jsonUserObject == null) {
            String message = "Failed to create Digitization User Request. User Id : " + requestData.getUserId();
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            logger.error(message);
            // if the user cannot be created we don't want the request cancelled
            return true;
        }

        JSONObject jsonBibObject = getSCFBibByInstMmsId(requestData);
        if (jsonBibObject == null) {
            String message = "Failed to create Digitization User Request. User Id : " + requestData.getUserId()
                    + " Can't get institution : " + requestData.getInstitution()
                    + " Bib - Create Request Failed. Mms Id : " + requestData.getMmsId();
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            logger.error(message);
            return false;
        }
        logger.debug("Refresh SCF user. User Id: " + jsonUserObject.getString("primary_id"));
        SCFUtil.refreshLinkedUser(requestData, jsonUserObject.getString("primary_id"));

        JSONObject jsonDigitizationRequestObject = SCFUtil.createSCFDigitizationUserRequest(jsonUserObject,
                jsonRequestObject, jsonBibObject, requestData);
        if (jsonDigitizationRequestObject != null) {
            SCFUtil.cancelTitleRequest(requestData);
            return true;
        }
        return false;
    }

    public static JSONObject getSCFBibByInstMmsId(ItemData itemData) {
        logger.debug("get Institution Bib to get NZ MMS ID");
        JSONObject jsonINSBibObject = SCFUtil.getINSBib(itemData);
        if (jsonINSBibObject == null) {
            return null;
        }
        String networkNumber = getNetworkNumber(jsonINSBibObject.getJSONArray("network_number"));
        JSONObject jsonBibObject = null;
        if (networkNumber != null) {
            itemData.setNetworkNumber(networkNumber);
            logger.debug("get SCF Bib based on NZ MMS ID");
            HttpResponse bibResponse = SCFUtil.getSCFBibByNZ(itemData);
            try {
                jsonBibObject = new JSONObject(bibResponse.getBody());
                if (jsonBibObject.has("total_record_count")
                        && "0".equals(jsonBibObject.get("total_record_count").toString())) {
                    logger.debug("No bib found for NZ :" + itemData.getNetworkNumber() + ". Barcode : "
                            + itemData.getBarcode());
                    return null;
                }
            } catch (Exception e) {
                logger.debug(
                        "No bib found for NZ :" + itemData.getNetworkNumber() + ". Barcode : " + itemData.getBarcode());
                return null;
            }
        } else {
            logger.debug("get SCF Bib based on Local Institution MMS ID");
            jsonBibObject = SCFUtil.getSCFBibByINST(itemData);
        }
        if (jsonBibObject == null) {
            return null;
        }
        return jsonBibObject;
    }

    public static void cancelRequest(ItemData requestData) {
        logger.info("Failed to create request - canceling source reqrest : " + requestData.getRequestId());
        SCFUtil.cancelRequest(requestData);
    }

}
