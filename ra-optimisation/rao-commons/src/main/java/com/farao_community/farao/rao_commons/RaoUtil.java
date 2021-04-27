/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramBuilder;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.MinMarginObjectiveFunction;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.rao_api.parameters.RaoParameters.ObjectiveFunction.*;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoUtil.class);

    private RaoUtil() {
    }

    public static void initData(RaoInput raoInput, RaoParameters raoParameters) {
        checkParameters(raoParameters, raoInput);
        initNetwork(raoInput.getNetwork(), raoInput.getNetworkVariantId());
    }

    public static void initNetwork(Network network, String networkVariantId) {
        network.getVariantManager().setWorkingVariant(networkVariantId);
    }

    public static void checkParameters(RaoParameters raoParameters, RaoInput raoInput) {

        if (raoParameters.getObjectiveFunction().getUnit().equals(Unit.AMPERE)
            && raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().isDc()) {
            throw new FaraoException(format("Objective function %s cannot be calculated with a DC default sensitivity engine", raoParameters.getObjectiveFunction().toString()));
        }

        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            if (raoInput.getGlskProvider() == null) {
                throw new FaraoException(format("Objective function %s requires glsks", raoParameters.getObjectiveFunction()));
            }
            if (raoParameters.getRelativeMarginPtdfBoundaries().isEmpty()) {
                throw new FaraoException(format("Objective function %s requires a config with a non empty boundary set", raoParameters.getObjectiveFunction()));
            }
        }

        if ((raoParameters.isRaoWithLoopFlowLimitation()
            || raoParameters.getObjectiveFunction().doesRequirePtdf())
            && (raoInput.getReferenceProgram() == null)) {
            LOGGER.info("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork(), raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters()));
        }

        if (raoParameters.isRaoWithLoopFlowLimitation() && (Objects.isNull(raoInput.getReferenceProgram()) || Objects.isNull(raoInput.getGlskProvider()))) {
            String msg = format(
                "Loopflow computation cannot be performed CRAC %s because it lacks a ReferenceProgram or a GlskProvider",
                raoInput.getCrac().getId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }

    public static SystematicSensitivityInterface createSystematicSensitivityInterface(RaoParameters raoParameters,
                                                                                      Set<RangeAction> rangeActions,
                                                                                      Set<BranchCnec> cnecs,
                                                                                      boolean withPtdfSensitivitiesForLoopFlows,
                                                                                      ZonalData<LinearGlsk> glskProvider,
                                                                                      Set<BranchCnec> loopflowCnecs) {

        Set<Unit> flowUnits = new HashSet<>();
        flowUnits.add(Unit.MEGAWATT);
        if (raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().isDc()) {
            flowUnits.add(Unit.AMPERE);
        }

        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface
            .builder()
            .withDefaultParameters(raoParameters.getDefaultSensitivityAnalysisParameters())
            .withFallbackParameters(raoParameters.getFallbackSensitivityAnalysisParameters())
            .withRangeActionSensitivities(rangeActions, cnecs, flowUnits);

        if (withPtdfSensitivitiesForLoopFlows) {
            builder.withPtdfSensitivities(glskProvider, loopflowCnecs, flowUnits);
        }

        return builder.build();
    }

    public static ObjectiveFunctionEvaluator createObjectiveFunction(Set<BranchCnec> cnecs,
                                                                     Set<BranchCnec> loopflowCnecs,
                                                                     Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW,
                                                                     CnecResults initialCnecResults,
                                                                     Set<String> countriesNotToOptimize,
                                                                     RaoParameters raoParameters) {
        switch (raoParameters.getObjectiveFunction()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
            case MAX_MIN_RELATIVE_MARGIN_IN_AMPERE:
                return new MinMarginObjectiveFunction(cnecs, loopflowCnecs, prePerimeterMarginsInAbsoluteMW,
                        initialCnecResults.getAbsolutePtdfSums(), initialCnecResults.getFlowsInA(), initialCnecResults.getLoopflowsInMW(), countriesNotToOptimize, raoParameters);
            case MAX_MIN_MARGIN_IN_MEGAWATT:
            case MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT:
                return new MinMarginObjectiveFunction(cnecs, loopflowCnecs, prePerimeterMarginsInAbsoluteMW,
                        initialCnecResults.getAbsolutePtdfSums(), initialCnecResults.getFlowsInMW(), initialCnecResults.getLoopflowsInMW(), countriesNotToOptimize, raoParameters);
            default:
                throw new NotImplementedException("Not implemented objective function");
        }
    }

    public static List<BranchCnec> getMostLimitingElements(Set<BranchCnec> cnecs, String variantId, Unit unit, boolean relativePositiveMargins, int numberOfElements) {
        List<BranchCnec> sortedCnecs = cnecs.stream().
                filter(BranchCnec::isOptimized).
                sorted(Comparator.comparingDouble(cnec -> computeCnecMargin(cnec, variantId, unit, relativePositiveMargins))).
                collect(Collectors.toList());
        if (sortedCnecs.isEmpty()) {
            // There are only pure MNECs
            sortedCnecs = cnecs.stream().
                    sorted(Comparator.comparingDouble(cnec -> computeCnecMargin(cnec, variantId, unit, relativePositiveMargins))).
                    collect(Collectors.toList());
        }
        return sortedCnecs.subList(0, Math.min(numberOfElements, sortedCnecs.size()));
    }

    public static BranchCnec getMostLimitingElement(Set<BranchCnec> cnecs, String variantId, Unit unit, boolean relativePositiveMargins) {
        return getMostLimitingElements(cnecs, variantId, unit, relativePositiveMargins, 1).get(0);
    }

    public static double computeCnecMargin(BranchCnec cnec, String variantId, Unit unit, boolean relativePositiveMargins) {
        CnecResult cnecResult = ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(variantId);
        unit.checkPhysicalParameter(PhysicalParameter.FLOW);
        double actualValue = unit.equals(Unit.MEGAWATT) ? cnecResult.getFlowInMW() : cnecResult.getFlowInA();
        double absoluteMargin = cnec.computeMargin(actualValue, Side.LEFT, unit);
        if (relativePositiveMargins && (absoluteMargin > 0)) {
            return absoluteMargin / ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(variantId).getAbsolutePtdfSum();
        } else {
            return absoluteMargin;
        }
    }

    public static double getBranchFlowUnitMultiplier(BranchCnec cnec, Side voltageSide, Unit unitFrom, Unit unitTo) {
        if (unitFrom == unitTo) {
            return 1;
        }
        double nominalVoltage = cnec.getNominalVoltage(voltageSide);
        if (unitFrom == Unit.MEGAWATT && unitTo == Unit.AMPERE) {
            return 1000 / (nominalVoltage * Math.sqrt(3));
        } else if (unitFrom == Unit.AMPERE && unitTo == Unit.MEGAWATT) {
            return nominalVoltage * Math.sqrt(3) / 1000;
        } else {
            throw new FaraoException("Only conversions between MW and A are supported.");
        }
    }

    public static Set<BranchCnec> computePerimeterCnecs(Crac crac, Set<State> perimeter) {
        if (perimeter != null) {
            Set<BranchCnec> cnecs = new HashSet<>();
            perimeter.forEach(state -> cnecs.addAll(crac.getBranchCnecs(state)));
            return cnecs;
        } else {
            return  crac.getBranchCnecs();
        }
    }
}
