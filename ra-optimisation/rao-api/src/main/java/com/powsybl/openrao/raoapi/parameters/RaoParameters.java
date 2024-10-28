/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionConfigLoader;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RaoParameters extends AbstractExtendable<RaoParameters> {
    private ObjectiveFunctionParameters objectiveFunctionParameters = new ObjectiveFunctionParameters();
    private RangeActionsOptimizationParameters rangeActionsOptimizationParameters = new RangeActionsOptimizationParameters();
    private TopoOptimizationParameters topoOptimizationParameters = new TopoOptimizationParameters();
    private NotOptimizedCnecsParameters notOptimizedCnecsParameters = new NotOptimizedCnecsParameters();
    private Optional<MnecParameters> mnecParameters = Optional.empty();
    private Optional<RelativeMarginsParameters> relativeMarginsParameters = Optional.empty();
    private Optional<LoopFlowParameters> loopFlowParameters = Optional.empty();

    // Getters and setters
    public void setObjectiveFunctionParameters(ObjectiveFunctionParameters objectiveFunctionParameters) {
        this.objectiveFunctionParameters = objectiveFunctionParameters;
    }

    public void setRangeActionsOptimizationParameters(RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        this.rangeActionsOptimizationParameters = rangeActionsOptimizationParameters;
    }

    public void setTopoOptimizationParameters(TopoOptimizationParameters topoOptimizationParameters) {
        this.topoOptimizationParameters = topoOptimizationParameters;
    }

    public void setNotOptimizedCnecsParameters(NotOptimizedCnecsParameters notOptimizedCnecsParameters) {
        this.notOptimizedCnecsParameters = notOptimizedCnecsParameters;
    }

    public void setMnecParameters(MnecParameters mnecParameters) {
        this.mnecParameters = Optional.of(mnecParameters);
    }

    public void setRelativeMarginsParameters(RelativeMarginsParameters relativeMarginsParameters) {
        this.relativeMarginsParameters = Optional.of(relativeMarginsParameters);
    }

    public void setLoopFlowParameters(LoopFlowParameters loopFlowParameters) {
        this.loopFlowParameters = Optional.of(loopFlowParameters);
    }

    public ObjectiveFunctionParameters getObjectiveFunctionParameters() {
        return objectiveFunctionParameters;
    }

    public RangeActionsOptimizationParameters getRangeActionsOptimizationParameters() {
        return rangeActionsOptimizationParameters;
    }

    public TopoOptimizationParameters getTopoOptimizationParameters() {
        return topoOptimizationParameters;
    }

    public NotOptimizedCnecsParameters getNotOptimizedCnecsParameters() {
        return notOptimizedCnecsParameters;
    }

    public Optional<MnecParameters> getMnecParameters() {
        return mnecParameters;
    }

    public Optional<RelativeMarginsParameters> getRelativeMarginsParameters() {
        return relativeMarginsParameters;
    }

    public Optional<LoopFlowParameters> getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public boolean hasExtension(Class classType) {
        return Objects.nonNull(this.getExtension(classType));
    }

    // ConfigLoader

    /**
     * A configuration loader interface for the RaoParameters extensions loaded from the platform configuration
     *
     * @param <E> The extension class
     */
    public interface ConfigLoader<E extends Extension<RaoParameters>> extends ExtensionConfigLoader<RaoParameters, E> {
    }

    private static final Supplier<ExtensionProviders<RaoParameters.ConfigLoader>> PARAMETERS_EXTENSIONS_SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(RaoParameters.ConfigLoader.class, "rao-parameters"));

    /**
     * @return RaoParameters from platform default config.
     */
    public static RaoParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    /**
     * @param platformConfig PlatformConfig where the RaoParameters should be read from
     * @return RaoParameters from the provided platform config
     */
    public static RaoParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        RaoParameters parameters = new RaoParameters();
        load(parameters, platformConfig);
        parameters.loadExtensions(platformConfig);
        return parameters;
    }

    public static void load(RaoParameters parameters, PlatformConfig platformConfig) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(platformConfig);
        parameters.setObjectiveFunctionParameters(ObjectiveFunctionParameters.load(platformConfig));
        parameters.setRangeActionsOptimizationParameters(RangeActionsOptimizationParameters.load(platformConfig));
        parameters.setTopoOptimizationParameters(TopoOptimizationParameters.load(platformConfig));
        parameters.setNotOptimizedCnecsParameters(NotOptimizedCnecsParameters.load(platformConfig));
        MnecParameters.load(platformConfig).ifPresent(parameters::setMnecParameters);
        RelativeMarginsParameters.load(platformConfig).ifPresent(parameters::setRelativeMarginsParameters);
        LoopFlowParameters.load(platformConfig).ifPresent(parameters::setLoopFlowParameters);
    }

    private void loadExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : PARAMETERS_EXTENSIONS_SUPPLIER.get().getProviders()) {
            Extension extension = provider.load(platformConfig);
            if (extension != null) {
                addExtension(provider.getExtensionClass(), extension);
            }
        }
    }

    public boolean hasLoopFlowParameters() {
        return this.getLoopFlowParameters().isPresent() || this.hasExtension(OpenRaoSearchTreeParameters.class) && this.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters().isPresent();
    }
}
