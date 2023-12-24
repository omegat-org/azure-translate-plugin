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

import org.omegat.core.Core;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.gui.exttrans.IMachineTranslation;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.CredentialsManager;
import org.omegat.util.Language;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;
import org.omegat.util.StringUtil;

import java.awt.Dimension;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

import javax.swing.JCheckBox;

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
public class MicrosoftTranslatorAzure extends BaseCachedTranslate implements IMachineTranslation {

    protected static final String ALLOW_MICROSOFT_TRANSLATOR_AZURE = "allow_microsoft_translator_azure";

    protected static final String PROPERTY_NEURAL = "microsoft.neural";
    protected static final String PROPERTY_V2 = "microsoft.v2";
    protected static final String PROPERTY_SUBSCRIPTION_KEY = "microsoft.api.subscription_key";
    protected static final String PROPERTY_REGION = "microsoft.api.region";

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("AzureTranslatorBundle");

    private MicrosoftTranslatorBase translator = null;

    /**
     * Constructor of the connector.
     */
    public MicrosoftTranslatorAzure() {
        super();
    }

    /**
     * Utility function to get a localized message.
     * @param key bundle key.
     * @return a localized string.
     */
    static String getString(String key) {
        return BUNDLE.getString(key);
    }

    /**
     * Register plugin into OmegaT.
     */
    @SuppressWarnings("unused")
    public static void loadPlugins() {
        String requiredVersion = "5.8.0";
        String requiredUpdate = "0";
        try {
            Class<?> clazz = Class.forName("org.omegat.util.VersionChecker");
            Method compareVersions =
                    clazz.getMethod("compareVersions", String.class, String.class, String.class, String.class);
            if ((int) compareVersions.invoke(clazz, OStrings.VERSION, OStrings.UPDATE, requiredVersion, requiredUpdate)
                    < 0) {
                Core.pluginLoadingError("MicrosoftTranslatorAzure Plugin cannot be loaded because OmegaT Version "
                        + OStrings.VERSION + " is lower than required version " + requiredVersion);
                return;
            }
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            Core.pluginLoadingError(
                    "MicrosoftTranslatorAzure cannot be loaded because this OmegaT version is not supported");
            return;
        }
        Core.registerMachineTranslationClass(MicrosoftTranslatorAzure.class);
    }

    /**
     * Unregister plugin.
     * Currently not supported.
     */
    @SuppressWarnings("unused")
    public static void unloadPlugins() {}

    /**
     * Return a name of the connector.
     * @return connector name.
     */
    public String getName() {
        return getString("MT_ENGINE_MICROSOFT_AZURE");
    }

    @Override
    protected String getPreferenceName() {
        return ALLOW_MICROSOFT_TRANSLATOR_AZURE;
    }

    /**
     * Store a credential. Credentials are stored in temporary system properties and, if
     * <code>temporary</code> is <code>false</code>, in the program's persistent preferences encoded in
     * Base64. Retrieve a credential with {@link #getCredential(String)}.
     *
     * @param id
     *            ID or key of the credential to store
     * @param value
     *            value of the credential to store
     * @param temporary
     *            if <code>false</code>, encode with Base64 and store in persistent preferences as well
     */
    protected void setCredential(String id, String value, boolean temporary) {
        System.setProperty(id, value);
        if (temporary) {
            CredentialsManager.getInstance().store(id, "");
        } else {
            CredentialsManager.getInstance().store(id, value);
        }
    }

    /**
     * Retrieve a credential with the given ID. First checks temporary system properties, then falls back to
     * the program's persistent preferences. Store a credential with
     * {@link #setCredential(String, String, boolean)}.
     *
     * @param id
     *            ID or key of the credential to retrieve
     * @return the credential value in plain text
     */
    protected String getCredential(String id) {
        String property = System.getProperty(id);
        if (property != null) {
            return property;
        }
        return CredentialsManager.getInstance().retrieve(id).orElse("");
    }

    protected void setKey(String key, boolean temporary) {
        setCredential(PROPERTY_SUBSCRIPTION_KEY, key, temporary);
    }

    protected String getKey() throws Exception {
        String key = getCredential(PROPERTY_SUBSCRIPTION_KEY);
        if (StringUtil.isEmpty(key)) {
            throw new Exception(getString("MT_ENGINE_MICROSOFT_SUBSCRIPTION_KEY_NOTFOUND"));
        }
        return key;
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        if (isV2() && (translator == null || translator instanceof AzureTranslatorV3)) {
            translator = new MicrosoftTranslatorV2(this);
        } else if (translator == null || translator instanceof MicrosoftTranslatorV2) {
            translator = new AzureTranslatorV3(this);
        }
        return translator.translate(sLang, tLang, text);
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    /**
     * Whether to use a v2 Neural Machine Translation System.
     *
     * @see <a href="https://sourceforge.net/p/omegat/feature-requests/1366/">Add support for
     * Microsoft neural machine translation</a>
     */
    protected boolean isNeural() {
        return Preferences.isPreference(PROPERTY_NEURAL);
    }

    protected boolean isV2() {
        return Preferences.isPreference(PROPERTY_V2);
    }

    protected String getRegion() {
        return Preferences.getPreferenceDefault(MicrosoftTranslatorAzure.PROPERTY_REGION, "");
    }

    @Override
    public void showConfigurationUI(Window parent) {
        JCheckBox neuralCheckBox = new JCheckBox(getString("MT_ENGINE_MICROSOFT_NEURAL_LABEL"));
        neuralCheckBox.setSelected(isNeural());
        JCheckBox v2CheckBox = new JCheckBox(getString("MT_ENGINE_MICROSOFT_V2_LABEL"));
        v2CheckBox.setSelected(isV2());
        neuralCheckBox.setEnabled(isV2());
        v2CheckBox.addActionListener(e -> neuralCheckBox.setEnabled(v2CheckBox.isSelected()));
        v2CheckBox.setToolTipText(getString("MT_ENGINE_MICROSOFT_V3_NOT_IMPLEMENTED"));

        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                setKey(panel.valueField1.getText().trim(), panel.temporaryCheckBox.isSelected());
                Preferences.setPreference(PROPERTY_NEURAL, neuralCheckBox.isSelected());
                Preferences.setPreference(PROPERTY_V2, v2CheckBox.isSelected());
                Preferences.setPreference(
                        PROPERTY_REGION, panel.valueField2.getText().trim());
            }
        };
        dialog.panel.valueLabel1.setText(getString("MT_ENGINE_MICROSOFT_SUBSCRIPTION_KEY_LABEL"));
        dialog.panel.valueField1.setText(getCredential(PROPERTY_SUBSCRIPTION_KEY));
        int height = dialog.panel.getFont().getSize();
        dialog.panel.valueField1.setPreferredSize(new Dimension(height * 24, height * 2));
        dialog.panel.valueLabel2.setText(getString("MT_ENGINE_MICROSOFT_SUBSCRIPTION_REGION"));
        dialog.panel.valueField2.setText(Preferences.getPreferenceDefault(PROPERTY_REGION, ""));
        dialog.panel.valueField2.setPreferredSize(new Dimension(height * 12, height * 2));

        boolean isCredentialStoredTemporarily =
                !CredentialsManager.getInstance().isStored(PROPERTY_SUBSCRIPTION_KEY)
                        && !System.getProperty(PROPERTY_SUBSCRIPTION_KEY, "").isEmpty();
        dialog.panel.temporaryCheckBox.setSelected(isCredentialStoredTemporarily);
        dialog.panel.itemsPanel.add(v2CheckBox);
        dialog.panel.itemsPanel.add(neuralCheckBox);

        dialog.show();
    }
}
