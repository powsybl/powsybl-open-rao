/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
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
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RaoParameters extends AbstractExtendable<RaoParameters> {
    private ObjectiveFunctionParameters objectiveFunctionParameters;
    private RangeActionsOptimizationParameters rangeActionsOptimizationParameters;
    private TopoOptimizationParameters topoOptimizationParameters;
    private NotOptimizedCnecsParameters notOptimizedCnecsParameters;
    private Optional<MnecParameters> mnecParameters;
    private Optional<RelativeMarginsParameters> relativeMarginsParameters;
    private Optional<LoopFlowParameters> loopFlowParameters;

    public RaoParameters(final ReportNode reportNode) {
        this.objectiveFunctionParameters = new ObjectiveFunctionParameters();
        this.rangeActionsOptimizationParameters = new RangeActionsOptimizationParameters();
        this.topoOptimizationParameters = new TopoOptimizationParameters(reportNode);
        this.notOptimizedCnecsParameters = new NotOptimizedCnecsParameters();
        this.mnecParameters = Optional.empty();
        this.relativeMarginsParameters = Optional.empty();
        this.loopFlowParameters = Optional.empty();
    }

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
    public static RaoParameters load(final ReportNode reportNode) {
        return load(PlatformConfig.defaultConfig(), reportNode);
    }

    /**
     * @param platformConfig PlatformConfig where the RaoParameters should be read from
     * @return RaoParameters from the provided platform config
     */
    public static RaoParameters load(final PlatformConfig platformConfig, final ReportNode reportNode) {
        Objects.requireNonNull(platformConfig);
        RaoParameters parameters = new RaoParameters(reportNode);
        load(parameters, platformConfig, reportNode);
        parameters.loadExtensions(platformConfig);
        addOptionalExtensionsDefaultValuesIfExist(parameters, reportNode);
        return parameters;
    }

    public static void load(final RaoParameters parameters, final PlatformConfig platformConfig, final ReportNode reportNode) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(platformConfig);
        parameters.setObjectiveFunctionParameters(ObjectiveFunctionParameters.load(platformConfig));
        parameters.setRangeActionsOptimizationParameters(RangeActionsOptimizationParameters.load(platformConfig));
        parameters.setTopoOptimizationParameters(TopoOptimizationParameters.load(platformConfig, reportNode));
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

    public static void addOptionalExtensionsDefaultValuesIfExist(final RaoParameters parameters, final ReportNode reportNode) {
        OpenRaoSearchTreeParameters extension = parameters.getExtension(OpenRaoSearchTreeParameters.class);
        if (parameters.getMnecParameters().isPresent()) {
            if (Objects.isNull(extension)) {
                parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(reportNode));
            }
            extension = parameters.getExtension(OpenRaoSearchTreeParameters.class);
            if (extension.getMnecParameters().isEmpty()) {
                extension.setMnecParameters(new SearchTreeRaoMnecParameters());
            }
        } else {
            if (!Objects.isNull(extension) && extension.getMnecParameters().isPresent()) {
                parameters.setMnecParameters(new com.powsybl.openrao.raoapi.parameters.MnecParameters());
            }
        }
        if (parameters.getRelativeMarginsParameters().isPresent()) {
            if (Objects.isNull(extension)) {
                parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(reportNode));
            }
            extension = parameters.getExtension(OpenRaoSearchTreeParameters.class);
            if (extension.getRelativeMarginsParameters().isEmpty()) {
                extension.setRelativeMarginsParameters(new SearchTreeRaoRelativeMarginsParameters());
            }
        } else {
            if (!Objects.isNull(extension) && extension.getRelativeMarginsParameters().isPresent()) {
                parameters.setRelativeMarginsParameters(new com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters());
            }
        }
        if (parameters.getLoopFlowParameters().isPresent()) {
            if (Objects.isNull(extension)) {
                parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(reportNode));
            }
            extension = parameters.getExtension(OpenRaoSearchTreeParameters.class);
            if (extension.getLoopFlowParameters().isEmpty()) {
                extension.setLoopFlowParameters(new SearchTreeRaoLoopFlowParameters());
            }
        } else {
            if (!Objects.isNull(extension) && extension.getLoopFlowParameters().isPresent()) {
                parameters.setLoopFlowParameters(new com.powsybl.openrao.raoapi.parameters.LoopFlowParameters());
            }
        }
    }
}
