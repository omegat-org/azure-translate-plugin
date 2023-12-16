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

import org.omegat.util.HttpConnectionUtils;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Support for Microsoft Translator API machine translation.
 * @author Hiroshi Miura
 */
public class AzureTranslatorV3 extends MicrosoftTranslatorBase {

    private static final String DEFAULT_URL = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0";

    private String urlTranslate;
    private final ObjectMapper mapper = new ObjectMapper();

    public AzureTranslatorV3(MicrosoftTranslatorAzure parent) {
        super(parent);
        urlTranslate = DEFAULT_URL;
    }

    @Override
    protected String requestTranslate(String langFrom, String langTo, String text) throws Exception {
        Map<String, String> p = new TreeMap<>();
        p.put("Ocp-Apim-Subscription-Key", parent.getKey());
        p.put("Ocp-Apim-Subscription-Region", parent.getRegion());
        String url = urlTranslate + "&from=" + langFrom + "&to=" + langTo;
        String json = createJsonRequest(text);
        String res = HttpConnectionUtils.postJSON(url, json, p);
        JsonNode root = mapper.readTree(res);
        JsonNode translations = root.get(0).get("translations");
        if (translations == null) {
            return null;
        }
        JsonNode translation = translations.get(0).get("text");
        if (translation == null) {
            return null;
        }
        return translation.asText();
    }

    /**
     * Method for test.
     * @param url alternative url.
     */
    public void setUrl(String url) {
        urlTranslate = url;
    }

    /**
     * Create Watson request and return as json string.
     */
    protected String createJsonRequest(String trText) throws JsonProcessingException {
        Map<String, Object> param = new TreeMap<>();
        param.put("text", trText);
        return new ObjectMapper().writeValueAsString(Collections.singletonList(param));
    }
}
