package org.omegat.connectors.machinetranslators;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;

public class TestMicrosoftTranslatorAzure {
    @Rule
    public WireMockRule wireMockRule =
           new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort().dynamicHttpsPort());

}
