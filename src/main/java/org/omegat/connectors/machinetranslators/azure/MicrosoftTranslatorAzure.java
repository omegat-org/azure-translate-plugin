/**************************************************************************
 * OmegaT - Computer Assisted Translation (CAT) tool
 * with fuzzy matching, translation memory, keyword search,
 * glossaries, and translation leveraging into updated projects.
 *
 * Copyright (C) 2012 Alex Buloichik, Didier Briel
 * 2016-2017 Aaron Madlon-Kay
 * 2018 Didier Briel
 * 2022,2023 Hiroshi Miura
 * Home page: http://www.omegat.org/
 * Support center: https://omegat.org/support
 *
 * This file is part of OmegaT.
 *
 * OmegaT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OmegaT is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/
package org.omegat.connectors.machinetranslators.azure;

import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.IProjectEventListener;
import org.omegat.gui.exttrans.IMachineTranslation;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.*;

import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.OptionalLong;
import java.util.ResourceBundle;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import javax.swing.*;

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;

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
public class MicrosoftTranslatorAzure implements IMachineTranslation {

    protected boolean enabled;

    protected static final String ALLOW_MICROSOFT_TRANSLATOR_AZURE = "allow_microsoft_translator_azure";

    protected static final String PROPERTY_NEURAL = "microsoft.neural";
    protected static final String PROPERTY_V2 = "microsoft.v2";
    protected static final String PROPERTY_SUBSCRIPTION_KEY = "microsoft.api.subscription_key";

    private static final ResourceBundle bundle = ResourceBundle.getBundle("AzureTranslatorBundle");

    /**
     * Machine translation implementation can use this cache for skip requests
     * twice. Cache will be cleared when project change.
     */
    private final Cache<String, String> cache;

    public MicrosoftTranslatorAzure() {
        if (Core.getMainWindow() != null) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem();
            menuItem.setText(getName());
            menuItem.addActionListener(e -> setEnabled(menuItem.isSelected()));
            enabled = Preferences.isPreference(ALLOW_MICROSOFT_TRANSLATOR_AZURE);
            menuItem.setState(enabled);
            Core.getMainWindow().getMainMenu().getMachineTranslationMenu().add(menuItem);
            // Preferences listener
            Preferences.addPropertyChangeListener(ALLOW_MICROSOFT_TRANSLATOR_AZURE, e -> {
                boolean newValue = (Boolean) e.getNewValue();
                menuItem.setSelected(newValue);
                enabled = newValue;
            });
        }

        cache = getCacheLayer(getName());
        setCacheClearPolicy();
    }

    /**
     * Creat cache object.
     * <p>
     * MT connectors can override cache size and invalidate policy.
     * @param name name of cache which should be unique among MT connectors.
     * @return Cache object
     */
    protected Cache<String, String> getCacheLayer(String name) {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager manager = provider.getCacheManager();
        Cache<String, String> cache1 = manager.getCache(name);
        if (cache1 != null) {
            return cache1;
        }
        CaffeineConfiguration<String, String> config = new CaffeineConfiguration<>();
        config.setExpiryPolicyFactory(() -> new CreatedExpiryPolicy(Duration.ONE_DAY));
        config.setMaximumSize(OptionalLong.of(1_000));
        return manager.createCache(name, config);
    }

    /**
     * Register cache clear policy.
     */
    protected void setCacheClearPolicy() {
        CoreEvents.registerProjectChangeListener(eventType -> {
            if (eventType.equals(IProjectEventListener.PROJECT_CHANGE_TYPE.CLOSE)) {
                cache.clear();
            }
        });
    }

    public static String getString(String key) {
        return bundle.getString(key);
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

    public String getName() {
        return getString("MT_ENGINE_MICROSOFT_AZURE");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String getTranslation(Language sLang, Language tLang, String text) throws Exception {
        if (enabled) {
            return translate(sLang, tLang, text);
        } else {
            return null;
        }
    }

    @Override
    public String getCachedTranslation(Language sLang, Language tLang, String text) {
        if (enabled) {
            return getFromCache(sLang, tLang, text);
        } else {
            return null;
        }
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
        CredentialsManager.getInstance().store(id, temporary ? "" : value);
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

    protected synchronized String translate(Language sLang, Language tLang, String text) throws Exception {
        String prev = getFromCache(sLang, tLang, text.length() > 10000 ? text.substring(0, 9997) + "..." : text);
        if (prev != null) {
            return prev;
        }
        MicrosoftTranslatorBase translator;
        if (isV2()) {
            translator = new MicrosoftTranslatorV2(this);
        } else {
            translator = new AzureTranslatorV3(this);
        }
        String translation = translator.translate(sLang, tLang, text);
        if (translation != null) {
            putToCache(sLang, tLang, text, translation);
        }
        return translation;
    }

    protected String getFromCache(Language sLang, Language tLang, String text) {
        return cache.get(sLang + "/" + tLang + "/" + text);
    }

    protected void putToCache(Language sLang, Language tLang, String text, String result) {
        cache.put(sLang.toString() + "/" + tLang.toString() + "/" + text, result);
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
    protected static boolean isNeural() {
        String value = Preferences.getPreference(PROPERTY_NEURAL);
        return Boolean.parseBoolean(value);
    }

    protected static boolean isV2() {
        String value = Preferences.getPreference(PROPERTY_V2);
        return Boolean.parseBoolean(value);
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
            }
        };
        dialog.panel.valueLabel1.setText(getString("MT_ENGINE_MICROSOFT_SUBSCRIPTION_KEY_LABEL"));
        dialog.panel.valueField1.setText(getCredential(PROPERTY_SUBSCRIPTION_KEY));
        dialog.panel.valueLabel2.setVisible(false);
        dialog.panel.valueField2.setVisible(false);

        boolean isCredentialStoredTemporarily =
                !CredentialsManager.getInstance().isStored(PROPERTY_SUBSCRIPTION_KEY)
                        && !System.getProperty(PROPERTY_SUBSCRIPTION_KEY, "").isEmpty();
        dialog.panel.temporaryCheckBox.setSelected(isCredentialStoredTemporarily);
        dialog.panel.itemsPanel.add(v2CheckBox);
        dialog.panel.itemsPanel.add(neuralCheckBox);

        dialog.show();
    }
}
