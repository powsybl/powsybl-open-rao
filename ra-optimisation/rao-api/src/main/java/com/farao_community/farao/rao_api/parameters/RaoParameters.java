/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionConfigLoader;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

import java.util.Objects;

/**
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
// TODO rename getters setters with intelliji refacto
public class RaoParameters extends AbstractExtendable<RaoParameters> {

    private ObjectiveFunctionParameters objectiveFunctionParameters = ObjectiveFunctionParameters.loadDefault();
    private RangeActionsOptimizationParameters rangeActionsOptimizationParameters = RangeActionsOptimizationParameters.loadDefault();
    private TopoOptimizationParameters topoOptimizationParameters = TopoOptimizationParameters.loadDefault();
    private MultithreadingParameters multithreadingParameters = MultithreadingParameters.loadDefault();
    private SecondPreventiveRaoParameters secondPreventiveRaoParameters = SecondPreventiveRaoParameters.loadDefault();
    private RaUsageLimitsPerContingencyParameters raUsageLimitsPerContingencyParameters = RaUsageLimitsPerContingencyParameters.loadDefault();
    private NotOptimizedCnecsParameters notOptimizedCnecsParameters = NotOptimizedCnecsParameters.loadDefault();
    private LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = LoadFlowAndSensitivityParameters.loadDefault();

    // GETTERS AND SETTERS
    public ObjectiveFunctionParameters.ObjectiveFunctionType getObjectiveFunctionType() {
        return getObjectiveFunctionParameters().getObjectiveFunctionType();
    }

    public RaoParameters setObjectiveFunctionType(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType) {
        getObjectiveFunctionParameters().setObjectiveFunctionType(objectiveFunctionType);
        return this;
    }

    public int getMaxMipIterations() {
        return rangeActionsOptimizationParameters.getMaxMipIterations();
    }

    public RaoParameters setMaxMipIterations(int maxMipIterations) {
        this.rangeActionsOptimizationParameters.setMaxMipIterations(maxMipIterations);
        return this;
    }

    public double getPstPenaltyCost() {
        return rangeActionsOptimizationParameters.getPstPenaltyCost();
    }

    public RaoParameters setPstPenaltyCost(double pstPenaltyCost) {
        this.rangeActionsOptimizationParameters.setPstPenaltyCost(pstPenaltyCost);
        return this;
    }

    public double getPstSensitivityThreshold() {
        return rangeActionsOptimizationParameters.getPstSensitivityThreshold();
    }

    public RaoParameters setPstSensitivityThreshold(double pstSensitivityThreshold) {
        this.rangeActionsOptimizationParameters.setPstSensitivityThreshold(pstSensitivityThreshold);
        return this;
    }

    public double getHvdcPenaltyCost() {
        return rangeActionsOptimizationParameters.getHvdcPenaltyCost();
    }

    public void setHvdcPenaltyCost(double hvdcPenaltyCost) {
        this.rangeActionsOptimizationParameters.setHvdcPenaltyCost(hvdcPenaltyCost);
    }

    public double getHvdcSensitivityThreshold() {
        return rangeActionsOptimizationParameters.getHvdcSensitivityThreshold();
    }

    public void setHvdcSensitivityThreshold(double hvdcSensitivityThreshold) {
        this.rangeActionsOptimizationParameters.setHvdcSensitivityThreshold(hvdcSensitivityThreshold);
    }

    public double getInjectionRaPenaltyCost() {
        return rangeActionsOptimizationParameters.getInjectionRaPenaltyCost();
    }

    public void setInjectionRaPenaltyCost(double injectionRaPenaltyCost) {
        this.rangeActionsOptimizationParameters.setInjectionRaPenaltyCost(injectionRaPenaltyCost);
    }

    public double getInjectionRaSensitivityThreshold() {
        return rangeActionsOptimizationParameters.getInjectionRaSensitivityThreshold();
    }

    public void setInjectionRaSensitivityThreshold(double injectionRaSensitivityThreshold) {
        this.rangeActionsOptimizationParameters.setInjectionRaSensitivityThreshold(injectionRaSensitivityThreshold);
    }

    // TODO : DELETE
    public double getFallbackOverCost() {
        return 0;
    }


    public SensitivityAnalysisParameters getSensitivityWithLoadFlowParameters() {
        return loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters();
    }

    public String getLoadFlowProvider() {
        return loadFlowAndSensitivityParameters.getLoadFlowProvider();
    }

    public void setLoadFlowProvider(String loadFlowProvider) {
        this.loadFlowAndSensitivityParameters.setLoadFlowProvider(loadFlowProvider);
    }

    public String getSensitivityProvider() {
        return loadFlowAndSensitivityParameters.getSensitivityProvider();
    }

    public void setSensitivityProvider(String sensitivityProvider) {
        this.loadFlowAndSensitivityParameters.setSensitivityProvider(sensitivityProvider);
    }

    public RaoParameters setSensitivityWithLoadFlowParameters(SensitivityAnalysisParameters sensitivityWithLoadFlowParameters) {
        this.loadFlowAndSensitivityParameters.setSensitivityWithLoadFlowParameters(Objects.requireNonNull(sensitivityWithLoadFlowParameters));
        return this;
    }

    // TODO : DELETE
    public SensitivityAnalysisParameters getFallbackSensitivityAnalysisParameters() {
        return loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters();
    }

    public int getContingencyScenariosInParallel() {
        return multithreadingParameters.getContingencyScenariosInParallel();
    }

    public void setContingencyScenariosInParallel(int contingencyScenariosInParallel) {
        this.multithreadingParameters.setContingencyScenariosInParallel(contingencyScenariosInParallel);
    }

    public RangeActionsOptimizationParameters.Solver getLinearOptimizationSolver() {
        return rangeActionsOptimizationParameters.getLinearOptimizationSolver().getSolver();
    }

    public void setLinearOptimizationSolver(RangeActionsOptimizationParameters.Solver solver) {
        this.rangeActionsOptimizationParameters.setLinearOptimizationSolverSolver(solver);
    }

    public double getRelativeMipGap() {
        return rangeActionsOptimizationParameters.getLinearOptimizationSolver().getRelativeMipGap();
    }

    public void setRelativeMipGap(double relativeMipGap) {
        this.rangeActionsOptimizationParameters.setLinearOptimizationSolverRelativeMipGap(relativeMipGap);
    }

    public RangeActionsOptimizationParameters.PstModel getPstModel() {
        return rangeActionsOptimizationParameters.getPstModel();
    }

    public void setPstModel(RangeActionsOptimizationParameters.PstModel pstModel) {
        this.rangeActionsOptimizationParameters.setPstModel(pstModel);
    }

    public boolean getForbidCostIncrease() {
        return objectiveFunctionParameters.getForbidCostIncrease();
    }

    public void setForbidCostIncrease(boolean forbidCostIncrease) {
        this.objectiveFunctionParameters.setForbidCostIncrease(forbidCostIncrease);
    }

    public String getSolverSpecificParameters() {
        return rangeActionsOptimizationParameters.getLinearOptimizationSolver().getSolverSpecificParameters();
    }

    public void setSolverSpecificParameters(String solverSpecificParameters) {
        this.rangeActionsOptimizationParameters.setLinearOptimizationSolverSpecificParameters(solverSpecificParameters);
    }

    // TODO : rationaliser les getters/setters ??

    public void setObjectiveFunctionParameters(ObjectiveFunctionParameters objectiveFunctionParameters) {
        this.objectiveFunctionParameters = objectiveFunctionParameters;
    }

    public void setRangeActionsOptimizationParameters(RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        this.rangeActionsOptimizationParameters = rangeActionsOptimizationParameters;
    }

    public void setTopoOptimizationParameters(TopoOptimizationParameters topoOptimizationParameters) {
        this.topoOptimizationParameters = topoOptimizationParameters;
    }

    public void setMultithreadingParameters(MultithreadingParameters multithreadingParameters) {
        this.multithreadingParameters = multithreadingParameters;
    }

    public void setSecondPreventiveRaoParameters(SecondPreventiveRaoParameters secondPreventiveRaoParameters) {
        this.secondPreventiveRaoParameters = secondPreventiveRaoParameters;
    }

    public void setRaUsageLimitsPerContingencyParameters(RaUsageLimitsPerContingencyParameters raUsageLimitsPerContingencyParameters) {
        this.raUsageLimitsPerContingencyParameters = raUsageLimitsPerContingencyParameters;
    }

    public void setNotOptimizedCnecsParameters(NotOptimizedCnecsParameters notOptimizedCnecsParameters) {
        this.notOptimizedCnecsParameters = notOptimizedCnecsParameters;
    }

    public void setLoadFlowAndSensitivityParameters(LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters) {
        this.loadFlowAndSensitivityParameters = loadFlowAndSensitivityParameters;
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

    public MultithreadingParameters getMultithreadingParameters() {
        return multithreadingParameters;
    }

    public SecondPreventiveRaoParameters getSecondPreventiveRaoParameters() {
        return secondPreventiveRaoParameters;
    }

    public RaUsageLimitsPerContingencyParameters getRaUsageLimitsPerContingencyParameters() {
        return raUsageLimitsPerContingencyParameters;
    }

    public NotOptimizedCnecsParameters getNotOptimizedCnecsParameters() {
        return notOptimizedCnecsParameters;
    }

    public LoadFlowAndSensitivityParameters getLoadFlowAndSensitivityParameters() {
        return loadFlowAndSensitivityParameters;
    }

    /**
     * A configuration loader interface for the RaoParameters extensions loaded from the platform configuration
     * @param <E> The extension class
     */
    public static interface ConfigLoader<E extends Extension<RaoParameters>> extends ExtensionConfigLoader<RaoParameters, E> {
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
        parameters.readExtensions(platformConfig);

        return parameters;
    }

// TODO : handle this with new objects
    public static void load(RaoParameters parameters, PlatformConfig platformConfig) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(platformConfig);
        parameters.setObjectiveFunctionParameters(ObjectiveFunctionParameters.load(platformConfig));
        parameters.setRangeActionsOptimizationParameters(RangeActionsOptimizationParameters.load(platformConfig));
        parameters.setTopoOptimizationParameters(TopoOptimizationParameters.load(platformConfig));
        parameters.setMultithreadingParameters(MultithreadingParameters.load(platformConfig));
        parameters.setSecondPreventiveRaoParameters(SecondPreventiveRaoParameters.load(platformConfig));
        parameters.setRaUsageLimitsPerContingencyParameters(RaUsageLimitsPerContingencyParameters.load(platformConfig));
        parameters.setNotOptimizedCnecsParameters(NotOptimizedCnecsParameters.load(platformConfig));
        parameters.setLoadFlowAndSensitivityParameters(LoadFlowAndSensitivityParameters.load(platformConfig));
    }

    private void readExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : PARAMETERS_EXTENSIONS_SUPPLIER.get().getProviders()) {
            addExtension(provider.getExtensionClass(), provider.load(platformConfig));
        }
    }
}
