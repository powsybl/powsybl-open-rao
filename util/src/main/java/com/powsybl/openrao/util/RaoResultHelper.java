package com.powsybl.openrao.util;

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class RaoResultHelper {

    private RaoResultHelper() {
    }

    /**
     * Indicates whether all the CNECs of a given type are secure (i.e. with a margin >= 0) at the last instant (i.e. after RAO).
     *
     * @param raoResult                      The RaoResult for which to check security.
     * @param crac                           The CRAC for which to check security.
     * @param raoParameters                  The RaoParameters for which to check security.
     * @param u                              The types of CNECs to check (FLOW -> FlowCNECs, ANGLE -> AngleCNECs, VOLTAGE -> VoltageCNECs). 1 to 3 arguments can be provided.
     * @return whether all the CNECs of the given type(s) are secure at the last instant (i.e. after RAO).
     */
    public static boolean isSecure(RaoResult raoResult, Crac crac, RaoParameters raoParameters, PhysicalParameter... u) {
        boolean excludeCnecsForTsosWithoutCras = raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras();
        Unit flowUnit = getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isDc() ? Unit.MEGAWATT : Unit.AMPERE;

        return isSecure(raoResult, crac, excludeCnecsForTsosWithoutCras, flowUnit, u);
    }

    /**
     * Indicates whether all the CNECs of a given type are secure (i.e. with a margin >= 0) at the last instant (i.e. after RAO).
     *
     * @param raoResult                      The RaoResult for which to check security.
     * @param cracs                           The CRACs for which to check security.
     * @param raoParameters                  The RaoParameters for which to check security.
     * @param u                              The types of CNECs to check (FLOW -> FlowCNECs, ANGLE -> AngleCNECs, VOLTAGE -> VoltageCNECs). 1 to 3 arguments can be provided.
     * @return whether all the CNECs of the given type(s) are secure at the last instant (i.e. after RAO).
     */
    public static boolean isSecure(TimeCoupledRaoResult raoResult, TemporalData<Crac> cracs, RaoParameters raoParameters, PhysicalParameter... u) {

        for (OffsetDateTime timestamp : cracs.getTimestamps()) {
            if (!isSecure(raoResult.getIndividualRaoResult(timestamp), cracs.getData(timestamp).orElseThrow(), raoParameters, u)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSecure(RaoResult raoResult, Crac crac, boolean excludeCnecsForTsosWithoutCras, Unit flowUnit, PhysicalParameter... u) {
        Set<PhysicalParameter> parameters = new HashSet<>(Arrays.asList(u));
        if (parameters.isEmpty()) {
            throw new OpenRaoException("No physical parameter provided.");
        }
        if (raoResult.getComputationStatus() == ComputationStatus.FAILURE) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("RAO computation failed. It is not possible to assess security.");
            return false;
        }
        Set<String> tsosWithoutCras = new HashSet<>();

        if (excludeCnecsForTsosWithoutCras) {
            Set<String> allTsos = crac.getRemedialActions().stream().map(RemedialAction::getOperator).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<String> allTsosWithCras = crac.getRemedialActions()
                .stream()
                .filter(remedialAction ->
                    remedialAction
                        .getUsageRules()
                        .stream()
                        .anyMatch(usageRule -> usageRule.getInstant().isCurative())
                )
                .map(RemedialAction::getOperator)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            tsosWithoutCras.addAll(
                allTsos
                    .stream()
                    .filter(tso -> !allTsosWithCras.contains(tso)).collect(Collectors.toSet())
            );
        }
        if (parameters.contains(PhysicalParameter.FLOW)) {
            // use the same flow unit as the one use for the LF
            // some FlowCNECs shall not be taken into account for the security assessment:
            // - MNECs
            // - CNECs for TSOS without CRAs (if excludeCnecsForTsosWithoutCras is true)
            // - outage CNECs that were duplicated from auto CNECs
            for (FlowCnec flowCnec : crac.getFlowCnecs()) {
                if (flowCnec.isOptimized() && !tsosWithoutCras.contains(flowCnec.getOperator()) && !flowCnec.getId().contains("OUTAGE DUPLICATE")) {
                    Optional<Double> minMargin = safeGetDouble(raoResult.getMargin(flowCnec.getState().getInstant(), flowCnec, flowUnit));
                    if (minMargin.isPresent()) {
                        if (minMargin.get() < 0) {
                            return false;
                        }
                    } else {
                        // no flow value available: assume it is secure
                        throw new OpenRaoException("No flow value available for FlowCNEC %s.".formatted(flowCnec.getId()));
                    }
                }
            }
        }
        if (parameters.contains(PhysicalParameter.ANGLE)) {
            for (AngleCnec angleCnec : crac.getAngleCnecs()) {
                Optional<Double> minDegreeMargin = safeGetDouble(raoResult.getMargin(angleCnec.getState().getInstant(), angleCnec, Unit.DEGREE));
                if (minDegreeMargin.isPresent()) {
                    if (minDegreeMargin.get() < 0) {
                        return false;
                    }
                } else {
                    throw new OpenRaoException("No angle value available for AngleCNEC %s.".formatted(angleCnec.getId()));
                }
            }
        }
        if (parameters.contains(PhysicalParameter.VOLTAGE)) {
            for (VoltageCnec voltageCnec : crac.getVoltageCnecs()) {
                Optional<Double> minKiloVoltMargin = safeGetDouble(raoResult.getMargin(voltageCnec.getState().getInstant(), voltageCnec, Unit.KILOVOLT));
                if (minKiloVoltMargin.isPresent()) {
                    if (minKiloVoltMargin.get() < 0) {
                        return false;
                    }
                } else {
                    throw new OpenRaoException("No voltage value available for VoltageCNEC %s.".formatted(voltageCnec.getId()));
                }
            }
        }
        return true;
    }

    private static Optional<Double> safeGetDouble(double value) {
        return Double.isNaN(value) ? Optional.empty() : Optional.of(value);
    }
}
