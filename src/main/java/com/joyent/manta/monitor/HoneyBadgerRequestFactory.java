package com.joyent.manta.monitor;

import com.joyent.manta.client.MantaClient;
import io.honeybadger.reporter.dto.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HoneyBadgerRequestFactory {
    private static final Session EMPTY_SESSION = new Session();
    private final com.joyent.manta.config.ConfigContext mantaConfig;
    private final io.honeybadger.reporter.config.ConfigContext hbConfig;
    private final Context context;

    private static class PairMapEntry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private V val;

        private PairMapEntry(final K key, final V val) {
            this.key = key;
            this.val = val;
        }

        private static Map.Entry<String, String> asStringEntry(final Pair<String, ?> pair) {
            return new PairMapEntry<>(pair.getKey(),
                    Objects.toString(pair.getValue().toString()));
        }

        @Override
        public K getKey() {
            return null;
        }

        @Override
        public V getValue() {
            return null;
        }

        @Override
        public V setValue(final V value) {
            return null;
        }
    }

    @Inject
    public HoneyBadgerRequestFactory(final com.joyent.manta.config.ConfigContext mantaConfig,
                                     final io.honeybadger.reporter.config.ConfigContext hbConfig) {
        this.mantaConfig = mantaConfig;
        this.hbConfig = hbConfig;
        this.context = new Context().setUsername(mantaConfig.getMantaUser());
    }

    public Request build(final String path, final ExceptionContext exceptionContext) {
        final String mantaUrlWithoutTrailingSlashes = StringUtils.removeEnd(mantaConfig.getMantaURL(), MantaClient.SEPARATOR);
        final String url = mantaUrlWithoutTrailingSlashes + MantaClient.SEPARATOR + path;
        final Params params;

        try (Stream<Pair<String, Object>> stream = exceptionContext.getContextEntries().stream()) {
            final Map<String, String> map = stream.map(PairMapEntry::asStringEntry)
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            params = Params.parseParamsFromMap(hbConfig.getExcludedParams(),
                    map);
        }

        Params params = new Params();
        CgiData cgiData = new CgiData();
        return new Request(context, url, params, EMPTY_SESSION, cgiData);
    }

    Params buildParams(final ExceptionContext exceptionContext) {
        final Params params = new Params();
        if (exceptionContext == null) {
            return params;
        }

        final Set<String> keys = exceptionContext.getContextLabels();

        for (String k : keys) {
            final List<Object> values = exceptionContext.getContextValues(k);
            final String[] stringVals = new String[values.size()];

            int i = 0;
            for (Object v : values) {
                stringVals[i++] = Objects.toString(v);
            }

        }

        return params;
    }
}
