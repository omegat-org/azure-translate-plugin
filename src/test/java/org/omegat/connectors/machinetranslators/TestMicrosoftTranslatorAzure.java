package org.omegat.connectors.machinetranslators;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omegat.util.*;
import wiremock.org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


@WireMockTest
public class TestMicrosoftTranslatorAzure {

    private static final String TOKEN_PATH = "/sts/v1.0/issueToken";
    private static final String V2_API_PATH = "/v2/http.svc/Translate";

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

        String text = "source text";
        String key = "abcdefg";

        WireMock wireMock = wireMockRuntimeInfo.getWireMock();
        Map<String, StringValuePattern> headers = new HashMap<>();
        headers.put("Ocp-Apim-Subscription-Key", equalTo(key));
        headers.put("Content-Type", equalTo("application/json"));
        headers.put("Accept", equalTo("application/jwt"));
        wireMock.register(get(urlPathEqualTo(TOKEN_PATH))
                .withQueryParams(headers)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("PSEUDOTOKEN")
                )
        );
        Map<String, StringValuePattern> expectedParams = new HashMap<>();
        expectedParams.put("text", equalTo(text));
        expectedParams.put("from", equalTo("en"));
        expectedParams.put("to", equalTo("de"));
        expectedParams.put("contentType", equalTo("text/plain"));
        wireMock.register(get(urlPathEqualTo(V2_API_PATH))
                .withQueryParams(expectedParams)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<string>Translation Text</string>")));
        int port = wireMockRuntimeInfo.getHttpPort();

        MicrosoftTranslatorAzure azure = new MicrosoftTranslatorAzure();
        azure.setKey(key, false);
        MicrosoftTranslatorBase translator = new MicrosoftTranslatorV2(azure);
        String result = translator.translate(new Language("EN"), new Language("DE"), "source text");
        translator.setTokenUrl(String.format("http://localhost:%d%s", port, TOKEN_PATH));
        translator.setUrl(String.format("http://localhost:%d%s", port, V2_API_PATH));
        Assertions.assertEquals("Translation Text", result);
    }

    public static synchronized void init(String configDir) {
        RuntimePreferences.setConfigDir(configDir);
        Preferences.init();
        Preferences.initFilters();
        Preferences.initSegmentation();
    }
}
