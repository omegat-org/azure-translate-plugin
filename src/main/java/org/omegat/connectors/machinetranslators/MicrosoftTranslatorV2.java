/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2012 Alex Buloichik, Didier Briel
               2016-2017 Aaron Madlon-Kay
               2018 Didier Briel
               2022,2023 Hiroshi Miura
               Home page: http://www.omegat.org/
               Support center: https://omegat.org/support

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.connectors.machinetranslators;

import org.omegat.util.HttpConnectionUtils;
import org.omegat.util.Language;
import org.omegat.util.Log;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class MicrosoftTranslatorV2 extends MicrosoftTranslatorBase {

    private static final String DEFAULT_URL = "https://api.microsofttranslator.com/v2/http.svc/Translate";
    protected static final Pattern RE_RESPONSE = Pattern.compile("<string[^>]*>(.+)</string>");

    private String urlTranslate;

    private final MicrosoftTranslatorAzure parent;

    public MicrosoftTranslatorV2(MicrosoftTranslatorAzure parent) {
        this.parent = parent;
        urlTranslate = DEFAULT_URL;
    }

    /**
     * Method for test.
     * @param url alternative url.
     */
    public void setUrl(String url) {
        urlTranslate = url;
    }

    /**
     * translate text.
     * @param sLang source langauge.
     * @param tLang target language.
     * @param text source text.
     * @return translated text.
     * @throws Exception when connection error.
     */
    @Override
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

    /**
     * Converts language codes to Microsoft ones.
     * @param language
     *              a project language
     * @return either a language code, or a Chinese language code plus a Microsoft variant
     */
    private String checkMSLang(Language language) {
        String lang = language.getLanguage();
        if (lang.equalsIgnoreCase("zh-cn")) {
            return "zh-CHS";
        } else if (lang.equalsIgnoreCase("zh-tw") || lang.equalsIgnoreCase("zh-hk")) {
            return "zh-CHT";
        } else {
            return language.getLanguageCode();
        }
    }

    private String requestTranslate(String langFrom, String langTo, String text) throws Exception {
        Map<String, String> p = new TreeMap<>();
        p.put("appid", "Bearer " + accessToken);
        p.put("text", text);
        p.put("from", langFrom);
        p.put("to", langTo);
        p.put("contentType", "text/plain");
        if (MicrosoftTranslatorAzure.isNeural()) {
            p.put("category", "generalnn");
        }

        String r = HttpConnectionUtils.get(urlTranslate, p, null);
        Matcher m = RE_RESPONSE.matcher(r);
        if (m.matches()) {
            String translatedText = m.group(1);
            translatedText = translatedText.replace("&lt;", "<");
            translatedText = translatedText.replace("&gt;", ">");
            return translatedText;
        } else {
            Log.logWarningRB("MT_ENGINE_MICROSOFT_WRONG_RESPONSE");
            return null;
        }
    }

}
