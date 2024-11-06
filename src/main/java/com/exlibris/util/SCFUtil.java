package com.exlibris.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;

import com.exlibris.configuration.ConfigurationHandler;
import com.exlibris.items.ItemData;
import com.exlibris.logger.ReportUtil;
import com.exlibris.restapis.BibApi;
import com.exlibris.restapis.ConfApi;
import com.exlibris.restapis.HoldingApi;
import com.exlibris.restapis.HttpResponse;
import com.exlibris.restapis.ItemApi;
import com.exlibris.restapis.LoanApi;
import com.exlibris.restapis.RequestApi;
import com.exlibris.restapis.UserApi;

public class SCFUtil {

    final private static Logger logger = Logger.getLogger(SCFUtil.class);
    final private static String HOL_XML_TEMPLATE = "<holding><record><datafield ind1=\"0\" ind2=\" \" tag=\"852\"><subfield code=\"b\">_LIB_CODE_</subfield><subfield code=\"c\">_LOC_CODE_</subfield></datafield></record><suppress_from_publishing>false</suppress_from_publishing></holding>";

    private static Set<String> locationList = new HashSet<String>();
    
    public static String getSCFHoldingFromRecordAVA(String record) {
        try {
            // no AVA fields
            if (!record.contains("tag=\"AVA\"")) {
                return null;
            }
            JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
            String remoteStorageInst = props.getString("remote_storage_inst");
            String remoteStorageHoldingLibrary = props.getString("remote_storage_holding_library");
            String remoteStorageHoldingLocation = props.getString("remote_storage_holding_location");
            JSONArray institutions = props.getJSONArray("institutions");
            String r = XmlUtil.recordXmlToMarcXml(record);
            List<Record> Marcrecord = XmlUtil.xmlStringToMarc4jRecords(r);
            List<VariableField> variableFields = Marcrecord.get(0).getVariableFields("AVA");
            for (VariableField variableField : variableFields) {
                String holdingsID = ((DataField) variableField).getSubfieldsAsString("8");
                String library = ((DataField) variableField).getSubfieldsAsString("b");
                String location = ((DataField) variableField).getSubfieldsAsString("j");
                // if it's the default remote_storage_holding_library
                // and remote_storage_holding_location
                if (library.equals(remoteStorageHoldingLibrary)) {
                    if (location.equals(remoteStorageHoldingLocation)) {
                        return holdingsID;
                    }
                }
                // check if one of library locations is in the institution
                // configuration
                for (int i = 0; i < institutions.length(); i++) {
                    JSONObject inst = institutions.getJSONObject(i);
                    if (inst.get("code").toString().equals(remoteStorageInst)) {
                        JSONArray libraries = inst.getJSONArray("libraries");
                        for (int j = 0; j < libraries.length(); j++) {
                            if (library.equals(libraries.getJSONObject(j).get("code").toString())) {
                                if (libraries.getJSONObject(j).getJSONArray("remote_storage_location").toString()
                                        .contains(location)) {
                                    return holdingsID;
                                }
                            }
                        }
                        // only one institution can be equal
                        break;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static HttpResponse getSCFBibByNZ(ItemData itemData) {
        logger.debug("get SCF Bib. Barcode : " + itemData.getBarcode());
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();
        String networkNumber = itemData.getNetworkNumber();
        HttpResponse bibResponse = BibApi.retrieveBibsbyNZ(networkNumber, "full", "p_avail", baseUrl,
                remoteStorageApikey);
        return bibResponse;
    }

    public static JSONObject getSCFBibByINST(ItemData itemData) {
        logger.debug("get SCF Bib. Barcode : " + itemData.getBarcode());
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();
        String localNumber = "(" + itemData.getInstitution() + ")" + itemData.getMmsId();
        HttpResponse bibResponse = BibApi.retrieveBibsBySId(localNumber, "full", "p_avail", baseUrl,
                remoteStorageApikey);
        if (bibResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.debug("No bib found for System Id :" + localNumber + ". Barcode : " + itemData.getBarcode());
            return null;
        }
        JSONObject jsonBibObject = null;
        try {
            jsonBibObject = new JSONObject(bibResponse.getBody());
            if (jsonBibObject.has("total_record_count")
                    && "0".equals(jsonBibObject.get("total_record_count").toString())) {
                logger.debug("No bib found for System Id :" + localNumber + ". Barcode : " + itemData.getBarcode());
                return null;
            }
        } catch (Exception e) {
            logger.debug("No bib found for System Id :" + localNumber + ". Barcode : " + itemData.getBarcode());
            return null;
        }
        return jsonBibObject;
    }

    public static JSONObject getSCFItem(ItemData itemData) {
        logger.debug("get SCF Item. Barcode : " + itemData.getBarcode());
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();
        HttpResponse itemResponce = ItemApi.retrieveItem(itemData.getBarcode() + "X", baseUrl, remoteStorageApikey);
        if (itemResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.debug("No items found . Barcode : " + itemData.getBarcode());
            return null;
        }
        JSONObject jsonItemObject = new JSONObject(itemResponce.getBody());
        return jsonItemObject;
    }

    public static boolean isItemInRemoteStorage(ItemData itemData) {
        if (itemData.getLibrary() == null || itemData.getLocation() == null) {
            return false;
        }
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String institution = itemData.getInstitution();
        String library = itemData.getLibrary();
        String location = itemData.getLocation();
        JSONArray institutions = props.getJSONArray("institutions");
        for (int i = 0; i < institutions.length(); i++) {
            JSONObject inst = institutions.getJSONObject(i);
            if (inst.get("code").toString().equals(institution)) {
                JSONArray libraries = inst.getJSONArray("libraries");
                for (int j = 0; j < libraries.length(); j++) {
                    if (library.equals(libraries.getJSONObject(j).get("code").toString())) {
                        if (libraries.getJSONObject(j).getJSONArray("remote_storage_location").toString()
                                .contains(location)) {
                            return true;
                        }
                    }
                }
                break;
            }
        }
        logger.debug("Item is not located in a remote-storage location. library: " + library + "location: " + location
                + "Inst: " + institution);
        return false;
    }

    public static JSONObject createSCFBibByNZ(ItemData itemData) {
        logger.debug("create SCF Bib. Barcode : " + itemData.getBarcode());
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();

        HttpResponse bibResponse = BibApi.createBibByNZ(itemData.getNetworkNumber(), null, "false", "true",
                "<bib></bib>", baseUrl, remoteStorageApikey);
        if (bibResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't create SCF bib. Barcode : " + itemData.getBarcode());
            return null;
        }
        JSONObject jsonNewBibObject = new JSONObject(bibResponse.getBody());
        return jsonNewBibObject;
    }

    public static String createSCFHoldingAndGetId(JSONObject jsonBibObject, String mmsId, ItemData itemData) {
        return createSCFHolding(jsonBibObject, mmsId, itemData);
    }

    private static String createSCFHolding(JSONObject jsonBibObject, String mmsId, ItemData itemData) {
        logger.debug("create SCF Holding. MMS ID : " + mmsId);
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();
        String holdingLib = props.get("remote_storage_holding_library").toString();
        String defultholdingLoc = props.get("remote_storage_holding_location").toString();
        String holdingLoc = getLocationForNewHolding(holdingLib,defultholdingLoc,itemData);
        String holdingBody = HOL_XML_TEMPLATE.replace("_LIB_CODE_", holdingLib).replace("_LOC_CODE_", holdingLoc);
        HttpResponse holdingResponse = HoldingApi.createHolding(mmsId, holdingBody, baseUrl, remoteStorageApikey);
        if (holdingResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't create SCF holding. MMS ID : " + mmsId);
            return null;
        }
        JSONObject jsonHoldingObject = new JSONObject(holdingResponse.getBody());
        return jsonHoldingObject.getString("holding_id");
    }

    private static String getLocationForNewHolding(String holdingLib,String defultholdingLoc, ItemData itemData) {
    	logger.debug("get SCF loctions for library :" + holdingLib );
    	if(locationList.contains(itemData.getLocation())){
        	return itemData.getLocation();
        }
    	
    	logger.debug("get SCF loctions for library :" + holdingLib +" getting locations from API");
    	JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();
        HttpResponse locationsResponse = ConfApi.retrieveLibraryLocations(holdingLib, baseUrl,remoteStorageApikey);
        
        if (locationsResponse.getResponseCode() < HttpsURLConnection.HTTP_OK
                || locationsResponse.getResponseCode() >= HttpsURLConnection.HTTP_MOVED_PERM) {
            logger.warn("Can't get SCF Library Locations - " + locationsResponse.getBody() + ". Library : "
                    + holdingLib);
            return defultholdingLoc;
        }
        JSONObject librariesJson = new JSONObject(locationsResponse.getBody());
        for (int i = 0; i < librariesJson.getJSONArray("location").length(); i++) {
            JSONObject location = librariesJson.getJSONArray("location").getJSONObject(i);
            try {
            	locationList.add(location.getString("code"));
            } catch (JSONException e) {
            }
        }
        if(locationList.contains(itemData.getLocation())){
        	return itemData.getLocation();
        }
        logger.debug("SCF loction for library :" + holdingLib +" and location " + itemData.getLocation() + " does not exist returning defult location");
        return defultholdingLoc;
	}

	public static JSONObject getINSItem(ItemData itemData) {
        logger.debug("get institution : " + itemData.getInstitution() + " Item. Barcode : " + itemData.getBarcode());
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(itemData.getInstitution())) {
                institutionApiKey = inst.getString("apikey");
                break;
            }
        }
        HttpResponse itemResponce = ItemApi.retrieveItem(itemData.getBarcode(), baseUrl, institutionApiKey);
        if (itemResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't get institution : " + itemData.getInstitution() + " item. Barcode : "
                    + itemData.getBarcode());
            return null;
        }
        return new JSONObject(itemResponce.getBody());
    }

    public static String createSCFItemAndGetId(ItemData itemData, String mmsId, String holdingId) {
        JSONObject jsonItemObject = createSCFItem(itemData, mmsId, holdingId);
        if (jsonItemObject != null) {
            return jsonItemObject.getJSONObject("item_data").getString("pid");
        }
        return null;
    }

    public static JSONObject createSCFItem(ItemData itemData, String mmsId, String holdingId) {
        logger.debug("create SCF Item. Barcode : " + itemData.getBarcode());
        JSONObject instItem = getINSItem(itemData);
        if (instItem == null) {
            logger.warn("Can't create SCF item - Can't get institution : " + itemData.getInstitution()
                    + " item. Barcode : " + itemData.getBarcode());
            ReportUtil.getInstance().appendReport("ItemsHandler", itemData.getBarcode(), itemData.getInstitution(),
                    "Can't create SCF item. Barcode : " + itemData.getBarcode());
            return null;
        }
        instItem.getJSONObject("item_data").put("barcode", itemData.getBarcode() + "X");
        JSONObject provenance = new JSONObject();
        provenance.put("value", itemData.getInstitution());
        instItem.getJSONObject("item_data").put("provenance", provenance);
        instItem.getJSONObject("item_data").remove("po_line");
        instItem.getJSONObject("item_data").remove("library");
        instItem.getJSONObject("item_data").remove("location");
        instItem.getJSONObject("item_data").remove("policy");
        instItem.getJSONObject("item_data").remove("internal_note_1");
        instItem.getJSONObject("item_data").remove("internal_note_3");
        instItem.getJSONObject("item_data").remove("call_number");
        instItem.getJSONObject("holding_data").remove("temp_library");
        instItem.getJSONObject("holding_data").remove("in_temp_location");
        instItem.getJSONObject("holding_data").remove("temp_location");
        instItem.getJSONObject("holding_data").remove("temp_policy");

        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();
        HttpResponse itemResponse = ItemApi.createItem(mmsId, holdingId, baseUrl, remoteStorageApikey,
                instItem.toString());
        if (itemResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            ReportUtil.getInstance().appendReport("ItemsHandler", itemData.getBarcode(), itemData.getInstitution(),
                    "Can't create SCF item. Barcode : " + itemData.getBarcode());
            logger.warn("Can't create SCF item. Barcode : " + itemData.getBarcode() + " ." + itemResponse.getBody());
            return null;
        }
        return new JSONObject(itemResponse.getBody());
    }

    public static boolean deleteSCFItem(JSONObject jsonItemObject, ItemData itemData) {
        logger.debug("delete SCF Item. Barcode: " + jsonItemObject.getJSONObject("item_data").getString("barcode"));
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();
        String mmsId = jsonItemObject.getJSONObject("bib_data").getString("mms_id");
        String holdingId = jsonItemObject.getJSONObject("holding_data").getString("holding_id");
        String itemPid = jsonItemObject.getJSONObject("item_data").getString("pid");
        HttpResponse itemResponse = ItemApi.deleteItem(mmsId, holdingId, itemPid, null, null, baseUrl,
                remoteStorageApikey);
        if (itemResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't delete SCF item : " + itemPid);
            ReportUtil.getInstance().appendReport("ItemsHandler", itemData.getBarcode(), itemData.getInstitution(),
                    "Can't delete SCF item : " + itemPid);
            return false;
        }
        return true;
    }

    public static void updateSCFItem(ItemData itemData, JSONObject scfItem) {
        logger.debug("update SCF Item. Barcode : " + itemData.getBarcode());
        JSONObject instItem = getINSItem(itemData);
        if (instItem == null) {
            String message = "Can't update SCF Item - Can't get institution : " + itemData.getInstitution()
                    + " item. Barcode : " + itemData.getBarcode();
            logger.warn(message);
            ReportUtil.getInstance().appendReport("ItemsHandler", itemData.getBarcode(), itemData.getInstitution(),
                    message);
            return;
        }
        JSONObject scfItemData = scfItem.getJSONObject("item_data");
        instItem.getJSONObject("item_data").put("pid", scfItemData.getString("pid"));
        instItem.getJSONObject("item_data").put("barcode", itemData.getBarcode() + "X");
        instItem.getJSONObject("item_data").put("provenance", scfItemData.get("provenance"));
        instItem.getJSONObject("item_data").remove("po_line");
        instItem.getJSONObject("item_data").put("library", scfItemData.get("library"));
        instItem.getJSONObject("item_data").put("location", scfItemData.get("location"));
        instItem.getJSONObject("item_data").remove("policy");
        instItem.getJSONObject("item_data").put("alternative_call_number", scfItemData.get("alternative_call_number"));
        instItem.getJSONObject("item_data").put("alternative_call_number_type",
                scfItemData.get("alternative_call_number_type"));
        instItem.getJSONObject("item_data").put("storage_location_id", scfItemData.get("storage_location_id"));
        instItem.getJSONObject("item_data").put("internal_note_1", scfItemData.get("internal_note_1"));
        instItem.getJSONObject("item_data").put("internal_note_3", scfItemData.get("internal_note_3"));
        instItem.getJSONObject("item_data").put("statistics_note_2", scfItemData.get("statistics_note_2"));
        instItem.put("holding_data", scfItem.get("holding_data"));
        instItem.put("bib_data", scfItem.get("bib_data"));

        String mmsId = scfItem.getJSONObject("bib_data").getString("mms_id");
        String holdingId = scfItem.getJSONObject("holding_data").getString("holding_id");
        String itemPid = scfItemData.getString("pid");
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();
        String body = instItem.toString();
        HttpResponse itemResponse = ItemApi.updateItem(mmsId, holdingId, itemPid, baseUrl, remoteStorageApikey, body);
        if (itemResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            // "Can't update item";
            String message = "Can't update SCF item. Barcode : " + itemData.getBarcode() + ". "
                    + itemResponse.getBody();
            logger.warn(message);
            ReportUtil.getInstance().appendReport("ItemsHandler", itemData.getBarcode(), itemData.getInstitution(),
                    message);
        }

    }

    public static JSONObject getINSBib(ItemData itemData) {
        logger.debug("get institution : " + itemData.getSourceInstitution() + "Bib. Mms Id : " + itemData.getMmsId());
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(itemData.getSourceInstitution())) {
                institutionApiKey = inst.getString("apikey");
                break;
            }
        }
        HttpResponse itemResponce = BibApi.getBib(itemData.getMmsId(), "full", "None", baseUrl, institutionApiKey);
        if (itemResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't get institution : " + itemData.getInstitution() + " Bib. MMS Id : "
                    + itemData.getNetworkNumber());
            return null;
        }
        JSONObject jsonItemObject = new JSONObject(itemResponce.getBody());
        return jsonItemObject;
    }

    public static HttpResponse createSCFRequest(JSONObject jsonItemObject, ItemData itemData) {
        logger.debug("create SCF Request. Barcode: " + jsonItemObject.getJSONObject("item_data").getString("barcode"));
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();

        String mmsId = jsonItemObject.getJSONObject("bib_data").getString("mms_id");
        String holdingId = jsonItemObject.getJSONObject("holding_data").getString("holding_id");
        String itemPid = jsonItemObject.getJSONObject("item_data").getString("pid");
        String userId = getUserIdByIns(itemData);
        JSONObject jsonRequest = getRequestObj();
        jsonRequest.put("user_primary_id", userId);
        
        String comment ="";
        if(itemData.getRequestNote() != null) {
        	comment =  itemData.getRequestNote() + " ";
        }
        if(itemData.getPatron() != null) {
        	comment += itemData.getPatron().toString();
        } 
        jsonRequest.put("comment", comment);

        HttpResponse requestResponse = RequestApi.createRequest(mmsId, holdingId, itemPid, baseUrl, remoteStorageApikey,
                jsonRequest.toString(), userId);
        if (requestResponse.getResponseCode() != HttpsURLConnection.HTTP_OK) {
            String message = "Can't create SCF request. Item Pid : " + itemPid + "." + requestResponse.getBody();
            logger.error(message);
            ReportUtil.getInstance().appendReport("RequestHandler", itemData.getBarcode(), itemData.getInstitution(),
                    message);
            //if (requestResponse.getBody().toLowerCase().contains("patron has active request for selected item")) {
            //   return requestResponse;
            //}
            return null;
        }
        return requestResponse;
    }

    public static HttpResponse createSCFBibRequest(JSONObject jsonBibObject, JSONObject jsonRequestObject,
            ItemData itemData) {
        String mmsId = null;
        try {
            mmsId = jsonBibObject.getJSONArray("bib").getJSONObject(0).getString("mms_id");
        } catch (Exception e) {
            mmsId = jsonBibObject.getString("mms_id");
        }
        logger.debug("create SCF Request. Bib: " + mmsId);
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();

        String userId = getUserIdByIns(itemData);

        JSONObject jsonRequest = getRequestObj();
        jsonRequest.put("user_primary_id", userId);
        jsonRequest.put("description", itemData.getDescription());
        String comment = "";
        if (jsonRequestObject.has("comment") && !jsonRequestObject.get("comment").equals(null)) {
            comment += jsonRequestObject.getString("comment") + " ";
        }
        comment += "The inventory for this request should come from " + itemData.getSourceInstitution()+". ";
        if(itemData.getPatron() != null) {
        	comment += itemData.getPatron().toString();
        }
        if (jsonRequestObject != null) {
            if (itemData.getDescription() == null && jsonRequestObject.has("description")) {
                jsonRequest.put("description", jsonRequestObject.get("description"));
            }
            if (jsonRequestObject.has("manual_description") && !jsonRequestObject.get("manual_description").equals(null)
                    && !jsonRequestObject.get("manual_description").equals("")) {
                String holdingLib = props.get("remote_storage_holding_library").toString();
                String holdingLoc = props.get("remote_storage_holding_location").toString();
                String holdingId = getSCFHoldingByBib(mmsId, holdingLib, holdingLoc, baseUrl, remoteStorageApikey);
                if (holdingId != null) {
                    jsonRequest.put("manual_description", jsonRequestObject.get("manual_description"));
                    jsonRequest.put("holding_id", holdingId);
                } else {
                    comment += " " + jsonRequestObject.get("manual_description");
                }
            }
            if (jsonRequestObject.has("volume")) {
                jsonRequest.put("volume", jsonRequestObject.get("volume"));
            }
            if (jsonRequestObject.has("issue")) {
                jsonRequest.put("issue", jsonRequestObject.get("issue"));
            }
            if (jsonRequestObject.has("part")) {
                jsonRequest.put("part", jsonRequestObject.get("part"));
            }
            if (jsonRequestObject.has("date_of_publication")) {
                jsonRequest.put("date_of_publication", jsonRequestObject.get("date_of_publication"));
            }

        }
        comment += " {Source Request " + itemData.getSourceInstitution()+"-" + jsonRequestObject.get("request_id") + "-"  +jsonRequestObject.get("user_primary_id") +"}";
        jsonRequest.put("comment", comment);
        HttpResponse requestResponse = RequestApi.createBibRequest(mmsId, baseUrl, remoteStorageApikey,
                jsonRequest.toString(), userId);
        return requestResponse;

    }

    private static JSONObject getRequestObj() {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("request_type", "HOLD");
        JSONObject jsonRequestSubType = new JSONObject();
        jsonRequestSubType.put("value", "PATRON_PHYSICAL");
        jsonRequestSubType.put("desc", "Patron physical item request");
        jsonRequest.put("request_sub_type", jsonRequestSubType);
        jsonRequest.put("pickup_location_type", "USER_HOME_ADDRESS");
        jsonRequest.put("task_name", "Pickup From Shelf");

        return jsonRequest;
    }

    private static String getUserIdByIns(ItemData itemData) {
        return itemData.getInstitution() + "-" + itemData.getLibrary();
    }

    public static void scanINSRequest(JSONObject jsonItemObject, ItemData requestData) {
        logger.debug(
                "return request : " + requestData.getInstitution() + " Item. Barcode : " + requestData.getBarcode());
        String library = jsonItemObject.getJSONObject("item_data").getJSONObject("library").getString("value");
        if (jsonItemObject.getJSONObject("holding_data").getBoolean("in_temp_location")) {
            library = jsonItemObject.getJSONObject("holding_data").getJSONObject("temp_library").getString("value");
        }
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();

        String institutionApiKey = null;
        String circ_desk = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(requestData.getInstitution())) {
                institutionApiKey = inst.getString("apikey");
                JSONArray libraries = inst.getJSONArray("libraries");
                for (int j = 0; j < libraries.length(); j++) {
                    if (library.equals(libraries.getJSONObject(j).get("code").toString())) {
                        circ_desk = libraries.getJSONObject(j).getString("circ_desc");
                    }
                }
                break;
            }
        }
        String mmsId = jsonItemObject.getJSONObject("bib_data").getString("mms_id");
        String holdingId = jsonItemObject.getJSONObject("holding_data").getString("holding_id");
        String itemPid = jsonItemObject.getJSONObject("item_data").getString("pid");

        HttpResponse itemResponce = ItemApi.scanIn(mmsId, holdingId, itemPid, "scan", baseUrl, library, circ_desk,
                "true", institutionApiKey);

        if (itemResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            String message = "Can't scan in institution : " + requestData.getInstitution() + " item. Barcode : "
                    + requestData.getBarcode();
            logger.error(message);
            ReportUtil.getInstance().appendReport("LoanReturnedHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
        } else {
            logger.debug("Success scan in institution : " + requestData.getInstitution() + " item. Barcode : "
                    + requestData.getBarcode());
        }

    }

    public static void createSCFLoan(ItemData itemData, String itemPid) {

        logger.debug("create SCF Loan. Item Pid: " + itemPid);
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();
        String loanCircDesc = props.get("remote_storage_circ_desc").toString();
        String loanLibrary = props.get("remote_storage_holding_library").toString();
        String userId = getUserIdByIns(itemData);
        JSONObject jsonLoan = new JSONObject();
        JSONObject jsonCircDesk = new JSONObject();
        jsonCircDesk.put("value", loanCircDesc);
        jsonLoan.put("circ_desk", jsonCircDesk);
        JSONObject jsonLibrary = new JSONObject();
        jsonLibrary.put("value", loanLibrary);
        jsonLoan.put("library", jsonLibrary);

        HttpResponse requestResponse = LoanApi.createLoan(userId, itemPid, baseUrl, remoteStorageApikey,
                jsonLoan.toString());
        if (requestResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't create SCF loan. Item Pid : " + itemPid);
        }
    }

    public static JSONObject createSCFBibByINST(ItemData itemData, String body) {
        logger.debug("create SCF Bib. Barcode : " + itemData.getBarcode());
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();

        HttpResponse bibResponse = BibApi.createBibBySId("false", "true", body, baseUrl, remoteStorageApikey);
        if (bibResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't create SCF Bib. Barcode : " + itemData.getBarcode());
            return null;
        }
        JSONObject jsonNewBibObject = new JSONObject(bibResponse.getBody());
        return jsonNewBibObject;
    }

    public static JSONObject getItemRequests(JSONObject jsonItemObject, ItemData itemData) {
        logger.debug(
                "retrieve item requests : " + itemData.getInstitution() + " Item. Barcode : " + itemData.getBarcode());
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(itemData.getInstitution())) {
                institutionApiKey = inst.getString("apikey");
                break;
            }
        }

        String mmsId = jsonItemObject.getJSONObject("bib_data").getString("mms_id");
        String holdingId = jsonItemObject.getJSONObject("holding_data").getString("holding_id");
        String itemPid = jsonItemObject.getJSONObject("item_data").getString("pid");

        HttpResponse requestsResponce = RequestApi.getRequests(mmsId, holdingId, itemPid, baseUrl, institutionApiKey);
        if (requestsResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't get SCF Item Requests. Barcode : " + itemData.getBarcode());
            return null;
        }
        JSONObject jsonRequestsObject = new JSONObject(requestsResponce.getBody());
        return jsonRequestsObject;
    }

    public static HttpResponse cancelItemRequest(JSONObject jsonItemObject, ItemData itemData, String requestId,
            String cancellationNote) {
        logger.debug("cancel item requests from SCF Item. Barcode : " + itemData.getBarcode());

        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(itemData.getInstitution())) {
                institutionApiKey = inst.getString("apikey");
                break;
            }
        }
        String mmsId = jsonItemObject.getJSONObject("bib_data").getString("mms_id");
        String holdingId = jsonItemObject.getJSONObject("holding_data").getString("holding_id");
        String itemPid = jsonItemObject.getJSONObject("item_data").getString("pid");
        HttpResponse requestResponce = RequestApi.cancelRequest(mmsId, holdingId, itemPid, requestId,
                "CannotBeFulfilled", cancellationNote, baseUrl, institutionApiKey);
        if (requestResponce.getResponseCode() == HttpsURLConnection.HTTP_NO_CONTENT) {
            logger.info("successfully canceled SCF Item Requests. Barcode : " + itemData.getBarcode());
        } else {
            logger.warn("Can't cancel SCF Item Requests. Barcode : " + itemData.getBarcode());
        }
        return requestResponce;
    }

    public static JSONObject getINSRequest(ItemData itemData) {
        logger.debug("get institution : " + itemData.getSourceInstitution() + " Request. Request id : "
                + itemData.getRequestId());

        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(itemData.getSourceInstitution())) {
                institutionApiKey = inst.getString("apikey");
                break;
            }
        }

        HttpResponse requestsResponce = RequestApi.getRequest(itemData.getMmsId(), itemData.getRequestId(), baseUrl,
                institutionApiKey);
        if (requestsResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't get institution : " + itemData.getInstitution() + " Requests. Barcode : "
                    + itemData.getBarcode());
            return null;
        }
        JSONObject jsonRequestsObject = new JSONObject(requestsResponce.getBody());
        return jsonRequestsObject;
    }

    public static JSONObject createSCFUser(ItemData requestData, String userId, String userSourceInstitution) {
        logger.debug("create SCF User. User Id : " + userId + " User Source Institution : " + userSourceInstitution);
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        HttpResponse userResponce = UserApi.createLinkedUser(userId, userSourceInstitution, "<user></user>", baseUrl,
                remoteStorageApikey);
        if (userResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't create SCF User. User id : " + userId);
            return null;
        }
        try {
            return new JSONObject(userResponce.getBody());
        } catch (Exception e) {
            return null;
        }

    }

    private static JSONObject getDigitizationRequestObj() {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("request_type", "DIGITIZATION");
        JSONObject jsonRequestSubType = new JSONObject();
        jsonRequestSubType.put("value", "PHYSICAL_TO_DIGITIZATION");
        jsonRequestSubType.put("desc", "Patron digitization request");
        jsonRequest.put("request_sub_type", jsonRequestSubType);
        JSONObject jsonTargetDestination = new JSONObject();
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String digitizationDepartment = props.get("remote_storage_digitization_department").toString();
        jsonTargetDestination.put("value", digitizationDepartment);
        jsonTargetDestination.put("desc", "Digitization Department For Institution");
        jsonRequest.put("target_destination", jsonTargetDestination);
        return jsonRequest;
    }

    public static JSONObject createSCFDigitizationRequest(JSONObject jsonUserObject, JSONObject jsonRequestObject,
            JSONObject jsonItemObject, ItemData requestData) {

        logger.debug("create SCF Digitization Request. Barcode: "
                + jsonItemObject.getJSONObject("item_data").getString("barcode"));
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();

        String primaryId = jsonUserObject.getString("primary_id");
        String mmsId = jsonItemObject.getJSONObject("bib_data").getString("mms_id");
        String holdingId = jsonItemObject.getJSONObject("holding_data").getString("holding_id");
        String itemPid = jsonItemObject.getJSONObject("item_data").getString("pid");

        JSONObject jsonRequest = getDigitizationRequestObj();
        jsonRequest.put("user_primary_id", primaryId);
        if (jsonRequestObject.has("copyrights_declaration_signed_by_patron")) {
            jsonRequest.put("copyrights_declaration_signed_by_patron",
                    jsonRequestObject.get("copyrights_declaration_signed_by_patron"));
        }
        if (jsonRequestObject.has("description")) {
        	jsonRequest.put("description", jsonRequestObject.get("description"));
        }
        String comment = "";
        if (jsonRequestObject.has("comment") && !jsonRequestObject.get("comment").equals(null)) {
            comment = jsonRequestObject.getString("comment") + " ";
        }
        comment += "The inventory for this request should come from " + requestData.getSourceInstitution()+". ";
        if(requestData.getPatron() != null) {
        	comment += requestData.getPatron().toString();
        }
        jsonRequest.put("partial_digitization", jsonRequestObject.get("partial_digitization"));
        if (jsonRequestObject.has("required_pages_range")) {
            jsonRequest.put("required_pages_range", jsonRequestObject.get("required_pages_range"));
        }
        if (jsonRequestObject.has("full_chapter")) {
            jsonRequest.put("full_chapter", jsonRequestObject.get("full_chapter"));
        }
        if (jsonRequestObject.has("chapter_or_article_title")) {
            jsonRequest.put("chapter_or_article_title", jsonRequestObject.get("chapter_or_article_title"));
        }
        if (jsonRequestObject.has("chapter_or_article_author")) {
            jsonRequest.put("chapter_or_article_author", jsonRequestObject.get("chapter_or_article_author"));
        }
        if (jsonRequestObject.has("manual_description") && !jsonRequestObject.get("manual_description").equals(null)
                && !jsonRequestObject.get("manual_description").equals("")) {
            jsonRequest.put("manual_description", jsonRequestObject.get("manual_description"));
            comment += " " + jsonRequestObject.get("manual_description");
        }
        if (jsonRequestObject.has("last_interest_date")) {
            jsonRequest.put("last_interest_date", jsonRequestObject.get("last_interest_date"));
        }
        if (jsonRequestObject.has("volume")) {
            jsonRequest.put("volume", jsonRequestObject.get("volume"));
        }
        if (jsonRequestObject.has("issue")) {
            jsonRequest.put("issue", jsonRequestObject.get("issue"));
        }
        if (jsonRequestObject.has("part")) {
            jsonRequest.put("part", jsonRequestObject.get("part"));
        }
        if (jsonRequestObject.has("date_of_publication")) {
            jsonRequest.put("date_of_publication", jsonRequestObject.get("date_of_publication"));
        }
        jsonRequest.put("comment", comment);
        HttpResponse requestResponse = RequestApi.createRequest(mmsId, holdingId, itemPid, baseUrl, remoteStorageApikey,
                jsonRequest.toString(), primaryId);
        if (requestResponse.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            String message = "Can't create SCF request. Item Pid : " + itemPid + "." + requestResponse.getBody();
            logger.error(message);
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            return null;
        }
        JSONObject jsonRequestsObject = null;
        try {
            jsonRequestsObject = new JSONObject(requestResponse.getBody());
        } catch (Exception e) {
            String message = "Can't create SCF request. Item Pid : " + itemPid + "." + requestResponse.getBody();
            logger.error(message);
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
        }
        return jsonRequestsObject;
    }

    public static void cancelTitleRequest(ItemData requestData) {
        logger.debug("get institution : " + requestData.getInstitution() + " Request. Request id : "
                + requestData.getRequestId());

        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(requestData.getInstitution())) {
                institutionApiKey = inst.getString("apikey");
                break;
            }
        }

        HttpResponse requestsResponce = RequestApi.cancleTitleRequest(requestData.getMmsId(),
                requestData.getRequestId(), "RequestSwitched", "Request will be handled by the remote storage.",
                "false", baseUrl, institutionApiKey);
        if (requestsResponce.getResponseCode() == HttpsURLConnection.HTTP_NO_CONTENT) {
            logger.info("successfully canceled Title Requests. Mms Id : " + requestData.getMmsId() + " Institution : "
                    + requestData.getInstitution());
        } else {
            String message = "Can't cancel Title Requests. Mms Id : " + requestData.getMmsId() + " Institution : "
                    + requestData.getInstitution();
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            logger.error(message);
        }
    }

    public static JSONObject getSCFUser(ItemData requestData, String userId, String institution) {
        logger.debug("get SCF User. User Id : " + userId);
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        HttpResponse userResponce = UserApi.getLinkedUser(userId, institution, baseUrl, remoteStorageApikey);
        if (userResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't get SCF User. User id : " + userId);
            return null;
        }
        try {
            return new JSONObject(userResponce.getBody());
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject getINSUser(ItemData requestData, String userId, String userIdType, String institution) {
        logger.info("get SCF User. User Id : " + userId + " Institution code : " + institution);
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(institution)) {
                institutionApiKey = inst.getString("apikey");
                break;
            }
        }
        HttpResponse userResponce = UserApi.getUser(userId, userIdType, baseUrl, institutionApiKey);
        if (userResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't get institutions " + requestData.getInstitution() + " User. User id : " + userId);
            return null;
        }
        try {
            return new JSONObject(userResponce.getBody());
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject createSCFDigitizationUserRequest(JSONObject jsonUserObject, JSONObject jsonRequestObject,
            JSONObject jsonBibObject, ItemData requestData) {
        logger.debug("create SCF Digitization Request. User Id: " + jsonUserObject.getString("primary_id"));
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        String baseUrl = props.get("gateway").toString();

        String primaryId = jsonUserObject.getString("primary_id");
        String mmsId = null;
        try {
            mmsId = jsonBibObject.getJSONArray("bib").getJSONObject(0).getString("mms_id");
        } catch (Exception e) {
            mmsId = jsonBibObject.getString("mms_id");
        }

        JSONObject jsonRequest = getDigitizationRequestObj();
        jsonRequest.put("user_primary_id", primaryId);
        if (jsonRequestObject.has("copyrights_declaration_signed_by_patron")) {
            jsonRequest.put("copyrights_declaration_signed_by_patron",
                    jsonRequestObject.get("copyrights_declaration_signed_by_patron"));
        }
        jsonRequest.put("description", jsonRequestObject.get("description"));
        String comment = "";
        if (jsonRequestObject.has("comment") && !jsonRequestObject.get("comment").equals(null)) {
            comment = jsonRequestObject.getString("comment") + " ";
        }
        comment += "The inventory for this request should come from " + requestData.getSourceInstitution()+". ";
        if(requestData.getPatron() != null) {
        	comment += requestData.getPatron().toString();
        }
        jsonRequest.put("partial_digitization", jsonRequestObject.get("partial_digitization"));
        if (jsonRequestObject.has("required_pages_range")) {
            jsonRequest.put("required_pages_range", jsonRequestObject.get("required_pages_range"));
        }
        if (jsonRequestObject.has("full_chapter")) {
            jsonRequest.put("full_chapter", jsonRequestObject.get("full_chapter"));
        }
        if (jsonRequestObject.has("chapter_or_article_title")) {
            jsonRequest.put("chapter_or_article_title", jsonRequestObject.get("chapter_or_article_title"));
        }
        if (jsonRequestObject.has("chapter_or_article_author")) {
            jsonRequest.put("chapter_or_article_author", jsonRequestObject.get("chapter_or_article_author"));
        }
        if (jsonRequestObject.has("manual_description") && !jsonRequestObject.get("manual_description").equals(null)
                && !jsonRequestObject.get("manual_description").equals("")) {
            jsonRequest.put("manual_description", jsonRequestObject.get("manual_description"));
            comment += " " + jsonRequestObject.get("manual_description");
        }
        if (jsonRequestObject.has("last_interest_date")) {
            jsonRequest.put("last_interest_date", jsonRequestObject.get("last_interest_date"));
        }
        if (jsonRequestObject.has("volume")) {
            jsonRequest.put("volume", jsonRequestObject.get("volume"));
        }
        if (jsonRequestObject.has("issue")) {
            jsonRequest.put("issue", jsonRequestObject.get("issue"));
        }
        if (jsonRequestObject.has("part")) {
            jsonRequest.put("part", jsonRequestObject.get("part"));
        }
        if (jsonRequestObject.has("date_of_publication")) {
            jsonRequest.put("date_of_publication", jsonRequestObject.get("date_of_publication"));
        }
        jsonRequest.put("comment", comment);
        HttpResponse requestResponse = RequestApi.createBibRequest(mmsId, baseUrl, remoteStorageApikey,
                jsonRequest.toString(), primaryId);
        if (requestResponse.getResponseCode() != HttpsURLConnection.HTTP_OK) {
            String message = "Can't create SCF request. Mms Id : " + mmsId + " User Id : " + primaryId + "."
                    + requestResponse.getBody();
            logger.error(message);
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            return null;
        }
        JSONObject jsonRequestsObject = null;
        try {
            jsonRequestsObject = new JSONObject(requestResponse.getBody());
        } catch (Exception e) {
            String message = "Can't create SCF request. Mms Id : " + mmsId + " User Id : " + primaryId + "."
                    + requestResponse.getBody();
            logger.error(message);
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getInstitution(), message);
            return null;
        }
        return jsonRequestsObject;
    }

    public static JSONObject getINSUserRequest(ItemData requestData) {
        logger.debug("get institution : " + requestData.getSourceInstitution() + " User Request. Request id : "
                + requestData.getRequestId());

        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(requestData.getSourceInstitution())) {
                institutionApiKey = inst.getString("apikey");
                break;
            }
        }
        HttpResponse requestsResponce = UserApi.getUserRequest(requestData.getUserId(), requestData.getRequestId(),
                baseUrl, institutionApiKey);
        if (requestsResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't get institution User Requests. User Id : " + requestData.getUserId());
            return null;
        }
        JSONObject jsonRequestsObject = new JSONObject(requestsResponce.getBody());
        return jsonRequestsObject;
    }

    public static String getDefaultLibrary(String institution) {
        logger.info("getting a default library for institution : " + institution);
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(institution)) {
                return inst.has("default_library") ? inst.getString("default_library") : null;
            }
        }
        return null;
    }

    public static void cancelRequest(ItemData requestData) {
        logger.debug("cancel institution : " + requestData.getSourceInstitution() + " Request. Request id : "
                + requestData.getRequestId());

        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
            JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
            if (inst.get("code").toString().equals(requestData.getSourceInstitution())) {
                institutionApiKey = inst.getString("apikey");
                break;
            }
        }
        HttpResponse requestsResponce = null;
        if (requestData.getMmsId() != null) {
            requestsResponce = RequestApi.cancleTitleRequest(requestData.getMmsId(), requestData.getRequestId(),
                    "CannotBeFulfilled", "Request could not be fulfilled by the SCF.", "true", baseUrl,
                    institutionApiKey);
        } else if (requestData.getUserId() != null) {
            requestsResponce = RequestApi.cancleUserRequest(requestData.getUserId(), requestData.getRequestId(),
                    "CannotBeFulfilled", "Request could not be fulfilled by the SCF.", "true", baseUrl,
                    institutionApiKey);
        }
        if (requestsResponce != null && requestsResponce.getResponseCode() == HttpsURLConnection.HTTP_NO_CONTENT) {
            logger.info("successfully canceled request. Request id : " + requestData.getRequestId() + " Institution : "
                    + requestData.getSourceInstitution());
        } else {
            String message = "Can't cancel requests. Request. Request id : " + requestData.getRequestId()
                    + " Institution : " + requestData.getSourceInstitution();
            ReportUtil.getInstance().appendReport("RequestHandler", requestData.getBarcode(),
                    requestData.getSourceInstitution(), message);
            logger.error(message);
        }

    }

    public static void refreshLinkedUser(ItemData requestData, String userId) {
        logger.debug("refresh SCF Linked User. User Id : " + userId);
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String remoteStorageApikey = props.get("remote_storage_apikey").toString();
        HttpResponse userResponce = UserApi.refreshLinkedUser(userId, baseUrl, remoteStorageApikey);
        if (userResponce.getResponseCode() == HttpsURLConnection.HTTP_BAD_REQUEST) {
            logger.warn("Can't refresh SCF User. User id : " + userId);
        }
        if (userResponce != null && userResponce.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            logger.info("successfully refreshed user. User id : " + userId);
        }
    }

    private static String getSCFHoldingByBib(String mmsId, String holdingLib, String holdingLoc, String baseUrl,
            String remoteStorageApikey) {
        logger.debug("get SCF Holdings. MMS Id : " + mmsId);
        HttpResponse holdingsResponce = HoldingApi.getHoldings(mmsId, baseUrl, remoteStorageApikey);
        if (holdingsResponce.getResponseCode() != HttpsURLConnection.HTTP_OK) {
            logger.warn("Can't get SCF Holdings - " + holdingsResponce.getBody());
            return null;
        }
        JSONObject jsonHoldingObject = new JSONObject(holdingsResponce.getBody());
        JSONArray holdings = jsonHoldingObject.getJSONArray("holding");

        for (int i = 0; i < holdings.length(); i++) {
            try {
                JSONObject holding = holdings.getJSONObject(i);

                if (holding.getJSONObject("library").getString("value").equals(holdingLib)
                        && holding.getJSONObject("location").getString("value").equals(holdingLoc)) {
                    return holding.getString("holding_id");
                }
            } catch (Exception e) {
                logger.info("Failed getting holding information error: " + e.getMessage() + " Holdings Responce: "
                        + holdingsResponce.getBody());
            }
        }
        
        if(holdings.length() > 0) {
        	//get the first location
        	JSONObject holding = holdings.getJSONObject(0);
        	return holding.getString("holding_id");
        }
        return null;
    }

}
