package com.joyent.manta.monitor.jetty;

import io.prometheus.client.Counter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class MantaMonitorServlet extends HttpServlet {

    private final Map<String, AtomicLong> clientStats;
    private final Counter requests;

    @Inject
    public MantaMonitorServlet (@Named("SharedStats") Map<String, AtomicLong> clientStats,
                                @Named("SharedCounter") Counter requests) {
        this.clientStats = clientStats;
        this.requests = requests;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if(clientStats.size() > 0) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            clientStats.forEach((key, value) -> {
                try{
                    response.getWriter().println("Elapsed time for "+key + " : " +value+" milliseconds");
                    requests.labels("get").inc();

                }catch (IOException ie) {
                    System.out.println(ie);
                }

            });
        } else {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Waiting to collect metrics");
        }

    }

//    public void setContext(MantaOperationContext context) {
//        this.context = context;
//    }


}
