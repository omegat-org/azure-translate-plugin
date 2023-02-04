package org.omegat.connectors.machinetranslators.azure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.omegat.util.HttpConnectionUtils;
import org.omegat.util.Language;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Hiroshi Miura
 */
public class AzureTranslatorV3 extends MicrosoftTranslatorBase {

    private static final String TRANSLATE_BASE_URL = "https://api.cognitive.microsofttranslator.com";
    private final MicrosoftTranslatorAzure parent;
    private String urlTranslate;

    private final ObjectMapper mapper;

    public AzureTranslatorV3(MicrosoftTranslatorAzure parent) {
        this.parent = parent;
        urlTranslate = TRANSLATE_BASE_URL;
        mapper = new ObjectMapper();
    }

    public void setUrl(String url) {
        urlTranslate = url;
    }

    protected synchronized String translate(Language sLang, Language tLang, String text) throws Exception {
        String langFrom = checkMSLang(sLang);
        String langTo = checkMSLang(tLang);
        String translation = requestTranslate(langFrom, langTo, text);
        Response response = mapper.readValue(translation, Response.class);
        return response.translations.text;
    }

    private String requestTranslate(String langFrom, String langTo, String text) throws Exception {
        String key = parent.getKey();
        Map<String, String> headerParam = new TreeMap<>();
        headerParam.put("Ocp-Apim-Subscription-Key", key);
        String url = urlTranslate + "/translate?api-version=3.0&" + "from=" + langFrom + "&to=" +langTo;
        Request req = new Request(text);
        ObjectWriter writer = mapper.writer();
        String json = writer.writeValueAsString(req);
        return HttpConnectionUtils.postJSON(url, json, headerParam);
    }

    static class Response {
        public Translations translations;
    }

    static class Translations {
        public String text;
    }

    static class Request {
        public String text;

        public Request(String text) {
            this.text = text;
        }
    }
}

