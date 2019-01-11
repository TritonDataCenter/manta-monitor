/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.servlets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Class that handles http request for /monitor endpoint, to present the elapsed time of operation.
 */
@Singleton
public class MantaMonitorServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(MantaMonitorServlet.class);
    private static final long serialVersionUID = 5240172014702531193L;
    private final Map<String, AtomicLong> clientStats;

    @Inject
    public MantaMonitorServlet(@Named("SharedStats") final Map<String, AtomicLong> clientStats) {
        this.clientStats = clientStats;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        if (clientStats.size() > 0) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            clientStats.forEach((key, value) -> {
                try {
                    response.getWriter().println("Elapsed time for " + key + " : " + value + " milliseconds");
                } catch (IOException ie) {
                    String message = String.format("Error in writing the response for the request: %s", request.getRequestURI());
                    LOG.error(message, ie);
                }
            });
        } else {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("Waiting to collect metrics");
        }
    }
}
