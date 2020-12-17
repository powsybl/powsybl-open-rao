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
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_util.CracCleaner;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramBuilder;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFLowsParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFlows;
import com.farao_community.farao.rao_commons.objective_function_evaluator.MinMarginObjectiveFunction;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.*;
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.*;
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
        initCrac(raoInput.getCrac(), raoInput.getNetwork());
    }

    public static void initNetwork(Network network, String networkVariantId) {
        network.getVariantManager().setWorkingVariant(networkVariantId);
        UcteAliasesCreation.createAliases(network);
    }

    public static void initCrac(Crac crac, Network network) {
        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);
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
            if (raoParameters.getPtdfBoundaries().isEmpty()) {
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

    public static SystematicSensitivityInterface createSystematicSensitivityInterface(RaoParameters raoParameters, RaoData raoData, boolean withPtdfSensitivitiesForLoopFlows) {

        Set<Unit> flowUnits = new HashSet<>();
        flowUnits.add(Unit.MEGAWATT);
        if (!raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().isDc()) {
            flowUnits.add(Unit.AMPERE);
        }

        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface
            .builder()
            .withDefaultParameters(raoParameters.getDefaultSensitivityAnalysisParameters())
            .withFallbackParameters(raoParameters.getFallbackSensitivityAnalysisParameters())
            .withRangeActionSensitivities(raoData.getAvailableRangeActions(), raoData.getCnecs(), flowUnits);

        if (raoParameters.isRaoWithLoopFlowLimitation() && withPtdfSensitivitiesForLoopFlows) {
            builder.withPtdfSensitivities(raoData.getGlskProvider(), raoData.getLoopflowCnecs(), flowUnits);
        }

        return builder.build();
    }

    public static IteratingLinearOptimizer createLinearOptimizer(RaoParameters raoParameters, SystematicSensitivityInterface systematicSensitivityInterface) {
        List<ProblemFiller> fillers = new ArrayList<>();
        fillers.add(new CoreProblemFiller(raoParameters.getPstSensitivityThreshold()));
        if (raoParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_AMPERE)
            || raoParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinMarginFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getPstPenaltyCost()));
            fillers.add(new MnecFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost(), raoParameters.getMnecConstraintAdjustmentCoefficient()));
        } else if (raoParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
            || raoParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinRelativeMarginFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getPstPenaltyCost(), raoParameters.getNegativeMarginObjectiveCoefficient(), raoParameters.getPtdfSumLowerBound()));
            fillers.add(new MnecFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost(), raoParameters.getMnecConstraintAdjustmentCoefficient()));
        }
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            // TO DO : add relative margins to IteratingLinearOptimizerWithLoopFlows
            // or merge IteratingLinearOptimizerWithLoopFlows with IteratingLinearOptimizer
            fillers.add(createMaxLoopFlowFiller(raoParameters));
            return new IteratingLinearOptimizerWithLoopFlows(fillers, systematicSensitivityInterface,
                createObjectiveFunction(raoParameters), createIteratingLoopFlowsParameters(raoParameters));
        } else {
            return new IteratingLinearOptimizer(fillers, systematicSensitivityInterface, createObjectiveFunction(raoParameters), createIteratingParameters(raoParameters));
        }
    }

    private static MaxLoopFlowFiller createMaxLoopFlowFiller(RaoParameters raoParameters) {
        return new MaxLoopFlowFiller(raoParameters.getLoopFlowConstraintAdjustmentCoefficient(), raoParameters.getLoopFlowViolationCost(), raoParameters.getLoopFlowApproximationLevel());
    }

    private static IteratingLinearOptimizerParameters createIteratingParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerParameters(raoParameters.getMaxIterations(), raoParameters.getFallbackOverCost());
    }

    private static IteratingLinearOptimizerWithLoopFLowsParameters createIteratingLoopFlowsParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerWithLoopFLowsParameters(raoParameters.getMaxIterations(),
            raoParameters.getFallbackOverCost(), raoParameters.getLoopFlowApproximationLevel(), raoParameters.getLoopFlowViolationCost());
    }

    public static ObjectiveFunctionEvaluator createObjectiveFunction(RaoParameters raoParameters) {
        switch (raoParameters.getObjectiveFunction()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
            case MAX_MIN_MARGIN_IN_MEGAWATT:
            case MAX_MIN_RELATIVE_MARGIN_IN_AMPERE:
            case MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT:
                return new MinMarginObjectiveFunction(raoParameters);
            default:
                throw new NotImplementedException("Not implemented objective function");
        }
    }

    public static List<Optional<Country>> getCnecLocation(Cnec cnec, Network network) {
        return getNetworkElementLocation(cnec.getNetworkElement(), network);
    }

    private static List<Optional<Country>> getNetworkElementLocation(NetworkElement networkElement, Network network) {
        Identifiable<?> ne = network.getIdentifiable(networkElement.getId());
        if (ne instanceof Line) {
            Line line = (Line) ne;
            return Arrays.asList(line.getTerminal1().getVoltageLevel().getSubstation().getCountry(), line.getTerminal2().getVoltageLevel().getSubstation().getCountry());
        } else if (ne instanceof TwoWindingsTransformer) {
            TwoWindingsTransformer transformer = (TwoWindingsTransformer) ne;
            return Arrays.asList(transformer.getTerminal1().getVoltageLevel().getSubstation().getCountry(), transformer.getTerminal2().getVoltageLevel().getSubstation().getCountry());
        } else if (ne instanceof Switch) {
            return Arrays.asList(((Switch) ne).getVoltageLevel().getSubstation().getCountry());
        } else {
            throw new NotImplementedException("Don't know how to fgure out the location of " + ne.getId() + " of type " + ne.getClass());
        }
    }

    public static List<Optional<Country>> getNetworkActionLocation(NetworkAction networkAction, Network network) {
        List<List<Optional<Country>>> listOfLists = networkAction.getNetworkElements().stream()
                .map(networkElement -> getNetworkElementLocation(networkElement, network))
                .collect(Collectors.toList());
        List<Optional<Country>> result = new ArrayList<>();
        listOfLists.forEach(result::addAll);
        return result;
    }

    public static Cnec getMostLimitingElement(Set<BranchCnec> cnecs, String variantId, Unit unit, boolean relativePositiveMargins) {
        List<Cnec> sortedCnecs = cnecs.stream().
                filter(Cnec::isOptimized).
                sorted(Comparator.comparingDouble(cnec -> computeCnecMargin(cnec, variantId, unit, relativePositiveMargins))).
                collect(Collectors.toList());
        return sortedCnecs.get(0);
    }

    public static double computeCnecMargin(BranchCnec cnec, String variantId, Unit unit, boolean relativePositiveMargins) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(variantId);
        unit.checkPhysicalParameter(PhysicalParameter.FLOW);
        double actualValue = unit.equals(Unit.MEGAWATT) ? cnecResult.getFlowInMW() : cnecResult.getFlowInA();
        double absoluteMargin = cnec.computeMargin(actualValue, Side.LEFT, unit);
        if (relativePositiveMargins && (absoluteMargin > 0)) {
            return absoluteMargin / cnec.getExtension(CnecResultExtension.class).getVariant(variantId).getAbsolutePtdfSum();
        } else {
            return absoluteMargin;
        }
    }
}
