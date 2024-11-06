package com.exlibris.configuration;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

@WebServlet("/configuration")
public class ConfigurationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    final private static Logger logger = Logger.getLogger(ConfigurationServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ConfigurationHandler.updateInstance();
        logger.info("End Update Configuration");
        resp.getWriter().write("End Update Configuration");
    }
}
