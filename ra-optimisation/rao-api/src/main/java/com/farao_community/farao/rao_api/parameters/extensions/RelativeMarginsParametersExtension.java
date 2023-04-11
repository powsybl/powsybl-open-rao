/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters.extensions;

import com.farao_community.farao.rao_api.ZoneToZonePtdfDefinition;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static com.farao_community.farao.rao_api.RaoParametersConstants.*;
/**
 * Extension : relative margin parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RelativeMarginsParametersExtension extends AbstractExtension<RaoParameters> {

    static final double DEFAULT_PTDF_SUM_LOWER_BOUND = 0.01;
    static final List<ZoneToZonePtdfDefinition> DEFAULT_RELATIVE_MARGIN_PTDF_BOUNDARIES = new ArrayList<>();
    private List<ZoneToZonePtdfDefinition> ptdfBoundaries = DEFAULT_RELATIVE_MARGIN_PTDF_BOUNDARIES;
    // prevents relative margins from diverging to +infinity
    private double ptdfSumLowerBound = DEFAULT_PTDF_SUM_LOWER_BOUND;

    public RelativeMarginsParametersExtension() { }

    public RelativeMarginsParametersExtension(double ptdfSumLowerBound) {
        this.ptdfSumLowerBound = ptdfSumLowerBound;
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
        return RELATIVE_MARGINS;
    }

    public static RelativeMarginsParametersExtension buildFromRaoParameters(RaoParameters raoParameters) {
        return raoParameters.getExtension(RelativeMarginsParametersExtension.class);
    }
}

