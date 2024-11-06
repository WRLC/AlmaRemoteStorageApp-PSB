package com.exlibris.library;

import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.exlibris.configuration.ConfigurationHandler;
import com.exlibris.restapis.ConfApi;
import com.exlibris.restapis.HttpResponse;

public class LibraryHandler {

    final private static Logger logger = Logger.getLogger(LibraryHandler.class);

    private static HashMap<String, HashMap<String, String>> libraryMap = new HashMap<String, HashMap<String, String>>();

    public static String getLibraryCode(String institution, String libraryName) {
        if (libraryMap.get(institution) == null) {
            libraryMap.put(institution, getInstitutionMap(institution));
        }
        if (libraryMap.get(institution) != null) {
            return libraryMap.get(institution).get(libraryName);
        }
        return null;

    }

    private static HashMap<String, String> getInstitutionMap(String institution) {
        logger.debug("get Institution Librarys. Institution : " + institution);
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();
        String baseUrl = props.get("gateway").toString();
        String institutionApiKey = null;
        if (props.get("remote_storage_inst").equals(institution)) {
            institutionApiKey = props.get("remote_storage_apikey").toString();
        } else {
            for (int i = 0; i < props.getJSONArray("institutions").length(); i++) {
                JSONObject inst = props.getJSONArray("institutions").getJSONObject(i);
                if (inst.get("code").toString().equals(institution)) {
                    institutionApiKey = inst.getString("apikey");
                    break;
                }
            }
        }
        if (institutionApiKey == null) {
            logger.warn("Can't get Institution Librarys - No Api Key. Institution : " + institution);
            return null;
        }
        HttpResponse librariesResponse = ConfApi.retrieveLibraries(baseUrl, institutionApiKey);

        if (librariesResponse.getResponseCode() < HttpsURLConnection.HTTP_OK
                || librariesResponse.getResponseCode() >= HttpsURLConnection.HTTP_MOVED_PERM) {
            logger.warn("Can't get Institution Librarys - " + librariesResponse.getBody() + ". Institution : "
                    + institution);
            return null;
        }
        JSONObject librariesJson = new JSONObject(librariesResponse.getBody());
        HashMap<String, String> libraryMap = new HashMap<String, String>();
        for (int i = 0; i < librariesJson.getJSONArray("library").length(); i++) {
            JSONObject library = librariesJson.getJSONArray("library").getJSONObject(i);
            try {
                libraryMap.put(library.getString("name"), library.getString("code"));
            } catch (JSONException e) {
            }
        }
        return libraryMap;

    }

}
