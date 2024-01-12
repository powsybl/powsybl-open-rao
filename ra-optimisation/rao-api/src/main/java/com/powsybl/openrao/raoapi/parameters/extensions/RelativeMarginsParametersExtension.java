/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.openrao.raoapi.ZoneToZonePtdfDefinition;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.ArrayList;
import java.util.List;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
/**
 * Extension : relative margin parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RelativeMarginsParametersExtension extends AbstractExtension<RaoParameters> {

    static final double DEFAULT_PTDF_SUM_LOWER_BOUND = 0.01;
    static final PtdfApproximation DEFAULT_PTDF_APPROXIMATION = PtdfApproximation.FIXED_PTDF;
    static final List<ZoneToZonePtdfDefinition> DEFAULT_RELATIVE_MARGIN_PTDF_BOUNDARIES = new ArrayList<>();
    private List<ZoneToZonePtdfDefinition> ptdfBoundaries = DEFAULT_RELATIVE_MARGIN_PTDF_BOUNDARIES;
    // prevents relative margins from diverging to +infinity
    private double ptdfSumLowerBound = DEFAULT_PTDF_SUM_LOWER_BOUND;
    private PtdfApproximation ptdfApproximation = DEFAULT_PTDF_APPROXIMATION;

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

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }

    public PtdfApproximation getPtdfApproximation() {
        return ptdfApproximation;
    }

    public void setPtdfApproximation(PtdfApproximation ptdfApproximation) {
        this.ptdfApproximation = ptdfApproximation;
    }

    public void setPtdfSumLowerBound(double ptdfSumLowerBound) {
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

    @Override
    public String getName() {
        return RELATIVE_MARGINS;
    }
}

