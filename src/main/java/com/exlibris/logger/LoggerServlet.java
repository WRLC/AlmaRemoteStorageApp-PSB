package com.exlibris.logger;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

@WebServlet("/logger")
public class LoggerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    final private static Logger logger = Logger.getLogger(LoggerServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String level = req.getParameter("level");
        LogManager.getRootLogger().setLevel(Level.toLevel(level.toUpperCase()));
        logger.info("level is :" + level.toUpperCase());
        resp.getWriter().write("level is :" + level.toUpperCase());

    }
}
