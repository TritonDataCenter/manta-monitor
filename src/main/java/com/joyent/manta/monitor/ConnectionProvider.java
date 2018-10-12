package com.joyent.manta.monitor;

import com.google.inject.throwingproviders.CheckedProvider;

import java.io.IOException;
import java.net.MalformedURLException;

public interface ConnectionProvider<T> extends CheckedProvider<T> {

    T get() throws MalformedURLException, IOException;
}
