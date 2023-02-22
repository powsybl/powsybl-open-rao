/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters.extensions;

import com.farao_community.farao.rao_api.ZoneToZonePtdfDefinition;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import static com.farao_community.farao.rao_api.RaoParametersConstants.*;
/**
 * Extension : relative margin parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RelativeMarginParametersExtension extends AbstractExtension<RaoParameters> {
    private List<ZoneToZonePtdfDefinition> ptdfBoundaries;
    // prevents relative margins from diverging to +infinity
    private double ptdfSumLowerBound;

    public static final double DEFAULT_PTDF_SUM_LOWER_BOUND = 0.01;
    public static final List<ZoneToZonePtdfDefinition> DEFAULT_RELATIVE_MARGIN_PTDF_BOUNDARIES = new ArrayList<>();

    public RelativeMarginParametersExtension(List<ZoneToZonePtdfDefinition> ptdfBoundaries, double ptdfSumLowerBound) {
        this.ptdfBoundaries = ptdfBoundaries;
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

    public static RelativeMarginParametersExtension loadDefault() {
        return new RelativeMarginParametersExtension(DEFAULT_RELATIVE_MARGIN_PTDF_BOUNDARIES, DEFAULT_PTDF_SUM_LOWER_BOUND);
    }

    public List<ZoneToZonePtdfDefinition> getPtdfBoundaries() {
        return ptdfBoundaries;
    }

    public List<String> getPtdfBoundariesAsString() {
        return ptdfBoundaries.stream()
                .map(ZoneToZonePtdfDefinition::toString)
                .collect(Collectors.toList());
    }

    public void setPtdfBoundariesFromString(List<String> boundaries) {
        this.ptdfBoundaries = boundaries.stream()
                .map(ZoneToZonePtdfDefinition::new)
                .collect(Collectors.toList());
    }

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }

    public void setPtdfSumLowerBound(double ptdfSumLowerBound) {
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

    @Override
    public String getName() {
        return RELATIVE_MARGINS_PARAMETERS_EXTENSION_NAME;
    }

    @AutoService(RaoParameters.ConfigLoader.class)
    public class RelativeMarginsParametersConfigLoader implements RaoParameters.ConfigLoader<RelativeMarginParametersExtension> {

        @Override
        public RelativeMarginParametersExtension load(PlatformConfig platformConfig) {
            Objects.requireNonNull(platformConfig);
            RelativeMarginParametersExtension parameters = loadDefault();
            platformConfig.getOptionalModuleConfig(RELATIVE_MARGINS)
                    .ifPresent(config -> {
                        parameters.setPtdfBoundariesFromString(config.getStringListProperty(PTDF_BOUNDARIES, new ArrayList<>()));
                        parameters.setPtdfSumLowerBound(config.getDoubleProperty(PTDF_SUM_LOWER_BOUND, DEFAULT_PTDF_SUM_LOWER_BOUND));
                    });
            return parameters;
        }

        @Override
        public String getExtensionName() {
            return RELATIVE_MARGINS_PARAMETERS_EXTENSION_NAME;
        }

        @Override
        public String getCategoryName() {
            return "rao-parameters";
        }

        @Override
        public Class<? super RelativeMarginParametersExtension> getExtensionClass() {
            return RelativeMarginParametersExtension.class;
        }
    }
}
