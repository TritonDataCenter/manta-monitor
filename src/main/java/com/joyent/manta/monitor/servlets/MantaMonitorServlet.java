package com.joyent.manta.monitor.servlets;

import com.joyent.manta.monitor.MBeanServerOperationException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


@Singleton
public class MantaMonitorServlet extends HttpServlet {
    private final Map<String, AtomicLong> clientStats;

    @Inject
    public MantaMonitorServlet (@Named("SharedStats") Map<String, AtomicLong> clientStats) {
        this.clientStats = clientStats;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if(clientStats.size() > 0) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            clientStats.forEach((key, value) -> {
                try {
                    response.getWriter().println("Elapsed time for "+key + " : " +value+" milliseconds");
                } catch (IOException ie) {
                    String message = "Error in writing the response";
                    MBeanServerOperationException mBeanServerOperationException = new MBeanServerOperationException(message, ie);
                    mBeanServerOperationException.setContextValue("requestURI", request.getRequestURI());
                    throw mBeanServerOperationException;
                }
            });
        } else {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("Waiting to collect metrics");
        }
    }
}