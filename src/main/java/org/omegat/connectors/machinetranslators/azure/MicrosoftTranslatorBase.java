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
import org.omegat.util.Language;
import org.omegat.util.Log;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Support for Microsoft Translator API machine translation.
 *
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Didier Briel
 * @author Aaron Madlon-Kay
 *
 * @see <a href="https://www.microsoft.com/en-us/translator/translatorapi.aspx">Translator API</a>
 * @see <a href="https://docs.microsofttranslator.com/text-translate.html">Translate Method reference</a>
 */
public abstract class MicrosoftTranslatorBase {

    protected static final String DEFAULT_URL_TOKEN = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";
    protected String urlToken = null;
    protected String accessToken;

    protected final MicrosoftTranslatorAzure parent;

    public MicrosoftTranslatorBase(MicrosoftTranslatorAzure parent) {
        this.parent = parent;
    }

    protected void setTokenUrl(String url) {
        urlToken = url;
    }

    protected void requestToken(String key) throws Exception {
        if (urlToken == null) {
            urlToken = DEFAULT_URL_TOKEN;
        }
        Map<String, String> headers = new TreeMap<>();
        headers.put("Ocp-Apim-Subscription-Key", key);
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/jwt");
        accessToken = HttpConnectionUtils.post(urlToken, Collections.emptyMap(), headers);
    }

    /**
     * Converts language codes to Microsoft ones.
     * @param language
     *              a project language
     * @return either a language code, or a Chinese language code plus a Microsoft variant
     */
    protected String checkMSLang(Language language) {
        String lang = language.getLanguage();
        if (lang.equalsIgnoreCase("zh-cn")) {
            return "zh-CHS";
        } else if (lang.equalsIgnoreCase("zh-tw") || lang.equalsIgnoreCase("zh-hk")) {
            return "zh-CHT";
        } else {
            return language.getLanguageCode();
        }
    }

    /**
     * translate text.
     * @param sLang source langauge.
     * @param tLang target language.
     * @param text source text.
     * @return translated text.
     * @throws Exception when connection error.
     */
    protected synchronized String translate(Language sLang, Language tLang, String text) throws Exception {
        String langFrom = checkMSLang(sLang);
        String langTo = checkMSLang(tLang);
        String translation;
        if (accessToken == null) {
            requestToken(parent.getKey());
            translation = requestTranslate(langFrom, langTo, text);
        } else {
            try {
                translation = requestTranslate(langFrom, langTo, text);
            } catch (HttpConnectionUtils.ResponseError ex) {
                if (ex.code == 400) {
                    Log.log("Re-fetching Microsoft Translator API token due to 400 response");
                    requestToken(parent.getKey());
                    translation = requestTranslate(langFrom, langTo, text);
                } else {
                    throw ex;
                }
            }
        }
        return translation;
    }

    protected abstract String requestTranslate(String langFrom, String langTo, String text) throws Exception;
}
