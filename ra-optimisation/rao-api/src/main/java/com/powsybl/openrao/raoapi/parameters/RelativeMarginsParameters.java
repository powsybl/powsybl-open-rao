/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.ZoneToZonePtdfDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * Extension : relative margin parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RelativeMarginsParameters {

    static final List<ZoneToZonePtdfDefinition> DEFAULT_RELATIVE_MARGIN_PTDF_BOUNDARIES = new ArrayList<>();
    private List<ZoneToZonePtdfDefinition> ptdfBoundaries = DEFAULT_RELATIVE_MARGIN_PTDF_BOUNDARIES;
    // prevents relative margins from diverging to +infinity

    public List<ZoneToZonePtdfDefinition> getPtdfBoundaries() {
        return ptdfBoundaries;
    }

    public List<String> getPtdfBoundariesAsString() {
        return ptdfBoundaries.stream()
                .map(ZoneToZonePtdfDefinition::toString)
                .toList();
    }

    public void setPtdfBoundariesFromString(List<String> boundaries) {
        this.ptdfBoundaries = boundaries.stream()
                .map(ZoneToZonePtdfDefinition::new)
                .toList();
    }

    public static Optional<RelativeMarginsParameters> load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(RELATIVE_MARGINS_SECTION)
            .map(config -> {
                RelativeMarginsParameters parameters = new RelativeMarginsParameters();
                parameters.setPtdfBoundariesFromString(config.getStringListProperty(PTDF_BOUNDARIES, new ArrayList<>()));
                return parameters;
            });
    }
}

