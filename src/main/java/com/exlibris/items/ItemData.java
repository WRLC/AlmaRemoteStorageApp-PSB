package com.exlibris.items;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.exlibris.library.LibraryHandler;
import com.exlibris.util.SCFUtil;

public class ItemData {

    final private static Logger logger = Logger.getLogger(ItemData.class);

    final static String BARCODE_SUB_FIELD = "b";
    final static String LIBRARY_SUB_FIELD = "c";
    final static String LOCATION_SUB_FIELD = "l";
    final static String INTERNAL_NOTE_2 = "n";

    private String barcode;
    private String institution;
    private String library;
    private String location;
    private String networkNumber;
    private String mmsId;
    private String description;
    private String note;
    private Record record;
    private String requestType;
    private String requestId;
    private String userId;
    private String sourceInstitution;
    private PatronInfo patron;
    private String requestNote;

    public ItemData(String mmsId, String barcode, String institution, String library, String location,
            String networkNumber, String note) {
        this.mmsId = mmsId;
        this.barcode = barcode;
        this.institution = institution;
        this.library = library;
        this.location = location;
        this.networkNumber = networkNumber;
        this.note = note;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public ItemData(String barcode) {
        this.barcode = barcode;
    }

    public String getMmsId() {
        return mmsId;
    }

    public void setMmsId(String mmsId) {
        this.mmsId = mmsId;
    }

    public String getNote() {
        return note;
    }

    public String getDescription() {
        return description;
    }

    public ItemData(String barcode, String institution, String mmsId, String description, String library, String type) {
        this.barcode = barcode;
        this.institution = institution;
        this.mmsId = mmsId;
        this.description = description;
        this.library = library;
        this.requestType = type;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getInstitution() {
        return institution;
    }

    public String getLibrary() {
        return library;
    }

    public String getLocation() {
        return location;
    }

    public String getNetworkNumber() {
        return networkNumber;
    }

    public void setNetworkNumber(String networkNumber) {
        this.networkNumber = networkNumber;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSourceInstitution() {
        return sourceInstitution;
    }

    public void setSourceInstitution(String sourceInstitution) {
        this.sourceInstitution = sourceInstitution;
    }
    

    public PatronInfo getPatron() {
		return patron;
	}

	public void setPatron(PatronInfo patron) {
		this.patron = patron;
	}
	

	public String getRequestNote() {
		return requestNote;
	}

	public void setRequestNote(String requestNote) {
		this.requestNote = requestNote;
	}

	public static ItemData dataFieldToItemData(String mmsId, DataField dataField, String institution, String nZMmsId) {
        String barcode = dataField.getSubfieldsAsString(BARCODE_SUB_FIELD);
        String library = dataField.getSubfieldsAsString(LIBRARY_SUB_FIELD);
        String location = dataField.getSubfieldsAsString(LOCATION_SUB_FIELD);
        String note = dataField.getSubfieldsAsString(INTERNAL_NOTE_2);
        ItemData itemData = new ItemData(mmsId, barcode, institution, library, location, nZMmsId, note);
        return itemData;
    }

    public static List<ItemData> xmlStringToRequestData(String xml, String institution) throws Exception {
        List<ItemData> requestDataList = new ArrayList<ItemData>();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource src = new InputSource();
        src.setCharacterStream(new StringReader(xml));
        Document doc = builder.parse(src);
        NodeList nl = doc.getDocumentElement().getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Element element = (Element) nl.item(x);
            String barcode = element.getElementsByTagName("xb:barcode").item(0) == null ? null
                    : element.getElementsByTagName("xb:barcode").item(0).getTextContent();
            String description = element.getElementsByTagName("xb:description").item(0) == null ? null
                    : element.getElementsByTagName("xb:description").item(0).getTextContent();
            String mmsId = element.getElementsByTagName("xb:mmsId").item(0) == null ? null
                    : element.getElementsByTagName("xb:mmsId").item(0).getTextContent();
            String type = element.getElementsByTagName("xb:requestType").item(0) == null ? ""
                    : element.getElementsByTagName("xb:requestType").item(0).getTextContent();
            String library = null;
            String libraryInstitution = institution;
            if (element.getElementsByTagName("xb:institutionCode").item(0) != null) {
                libraryInstitution = element.getElementsByTagName("xb:institutionCode").item(0) == null ? null
                        : element.getElementsByTagName("xb:institutionCode").item(0).getTextContent();
                String libraryName = null;
                try {
                    libraryName = ((Element) element.getElementsByTagName("xb:pickup").item(0))
                            .getElementsByTagName("xb:library").item(0).getTextContent();
                    library = LibraryHandler.getLibraryCode(libraryInstitution, libraryName);
                } catch (Exception e) {
                    logger.debug("failed to get library by library name for institution " + libraryInstitution + ","
                            + e.getMessage());
                }
            } else {
                library = element.getElementsByTagName("xb:libraryCode").item(0) == null ? null
                        : element.getElementsByTagName("xb:libraryCode").item(0).getTextContent();
            }
            if (library == null && libraryInstitution.equals(institution) && !type.equals("PHYSICAL_TO_DIGITIZATION")) {
                String libraryName = null;
                try {
                    libraryName = ((Element) element.getElementsByTagName("xb:pickup").item(0))
                            .getElementsByTagName("xb:library").item(0).getTextContent();
                    library = LibraryHandler.getLibraryCode(libraryInstitution, libraryName);
                } catch (Exception e) {
                    logger.debug("failed to get library by library name, " + e.getMessage());
                }
            }
            if (library == null) {
                library = SCFUtil.getDefaultLibrary(libraryInstitution);
            }

            ItemData itemData = new ItemData(barcode, libraryInstitution, mmsId, description, library, type);

            String id = element.getElementsByTagName("xb:requestId").item(0) == null ? ""
                    : element.getElementsByTagName("xb:requestId").item(0).getTextContent();
            itemData.setRequestId(id);
            itemData.setSourceInstitution(institution);

            if (type.equals("PHYSICAL_TO_DIGITIZATION")) {
                itemData.setInstitution(institution);
            }
            if (mmsId == null || "PHYSICAL_TO_DIGITIZATION".equals(type)) {
                try {
                    String userId = ((Element) element.getElementsByTagName("xb:patronInfo").item(0))
                            .getElementsByTagName("xb:patronIdentifier").item(0).getTextContent();
                    itemData.setUserId(userId);
                } catch (Exception e) {
                    logger.debug("can't get patronIdentifier, " + e.getMessage());
                }
            }
            // Include requester information in physical item requests
            if (element.getElementsByTagName("xb:patronInfo").item(0) != null) {
            	Element patronInfoElement = (Element) element.getElementsByTagName("xb:patronInfo").item(0);
            	String name = patronInfoElement.getElementsByTagName("xb:patronName").item(0) == null ? null
                        : patronInfoElement.getElementsByTagName("xb:patronName").item(0).getTextContent();
            	String identifier = patronInfoElement.getElementsByTagName("xb:patronIdentifier").item(0) == null ? null
                        : patronInfoElement.getElementsByTagName("xb:patronIdentifier").item(0).getTextContent();
            	String email = patronInfoElement.getElementsByTagName("xb:patronEmail").item(0) == null ? null
                        : patronInfoElement.getElementsByTagName("xb:patronEmail").item(0).getTextContent();
            	String address = patronInfoElement.getElementsByTagName("xb:patronAddress").item(0) == null ? null
                        : patronInfoElement.getElementsByTagName("xb:patronAddress").item(0).getTextContent();
            	itemData.setPatron(new PatronInfo(name, identifier, email, address));
            }
            
            if(element.getElementsByTagName("xb:requestNote").getLength() > 0) {
            	itemData.setRequestNote(element.getElementsByTagName("xb:requestNote").item(0).getTextContent());
            }
            requestDataList.add(itemData);
        }

        return requestDataList;

    }
}
