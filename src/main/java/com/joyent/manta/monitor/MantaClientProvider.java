package com.joyent.manta.monitor;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.config.ChainedConfigContext;
import com.joyent.manta.config.ConfigContext;
import com.joyent.manta.config.DefaultsConfigContext;
import com.joyent.manta.config.SystemSettingsConfigContext;

import javax.inject.Provider;

public class MantaClientProvider implements Provider<MantaClient> {
    private static final ConfigContext MANTA_CONFIG = new ChainedConfigContext(
            new DefaultsConfigContext(),
            new SystemSettingsConfigContext().setRetries(0)
    );

    @Override
    public MantaClient get() {
        return new MantaClient(MANTA_CONFIG);
    }
}
