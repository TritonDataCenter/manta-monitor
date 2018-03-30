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
import org.apache.commons.lang3.StringUtils;
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
    private final InstanceMetadata metadata;

    @Inject
    public HoneyBadgerRequestFactory(final MantaClient client,
                                     final InstanceMetadata metadata,
                                     final io.honeybadger.reporter.config.ConfigContext hbConfig) {
        this.mantaConfig = client.getContext();
        this.metadata = metadata;
        this.hbConfig = hbConfig;
    }

    public Request build(@Nullable final String path) {
        return build(path, null, null);
    }

    public Request build(@Nullable final String path,
                         @Nullable final ExceptionContext exceptionContext) {
        return build(path, null, exceptionContext);
    }

    public Request build(@Nullable final String path,
                         @Nullable final MantaHttpHeaders headers,
                         @Nullable final ExceptionContext exceptionContext) {
        final URI uri;

        if (path == null) {
            uri = null;
        } else {
            uri = buildURI(path).normalize();
        }

        return build(uri, headers, exceptionContext);
    }

    public Request build(@Nullable final URI uri,
                         @Nullable final MantaHttpHeaders headers,
                         @Nullable final ExceptionContext exceptionContext) {
        final String uriText;
        if (uri != null) {
            uriText = uri.normalize().toASCIIString();
        } else {
            uriText = null;
        }

        final Params params = new Params(hbConfig.getExcludedParams());
        final Session session = new Session();
        final CgiData cgiData = new CgiData();
        final Context context;

        if (headers != null) {
            Map<String, Object> missing = cgiData.addFromHttpHeaders(headers, Object::toString);
            context = buildContext(missing, exceptionContext);
        } else {
            context = buildContext(null, exceptionContext);
        }

        return new Request(context, uriText, params, session, cgiData);
    }

    private URI buildURI(final String path) {
        Objects.requireNonNull(path);

        final String urlBase = mantaConfig.getMantaURL();
        final String mantaUrlWithoutTrailingSlashes = StringUtils.removeEnd(
                urlBase, MantaClient.SEPARATOR);
        final String uriString = mantaUrlWithoutTrailingSlashes
                + MantaClient.SEPARATOR + path;

        return URI.create(uriString);
    }

    private Context buildContext(@Nullable final Map<String, ?> additionalContext,
                                 @Nullable final ExceptionContext exceptionContext) {
        final Context context = new Context().setUsername(mantaConfig.getMantaUser());

        if (metadata != null) {
            for (Map.Entry<String, String> entry : metadata.asMap().entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }

        if (additionalContext != null) {
            for (Map.Entry<String, ?> entry : additionalContext.entrySet()) {
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
