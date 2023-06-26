/*
 *  OmegaT - Computer Assisted Translation (CAT) tool
 *           with fuzzy matching, translation memory, keyword search,
 *           glossaries, and translation leveraging into updated projects.
 *
 *  Copyright (C) 2012 Alex Buloichik, Didier Briel
 *                2016-2017 Aaron Madlon-Kay
 *                2018 Didier Briel
 *                2022,2023 Hiroshi Miura
 *                Home page: https://www.omegat.org/
 *                Support center: https://omegat.org/support
 *
 *  This file is part of OmegaT.
 *
 *  OmegaT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OmegaT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.omegat.connectors.machinetranslators.azure;

import org.omegat.util.Language;
import org.omegat.util.Preferences;
import org.omegat.util.PreferencesImpl;
import org.omegat.util.PreferencesXML;
import org.omegat.util.RuntimePreferences;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wiremock.org.apache.commons.io.FileUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@WireMockTest
public class TestMicrosoftTranslatorAzure {

    private static final String TOKEN_PATH = "/sts/v1.0/issueToken";
    private static final String V2_API_PATH = "/v2/http.svc/Translate";
    private static final String V3_API_PATH = "/translate";
    private static final String key = "abcdefg";

    private File tmpDir;

    @BeforeEach
    public final void setUp() throws Exception {
        tmpDir = Files.createTempDirectory("omegat").toFile();
        Assertions.assertTrue(tmpDir.isDirectory());
    }

    @AfterEach
    public final void tearDown() throws Exception {
        FileUtils.deleteDirectory(tmpDir);
    }

    @Test
    void testResponseV2(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        File prefsFile = new File(tmpDir, Preferences.FILE_PREFERENCES);
        Preferences.IPreferences prefs = new PreferencesImpl(new PreferencesXML(null, prefsFile));
        prefs.setPreference(MicrosoftTranslatorAzure.ALLOW_MICROSOFT_TRANSLATOR_AZURE, true);
        prefs.setPreference(MicrosoftTranslatorAzure.PROPERTY_V2, true);
        prefs.setPreference(MicrosoftTranslatorAzure.PROPERTY_NEURAL, false);
        init(prefsFile.getAbsolutePath());

        String text = "Buy tomorrow";
        String translation = "Morgen kaufen gehen ein";

        WireMock wireMock = wireMockRuntimeInfo.getWireMock();
        wireMock.register(post(urlPathEqualTo(TOKEN_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/jwt"))
                .withHeader("Ocp-Apim-Subscription-Key", equalTo(key))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("PSEUDOTOKEN")));
        Map<String, StringValuePattern> expectedParams = new HashMap<>();
        expectedParams.put("appid", containing("PSEUDOTOKEN"));
        expectedParams.put("text", equalTo(text));
        expectedParams.put("from", equalTo("en"));
        expectedParams.put("to", equalTo("de"));
        expectedParams.put("contentType", equalTo("text/plain"));
        wireMock.register(get(urlPathEqualTo(V2_API_PATH))
                .withQueryParams(expectedParams)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\">" + translation
                                + "</string>")));
        int port = wireMockRuntimeInfo.getHttpPort();

        MicrosoftTranslatorAzure azure = new MicrosoftTranslatorAzureMock();
        MicrosoftTranslatorV2 translator = new MicrosoftTranslatorV2(azure);
        translator.setTokenUrl(String.format("http://localhost:%d%s", port, TOKEN_PATH));
        translator.setUrl(String.format("http://localhost:%d%s", port, V2_API_PATH));
        String result = translator.translate(new Language("EN"), new Language("DE"), text);
        Assertions.assertEquals(translation, result);
    }

    public static synchronized void init(String configDir) {
        RuntimePreferences.setConfigDir(configDir);
        Preferences.init();
        Preferences.initFilters();
        Preferences.initSegmentation();
    }

    static class MicrosoftTranslatorAzureMock extends MicrosoftTranslatorAzure {
        @Override
        protected String getKey() {
            return key;
        }

        @Override
        protected void setKey(String val, boolean temporary) {}
    }

    @Test
    void testResponseV3(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        File prefsFile = new File(tmpDir, Preferences.FILE_PREFERENCES);
        Preferences.IPreferences prefs = new PreferencesImpl(new PreferencesXML(null, prefsFile));
        prefs.setPreference(MicrosoftTranslatorAzure.ALLOW_MICROSOFT_TRANSLATOR_AZURE, true);
        prefs.setPreference(MicrosoftTranslatorAzure.PROPERTY_V2, false);
        prefs.setPreference(MicrosoftTranslatorAzure.PROPERTY_NEURAL, false);
        init(prefsFile.getAbsolutePath());

        String text = "Buy tomorrow";
        String translation = "Morgen kaufen gehen ein";

        WireMock wireMock = wireMockRuntimeInfo.getWireMock();
        wireMock.register(post(urlPathEqualTo(TOKEN_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/jwt"))
                .withHeader("Ocp-Apim-Subscription-Key", equalTo(key))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("PSEUDOTOKEN")));
        Map<String, StringValuePattern> expectedParams = new HashMap<>();
        expectedParams.put("api-version", equalTo("3.0"));
        expectedParams.put("from", equalTo("en"));
        expectedParams.put("to", equalTo("de"));
        wireMock.register(post(urlPathEqualTo(V3_API_PATH))
                .withQueryParams(expectedParams)
                .withHeader("Authentication", containing("PSEUDOTOKEN"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"translations\": [ {\"text\": \"" + translation + "\"}]}]")));
        int port = wireMockRuntimeInfo.getHttpPort();
        MicrosoftTranslatorAzure azure = new MicrosoftTranslatorAzureMock();
        AzureTranslatorV3 translator = new AzureTranslatorV3(azure);
        translator.setTokenUrl(String.format("http://localhost:%d%s", port, TOKEN_PATH));
        translator.setUrl(String.format("http://localhost:%d%s", port, V3_API_PATH));
        String result = translator.translate(new Language("EN"), new Language("DE"), text);
        Assertions.assertEquals(translation, result);
    }

}
