/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.http.MantaHttpHeaders;
import io.honeybadger.reporter.dto.CgiData;
import io.honeybadger.reporter.dto.Context;
import io.honeybadger.reporter.dto.Params;
import io.honeybadger.reporter.dto.Request;
import io.honeybadger.reporter.dto.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

public class HoneyBadgerRequestFactory {
    private final com.joyent.manta.config.ConfigContext mantaConfig;
    private final io.honeybadger.reporter.config.ConfigContext hbConfig;

    @Inject
    public HoneyBadgerRequestFactory(final MantaClient client,
                                     final io.honeybadger.reporter.config.ConfigContext hbConfig) {
        this.mantaConfig = client.getContext();
        this.hbConfig = hbConfig;
    }

    public Request build(final String path) {
        return build(path, null, null);
    }

    public Request build(final String path, @Nullable final ExceptionContext exceptionContext) {
        return build(path, null, exceptionContext);
    }

    public Request build(final String path,
                         @Nullable final MantaHttpHeaders headers,
                         @Nullable final ExceptionContext exceptionContext) {
        final URI uri = buildURI(path);
        final Params params = new Params(hbConfig.getExcludedParams());
        final Session session = new Session();
        final CgiData cgiData = new CgiData();
        final Context context;

        if (headers != null) {
            Map<String, Object> missing = cgiData.addFromHttpHeaders(headers, Object::toString);
            context = buildContext(missing, exceptionContext);
        } else {
            context = buildContext(null, null);
        }

        return new Request(context, uri.toASCIIString(), params, session, cgiData);
    }

    URI buildURI(final String path) {
        Objects.requireNonNull(path);

        final String urlBase = mantaConfig.getMantaURL();
        final String mantaUrlWithoutTrailingSlashes = StringUtils.removeEnd(
                urlBase, MantaClient.SEPARATOR);
        final String uriString = mantaUrlWithoutTrailingSlashes
                + MantaClient.SEPARATOR + path;

        return URI.create(uriString);
    }

    Context buildContext(@Nullable final Map<String, Object> additionalContext,
                         @Nullable final ExceptionContext exceptionContext) {
        final Context context = new Context().setUsername(mantaConfig.getMantaUser());

        if (additionalContext != null) {
            for (Map.Entry<String, Object> entry : additionalContext.entrySet()) {
                context.put(entry.getKey(), Objects.toString(entry.getValue()));
            }
        }

        if (exceptionContext != null && exceptionContext.getContextEntries() != null) {
            for (Pair<String, Object> pair : exceptionContext.getContextEntries()) {
                context.put(pair.getKey(), Objects.toString(pair.getValue()));
            }
        }

        return context;
    }
}
