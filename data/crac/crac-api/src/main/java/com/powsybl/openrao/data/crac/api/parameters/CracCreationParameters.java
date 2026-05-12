/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionConfigLoader;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Parameters related to the creation of a CRAC.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreationParameters extends AbstractExtendable<CracCreationParameters> {

    static final String MODULE_NAME = "crac-creation-parameters";
    private static final String DEFAULT_CRAC_FACTORY_NAME = CracFactory.findDefault().getName();
    static final MonitoredLineSide DEFAULT_DEFAULT_MONITORED_LINE_SIDE = MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES;

    public interface ConfigLoader<E extends Extension<CracCreationParameters>> extends ExtensionConfigLoader<CracCreationParameters, E> {
    }

    private static final Supplier<ExtensionProviders<ConfigLoader>> PARAMETERS_EXTENSIONS_SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, MODULE_NAME));

    private String cracFactoryName = DEFAULT_CRAC_FACTORY_NAME;

    public enum MonitoredLineSide {
        MONITOR_LINES_ON_SIDE_ONE(Set.of(TwoSides.ONE)),
        MONITOR_LINES_ON_SIDE_TWO(Set.of(TwoSides.TWO)),
        MONITOR_LINES_ON_BOTH_SIDES(Set.of(TwoSides.ONE, TwoSides.TWO));

        private final Set<TwoSides> monitoredSides;

        MonitoredLineSide(Set<TwoSides> monitoredSides) {
            this.monitoredSides = monitoredSides;
        }

        Set<TwoSides> getMonitoredSides() {
            return monitoredSides;
        }
    }

    public enum DurationThresholdsLimits {
        DURATION_THRESHOLDS_LIMITS_MAX_OUTAGE_INSTANT(60),
        DURATION_THRESHOLDS_LIMITS_MAX_AUTO_INSTANT(900);

        private final int limit;

        DurationThresholdsLimits(int limit) {
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }
    }

    private MonitoredLineSide defaultMonitoredLineSide = DEFAULT_DEFAULT_MONITORED_LINE_SIDE;

    private Map<String, RaUsageLimits> raUsageLimitsPerInstant = new HashMap<>();

    public String getCracFactoryName() {
        return cracFactoryName;
    }

    public void setCracFactoryName(String cracFactoryName) {
        this.cracFactoryName = cracFactoryName;
    }

    public CracFactory getCracFactory() {
        return CracFactory.find(cracFactoryName);
    }

    public Set<TwoSides> getDefaultMonitoredSides() {
        return defaultMonitoredLineSide.getMonitoredSides();
    }

    MonitoredLineSide getDefaultMonitoredLineSide() {
        return defaultMonitoredLineSide;
    }

    public void setDefaultMonitoredLineSide(MonitoredLineSide defaultMonitoredLineSide) {
        this.defaultMonitoredLineSide = defaultMonitoredLineSide;
    }

    public Map<String, RaUsageLimits> getRaUsageLimitsPerInstant() {
        return this.raUsageLimitsPerInstant;
    }

    public void addRaUsageLimitsForInstant(String instant, RaUsageLimits raUsageLimits) {
        this.raUsageLimitsPerInstant.put(instant, raUsageLimits);
    }

    public static CracCreationParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static CracCreationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        CracCreationParameters parameters = new CracCreationParameters();

        platformConfig.getOptionalModuleConfig(MODULE_NAME)
            .ifPresent(config -> parameters.setCracFactoryName(config.getStringProperty("crac-factory", DEFAULT_CRAC_FACTORY_NAME)));

        parameters.readExtensions(platformConfig);
        return parameters;
    }

    private void readExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : PARAMETERS_EXTENSIONS_SUPPLIER.get().getProviders()) {
            addExtension(provider.getExtensionClass(), provider.load(platformConfig));
        }
    }
}
