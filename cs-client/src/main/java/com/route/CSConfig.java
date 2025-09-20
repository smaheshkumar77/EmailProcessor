package com.route;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CSConfig {

    @ConfigProperty(name = "cs.app.key")
    String appKey;

    @ConfigProperty(name = "cs.app.sec")
    String appSecret;

    @ConfigProperty(name = "cs.callback.url")
    String callbackUrl;

    @ConfigProperty(name = "cs.tokens.file")
    String tokensFile;

    @ConfigProperty(name = "cs.refresh.interval.ms")
    long refreshIntervalMs;

    public String getAppKey() { return appKey; }
    public String getAppSecret() { return appSecret; }
    public String getCallbackUrl() { return callbackUrl; }
    public String getTokensFile() { return tokensFile; }
    public long getRefreshIntervalMs() { return refreshIntervalMs; }
}
