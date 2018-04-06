/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.common.collect.ImmutableSet;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.http.MantaHttpHeaders;
import io.honeybadger.reporter.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

    public Request build(@Nullable final String path,
                         @Nullable final MantaHttpHeaders headers,
                         @Nullable final Throwable throwable) {
        final URI uri;

        if (path == null) {
            uri = null;
        } else {
            uri = buildURI(path).normalize();
        }

        return build(uri, headers, throwable);
    }

    public Request build(@Nullable final URI uri,
                         @Nullable final MantaHttpHeaders headers,
                         @Nullable final Throwable throwable) {
        final String uriText;
        if (uri != null) {
            uriText = uri.normalize().toASCIIString();
        } else {
            uriText = null;
        }

        final Params params = new Params(hbConfig.getExcludedParams());
        final Session session = new Session();
        final CgiData cgiData = new CgiData();

        if (uri != null) {
            cgiData.setServerName(uri.getHost());

            if (uri.getPort() > 0) {
                cgiData.setServerPort(uri.getPort());
            } else if (uri.getScheme().equals("http")) {
                cgiData.setServerPort(80);
            } else if (uri.getScheme().equals("https")) {
                cgiData.setServerPort(443);
            }
        }
        final Context context;

        if (headers != null) {
            Map<String, Object> missing = cgiData.addFromHttpHeaders(headers, Object::toString);

            context = buildContext(missing, throwable);
        } else {
            context = buildContext(null, throwable);
        }

        final String requestMethod = context.get("requestMethod");

        if (isNotBlank(requestMethod)) {
            cgiData.setRequestMethod(requestMethod);
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
                                 @Nullable final Throwable throwable) {
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

        for (ExceptionContext ec : aggregateAllExceptionContextsWithinCauses(throwable)) {
            for (Pair<String, Object> pair : ec.getContextEntries()) {
                context.put(pair.getKey(), Objects.toString(pair.getValue()));
            }
        }

        return context;
    }

    /**
     * Walks through each exception's causes and determines if it is an instance
     * of {@link ExceptionContext}. If it is, it is then returned as a element
     * in a set.
     *
     * @param throwable root exception context to parse for causes
     * @return a set of all causes with contexts in the order of root to highest
     */
    private static Set<ExceptionContext> aggregateAllExceptionContextsWithinCauses(
            final Throwable throwable) {
        if (throwable == null) {
            return Collections.emptySet();
        }

        final List<Throwable> causes = ExceptionUtils.getThrowableList(throwable);
        Collections.reverse(causes);

        final ImmutableSet.Builder<ExceptionContext> contexted = new ImmutableSet.Builder<>();
        for (Throwable t : causes) {
            if (t instanceof ExceptionContext) {
                contexted.add((ExceptionContext) t);
            }
        }

        return contexted.build();
    }
}
