package com.joyent.manta.monitor;

import com.google.common.collect.ImmutableMap;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.http.MantaHttpHeaders;
import io.honeybadger.reporter.dto.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Class that post processes {@link Throwable} instances so that they are
 * presented in a useful way for Honeybadger reporting.
 */
public class ThrowableProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ThrowableProcessor.class);

    private final HoneyBadgerRequestFactory requestFactory;

    public class ProcessedResults {
        public final Throwable throwable;
        public final Throwable rootCause;
        public final Request request;
        public final List<Throwable> throwableAndCauses;

        public ProcessedResults(final Throwable throwable,
                                final Throwable rootCause,
                                final Request request,
                                final List<Throwable> throwableAndCauses) {
            this.throwable = throwable;
            this.rootCause = rootCause;
            this.request = request;
            this.throwableAndCauses = throwableAndCauses;

        }
    }

    public ThrowableProcessor(final HoneyBadgerRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    public ProcessedResults process(final Throwable inputThrowable) {
        final Throwable throwableAndCauses = unwrapMantaOperationException(inputThrowable);
        final List<Throwable> throwables = ExceptionUtils.getThrowableList(inputThrowable);
        final Map<String, String> aggregatedContext = aggregateAllExceptionContext(throwables);
        final String path;

        if (aggregatedContext.containsKey("path")) {
            path = aggregatedContext.get("path");
        } else if (aggregatedContext.containsKey("requestURL")) {
            path = parsePathFromURL(aggregatedContext.get("requestURL"));
        } else {
            path = null;
        }

        /* We add the message to the context because it will sometimes be
         * rewritten at a later stage of processing and we want to preserve
         * the original contents. */
        if (inputThrowable instanceof ExceptionContext) {
            ExceptionContext exceptionContext = (ExceptionContext) inputThrowable;
            final String message = StringUtils.substringBefore(inputThrowable.getMessage(),
                    "Exception Context:").trim();
            exceptionContext.setContextValue("actualMessage", message);
        }

        final MantaHttpHeaders mantaHeaders = findFirstMantaHeaderObject(throwables);
        final Request request = requestFactory.build(path, mantaHeaders, inputThrowable);
        final Throwable rootCause = rootCause(throwables);
        return new ProcessedResults(throwableAndCauses, rootCause, request, throwables);
    }

    @Nullable
    private static Throwable unwrapMantaOperationException(final Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        final Throwable cause = throwable.getCause();

        if (cause == null) {
            return throwable;
        }

        return cause;
    }

    private static Map<String, String> aggregateAllExceptionContext(final List<Throwable> throwables) {
        if (throwables.isEmpty()) {
            return Collections.emptyMap();
        }

        final List<Throwable> reversed = new ArrayList<>(throwables);
        Collections.reverse(reversed);

        ImmutableMap.Builder<String, String> map = new ImmutableMap.Builder<>();

        for (Throwable t : reversed) {
            if (t instanceof ExceptionContext) {
                final ExceptionContext ec = (ExceptionContext)t;
                for (String key : ec.getContextLabels()) {
                    final List<Object> val = ec.getContextValues(key);
                    final String stringValue = contextValueListToString(val);

                    if (stringValue != null) {
                        map.put(key, stringValue);
                    }
                }
            }
        }

        return map.build();
    }

    private static String contextValueListToString(final List<Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        final Iterator<Object> itr = values.iterator();

        final String first = Objects.toString(itr.next());

        if (!itr.hasNext()) {
            return first;
        }

        final StringBuilder builder = new StringBuilder(first).append(", ");

        while (itr.hasNext()) {
            final String next = Objects.toString(itr.next());
            builder.append(next);

            if (itr.hasNext()) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    @Nullable
    private static String parsePathFromURL(final String requestURL) {
        try {
            final URI uri = URI.create(requestURL);
            final String path = uri.getPath();

            if (isBlank(path)) {
                return null;
            }

            return path;
        } catch (Exception e) {
            String msg = String.format("Error parsing URI: %s", requestURL);
            LOG.error(msg, e);
            return null;
        }
    }

    @Nullable
    private static MantaHttpHeaders findFirstMantaHeaderObject(final List<Throwable> throwables) {
        if (throwables == null || throwables.isEmpty()) {
            return null;
        }

        for (Throwable t : throwables) {
            if (t instanceof MantaClientHttpResponseException) {
                MantaClientHttpResponseException mchre = (MantaClientHttpResponseException)t;

                if (mchre.getHeaders() != null) {
                    return mchre.getHeaders();
                }
            }

        }

        return null;
    }

    @Nullable
    private static Throwable rootCause(final List<Throwable> throwableAndCauses) {
        if (throwableAndCauses == null) {
            return null;
        }

        final int size = throwableAndCauses.size();

        if (size < 2) {
            return null;
        }

        return throwableAndCauses.get(size - 1);
    }
}
