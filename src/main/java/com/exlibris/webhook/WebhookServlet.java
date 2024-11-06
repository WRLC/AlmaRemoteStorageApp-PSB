package com.exlibris.webhook;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.exlibris.configuration.ConfigurationHandler;

/**
 * Servlet implementation class MainServlet
 */
@WebServlet("/webhook")
public class WebhookServlet extends HttpServlet {

    final Charset charSet = Charset.forName("UTF-8");

    private static final long serialVersionUID = 1L;
    final private static Logger logger = Logger.getLogger(WebhookServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String challenge = req.getParameter("challenge");
        logger.info("challenge is :" + challenge);
        resp.setContentType("application/json");
        JSONObject json = new JSONObject();
        json.put("challenge", challenge);
        resp.getWriter().write(json.toString());

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        JSONObject props = ConfigurationHandler.getInstance().getConfiguration();

        String str;
        String body = "";
        while ((str = request.getReader().readLine()) != null) {
            body += str;
        }
        logger.info("message: " + body);
        resp.getWriter().write("Got message");

        // validate secret (if it was defined in conf.json)
        String secret = null;
        try {
            secret = props.get("webhook_secret").toString();
        } catch (JSONException e) {
            logger.debug("webhook_secret not found in conf.json");
        }
        if (secret != null && !secret.isEmpty()) {
            String signature = request.getHeader("X-Exl-Signature");
            logger.info("signature is :" + signature);
            try {
                if (!validateSignature(body, secret, signature)) {
                    resp.getWriter().write("Invalid signature");
                    logger.info("Invalid signature");
                    return;
                }
            } catch (Exception e) {
                logger.debug("could not validate signature");
            }
        }

        final String message = body;
        Runnable runner = new Runnable() {

            public void run() {
                ManageWebHookMessages.getWebhookMessage(message);
            }
        };

        Thread thread = new Thread(runner);
        thread.start();
        logger.info("webhook handler ended");
    }

    private static byte[] hmacSHA256(String data, byte[] key) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes("UTF8"));
    }

    private boolean validateSignature(String message, String secret, String signature) throws Exception {
        String hash = Base64.getEncoder().encodeToString(hmacSHA256(message, secret.getBytes("UTF-8")));
        if (hash.equals(signature)) {
            return true;
        } else {
            logger.info("signature is " + signature + " and hash is " + hash + " not valid");
            return false;
        }
    }
}