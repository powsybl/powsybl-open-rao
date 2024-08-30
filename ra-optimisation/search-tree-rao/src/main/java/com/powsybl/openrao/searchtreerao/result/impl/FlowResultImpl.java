/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowResultImpl implements FlowResult {
    protected final SystematicSensitivityResult systematicSensitivityResult;
    private final Map<FlowCnec, Map<TwoSides, Double>> commercialFlows;
    private final Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums;
    private final FlowResult fixedCommercialFlows;
    private final FlowResult fixedPtdfZonalSums;
    private final Map<FlowCnec, Double> marginMapMW = new ConcurrentHashMap<>();
    private final Map<FlowCnec, Double> marginMapA = new ConcurrentHashMap<>();

    public FlowResultImpl(SystematicSensitivityResult systematicSensitivityResult,
                          Map<FlowCnec, Map<TwoSides, Double>> commercialFlows,
                          Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums) {
        this(systematicSensitivityResult, commercialFlows, null, ptdfZonalSums, null);
    }

    public FlowResultImpl(SystematicSensitivityResult systematicSensitivityResult,
                          FlowResult fixedCommercialFlows,
                          FlowResult fixedPtdfZonalSums) {
        this(systematicSensitivityResult, null, fixedCommercialFlows, null, fixedPtdfZonalSums);
    }

    public FlowResultImpl(SystematicSensitivityResult systematicSensitivityResult,
                           Map<FlowCnec, Map<TwoSides, Double>> commercialFlows,
                           FlowResult fixedCommercialFlows,
                           Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums,
                           FlowResult fixedPtdfZonalSums) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        if (commercialFlows == null && fixedCommercialFlows == null
            || commercialFlows != null && fixedCommercialFlows != null) {
            throw new OpenRaoException("Either commercialFlows or fixedCommercialFlows should be non null");
        }
        if (ptdfZonalSums == null && fixedPtdfZonalSums == null
            || ptdfZonalSums != null && fixedPtdfZonalSums != null) {
            throw new OpenRaoException("Either ptdfZonalSums or fixedPtdfZonalSums should be non null");
        }
        this.commercialFlows = commercialFlows;
        this.ptdfZonalSums = ptdfZonalSums;
        this.fixedCommercialFlows = fixedCommercialFlows;
        this.fixedPtdfZonalSums = fixedPtdfZonalSums;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getReferenceFlow(flowCnec, side);
        } else if (unit == Unit.AMPERE) {
            double intensity = systematicSensitivityResult.getReferenceIntensity(flowCnec, side);
            if (Double.isNaN(intensity) || Math.abs(intensity) <= 1e-6) {
                return systematicSensitivityResult.getReferenceFlow(flowCnec, side) * RaoUtil.getFlowUnitMultiplier(flowCnec, side, Unit.MEGAWATT, Unit.AMPERE);
            } else {
                return intensity;
            }
        } else {
            throw new OpenRaoException("Unknown unit for flow.");
        }
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant instant) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getReferenceFlow(flowCnec, side, instant);
        } else if (unit == Unit.AMPERE) {
            double intensity = systematicSensitivityResult.getReferenceIntensity(flowCnec, side, instant);
            if (Double.isNaN(intensity) || Math.abs(intensity) <= 1e-6) {
                return systematicSensitivityResult.getReferenceFlow(flowCnec, side, instant) * RaoUtil.getFlowUnitMultiplier(flowCnec, side, Unit.MEGAWATT, Unit.AMPERE);
            } else {
                return intensity;
            }
        } else {
            throw new OpenRaoException("Unknown unit for flow.");
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (unit != Unit.MEGAWATT) {
            throw new OpenRaoException("Commercial flows only in MW.");
        }
        if (fixedCommercialFlows != null) {
            return fixedCommercialFlows.getCommercialFlow(flowCnec, side, unit);
        } else {
            if (!commercialFlows.containsKey(flowCnec) || !commercialFlows.get(flowCnec).containsKey(side)) {
                throw new OpenRaoException(format("No commercial flow on the CNEC %s on side %s", flowCnec.getName(), side));
            }
            return commercialFlows.get(flowCnec).get(side);
        }
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        if (fixedPtdfZonalSums != null) {
            return fixedPtdfZonalSums.getPtdfZonalSum(flowCnec, side);
        } else {
            if (!ptdfZonalSums.containsKey(flowCnec) || !ptdfZonalSums.get(flowCnec).containsKey(side)) {
                throw new OpenRaoException(format("No PTDF computed on the CNEC %s on side %s", flowCnec.getName(), side));
            }
            return ptdfZonalSums.get(flowCnec).get(side);
        }
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        if (fixedPtdfZonalSums != null) {
            return fixedPtdfZonalSums.getPtdfZonalSums();
        } else {
            return ptdfZonalSums;
        }
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return convert(systematicSensitivityResult.getStatus());
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return convert(systematicSensitivityResult.getStatus(state));
    }

    private ComputationStatus convert(SystematicSensitivityResult.SensitivityComputationStatus sensiStatus) {
        return switch (sensiStatus) {
            case FAILURE -> ComputationStatus.FAILURE;
            case SUCCESS -> ComputationStatus.DEFAULT;
            case PARTIAL_FAILURE -> ComputationStatus.PARTIAL_FAILURE;
        };
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        if (unit.equals(Unit.MEGAWATT)) {
            return checkMarginMapAndGet(flowCnec, unit, marginMapMW);
        } else if (unit.equals(Unit.AMPERE)) {
            return checkMarginMapAndGet(flowCnec, unit, marginMapA);
        } else {
            throw new OpenRaoException(String.format("Wrong unit for flow cnec: %s", unit));
        }
    }

    private double checkMarginMapAndGet(FlowCnec flowCnec, Unit unit, Map<FlowCnec, Double> marginMap) {
        if (marginMap.containsKey(flowCnec)) {
            return marginMap.get(flowCnec);
        }
        double margin = flowCnec.getMonitoredSides().stream()
            .map(side -> getMargin(flowCnec, side, unit))
            .min(Double::compareTo).orElseThrow();
        marginMap.put(flowCnec, margin);
        return margin;
    }
}
