package com.farao_community.farao.rao_api.parameters.extensions;

import com.farao_community.farao.rao_api.parameters.ParametersUtil;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;

import java.util.ArrayList;
import java.util.Objects;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;
/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class LoopFlowParametersConfigLoader implements RaoParameters.ConfigLoader<LoopFlowParametersExtension> {

    @Override
    public LoopFlowParametersExtension load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        LoopFlowParametersExtension parameters = LoopFlowParametersExtension.loadDefault();
        platformConfig.getOptionalModuleConfig(LOOP_FLOW_PARAMETERS)
                .ifPresent(config -> {
                    parameters.setAcceptableIncrease(config.getDoubleProperty(ACCEPTABLE_INCREASE, LoopFlowParametersExtension.DEFAULT_ACCEPTABLE_INCREASE));
                    parameters.setApproximation(config.getEnumProperty(APPROXIMATION, LoopFlowParametersExtension.Approximation.class,
                            LoopFlowParametersExtension.DEFAULT_APPROXIMATION));
                    parameters.setConstraintAdjustmentCoefficient(config.getDoubleProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT, LoopFlowParametersExtension.DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                    parameters.setViolationCost(config.getDoubleProperty(VIOLATION_COST, LoopFlowParametersExtension.DEFAULT_VIOLATION_COST));
                    parameters.setCountries(ParametersUtil.convertToCountrySet(config.getStringListProperty(COUNTRIES, new ArrayList<>())));
                });
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return LOOP_FLOW_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LoopFlowParametersExtension> getExtensionClass() {
        return LoopFlowParametersExtension.class;
    }
}
