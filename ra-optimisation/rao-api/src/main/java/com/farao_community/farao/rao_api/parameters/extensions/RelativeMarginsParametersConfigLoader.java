package com.farao_community.farao.rao_api.parameters.extensions;

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
public class RelativeMarginsParametersConfigLoader implements RaoParameters.ConfigLoader<RelativeMarginsParametersExtension> {

    @Override
    public RelativeMarginsParametersExtension load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        RelativeMarginsParametersExtension parameters = RelativeMarginsParametersExtension.loadDefault();
        platformConfig.getOptionalModuleConfig(RELATIVE_MARGINS)
                .ifPresent(config -> {
                    parameters.setPtdfBoundariesFromString(config.getStringListProperty(PTDF_BOUNDARIES, new ArrayList<>()));
                    parameters.setPtdfSumLowerBound(config.getDoubleProperty(PTDF_SUM_LOWER_BOUND, RelativeMarginsParametersExtension.DEFAULT_PTDF_SUM_LOWER_BOUND));
                });
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return RELATIVE_MARGINS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super RelativeMarginsParametersExtension> getExtensionClass() {
        return RelativeMarginsParametersExtension.class;
    }
}
