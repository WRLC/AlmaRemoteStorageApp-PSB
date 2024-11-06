package com.exlibris.restapis;

import org.apache.log4j.Logger;

public class LoanApi {

    final private static Logger logger = Logger.getLogger(LoanApi.class);

    public static HttpResponse createLoan(String userId, String itemPid, String baseUrl, String apiKey, String body) {
        logger.info("Starting to handle creating Loan: " + body + ".");
        logger.info("Item Pid: " + itemPid + " - calling POST");

        String url = baseUrl + "/almaws/v1/users/" + userId + "/loans?item_pid=" + itemPid + "&apikey=" + apiKey;

        HttpResponse loanResponse = AlmaRestUtil.sendHttpReq(url, "POST", body);
        return loanResponse;
    }
}
