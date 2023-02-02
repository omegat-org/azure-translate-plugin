/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2012 Alex Buloichik, Didier Briel
               2016-2017 Aaron Madlon-Kay
               2018 Didier Briel
               2022 Hiroshi Miura
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

import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.Language;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;
import org.omegat.util.StringUtil;

import javax.swing.JCheckBox;
import java.awt.Window;

/**
 * Support for Microsoft Translator API machine translation.
 *
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Didier Briel
 * @author Aaron Madlon-Kay
 * @author Hiroshi Miura
 *
 * @see <a href="https://www.microsoft.com/en-us/translator/translatorapi.aspx">Translator API</a>
 * @see <a href="https://docs.microsofttranslator.com/text-translate.html">Translate Method reference</a>
 */
public class MicrosoftTranslatorAzure extends BaseTranslate {

    protected static final String PROPERTY_NEURAL = "microsoft.neural";
    protected static final String PROPERTY_SUBSCRIPTION_KEY = "microsoft.api.subscription_key";

    @Override
    protected String getPreferenceName() {
        return Preferences.ALLOW_MICROSOFT_TRANSLATOR_AZURE;
    }

    public String getName() {
        return OStrings.getString("MT_ENGINE_MICROSOFT_AZURE");
    }

    protected String getKey() throws Exception {
        String key = getCredential(PROPERTY_SUBSCRIPTION_KEY);
        if (StringUtil.isEmpty(key)) {
            throw new Exception(OStrings.getString("MT_ENGINE_MICROSOFT_SUBSCRIPTION_KEY_NOTFOUND"));
        }
        return key;
    }

    @Override
    protected synchronized String translate(Language sLang, Language tLang, String text) throws Exception {
        String prev = getFromCache(sLang, tLang, text.length() > 10000 ? text.substring(0, 9997) + "..." : text);
        if (prev != null) {
            return prev;
        }
        String translation = null;
        // call V2 or V3
        if (translation != null) {
            putToCache(sLang, tLang, text, translation);
        }
        return translation;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void showConfigurationUI(Window parent) {
        JCheckBox neuralCheckBox = new JCheckBox(OStrings.getString("MT_ENGINE_MICROSOFT_NEURAL_LABEL"));
        neuralCheckBox.setSelected(MicrosoftTranslatorBase.isNeural());

        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                String key = panel.valueField1.getText().trim();
                boolean temporary = panel.temporaryCheckBox.isSelected();
                setCredential(PROPERTY_SUBSCRIPTION_KEY, key, temporary);
                
                System.setProperty(PROPERTY_NEURAL, Boolean.toString(neuralCheckBox.isSelected()));
                Preferences.setPreference(PROPERTY_NEURAL, neuralCheckBox.isSelected());                
            }
        };
        dialog.panel.valueLabel1.setText(OStrings.getString("MT_ENGINE_MICROSOFT_SUBSCRIPTION_KEY_LABEL"));
        dialog.panel.valueField1.setText(getCredential(PROPERTY_SUBSCRIPTION_KEY));
        dialog.panel.valueLabel2.setVisible(false);
        dialog.panel.valueField2.setVisible(false);
        dialog.panel.temporaryCheckBox.setSelected(isCredentialStoredTemporarily(PROPERTY_SUBSCRIPTION_KEY));
        dialog.panel.itemsPanel.add(neuralCheckBox);

        dialog.show();
    }
}
