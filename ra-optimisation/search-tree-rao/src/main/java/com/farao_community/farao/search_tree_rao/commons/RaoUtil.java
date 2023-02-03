/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountry;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramBuilder;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {
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
            FaraoLoggerProvider.BUSINESS_WARNS.warn("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork(), raoParameters.getLoadFlowProvider(), raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters()));
        }

        if (raoParameters.isRaoWithLoopFlowLimitation() && (Objects.isNull(raoInput.getReferenceProgram()) || Objects.isNull(raoInput.getGlskProvider()))) {
            String msg = format(
                "Loopflow computation cannot be performed on CRAC %s because it lacks a ReferenceProgram or a GlskProvider",
                raoInput.getCrac().getId());
            FaraoLoggerProvider.BUSINESS_LOGS.error(msg);
            throw new FaraoException(msg);
        }
    }

    public static double getFlowUnitMultiplier(FlowCnec cnec, Side voltageSide, Unit unitFrom, Unit unitTo) {
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

    /* Method used to make sure the LP is reproducible. This basically rounds the least significant bits of a double.
     Let's say a double has 10 precision bits (in reality, 52)
     We take an initial double:
       .............//////////.....
     To which we add a "bigger" double :
       .........\\\\\\\\\\..........
      =>
       .........\\\\||||||..........
       (we "lose" the least significant bits of the first double because the sum double doesn't have enough precision to show them)
     Then we substract the same "bigger" double:
       .............//////..........
       We get back our original bits for the most significant part, but the least significant bits are still gone.
     */

    public static double roundDouble(double value, int numberOfBitsToRoundOff) {
        double t = value * (1L << numberOfBitsToRoundOff);
        if (t != Double.POSITIVE_INFINITY && value != Double.NEGATIVE_INFINITY && !Double.isNaN(t)) {
            return value - t + t;
        }
        return value;
    }

    /**
     * Returns true if a remedial action is available depending on its usage rules
     * If it has a OnFlowConstraint usage rule, then the margins are needed
     */
    public static boolean isRemedialActionAvailable(RemedialAction<?> remedialAction, State optimizedState, FlowResult flowResult, Set<FlowCnec> perimeterCnecs, Network network, Unit marginUnit) {
        switch (remedialAction.getUsageMethod(optimizedState)) {
            case AVAILABLE:
                return true;
            case TO_BE_EVALUATED:
                return remedialAction.getUsageRules().stream()
                    .filter(OnFlowConstraint.class::isInstance)
                    .anyMatch(usageRule -> isOnFlowConstraintAvailable((OnFlowConstraint) usageRule, optimizedState, flowResult, marginUnit))
                    || remedialAction.getUsageRules().stream()
                    .filter(OnFlowConstraintInCountry.class::isInstance)
                    .anyMatch(usageRule -> isOnFlowConstraintInCountryAvailable((OnFlowConstraintInCountry) usageRule, optimizedState, flowResult, perimeterCnecs, network, marginUnit));
            default:
                return false;
        }
    }

    /**
     * Returns true if a OnFlowConstraint usage rule is verified, ie if the associated CNEC has a negative margin
     * It needs a FlowResult to get the margin of the flow cnec
     */
    public static boolean isOnFlowConstraintAvailable(OnFlowConstraint onFlowConstraint, State optimizedState, FlowResult flowResult, Unit marginUnit) {
        if (!onFlowConstraint.getUsageMethod(optimizedState).equals(UsageMethod.TO_BE_EVALUATED)) {
            return false;
        } else {
            // We need to know the unit of the objective function, because a negative margin in A can be positive in MW
            // given different approximations, and vice versa
            return flowResult.getMargin(onFlowConstraint.getFlowCnec(), marginUnit) <= 0;
        }
    }

    /**
     * Returns true if a OnFlowConstraintInCountry usage rule is verified, ie if any CNEC of the country has a negative margin
     * It needs a FlowResult to get the margin of the flow cnecs
     */
    public static boolean isOnFlowConstraintInCountryAvailable(OnFlowConstraintInCountry onFlowConstraintInCountry, State optimizedState, FlowResult flowResult, Set<FlowCnec> perimeterCnecs, Network network, Unit marginUnit) {
        if (!onFlowConstraintInCountry.getUsageMethod(optimizedState).equals(UsageMethod.TO_BE_EVALUATED)) {
            return false;
        } else {
            Map<Instant, Set<Instant>> allowedCnecInstantPerRaInstant = Map.of(
                Instant.PREVENTIVE, Set.of(Instant.PREVENTIVE, Instant.OUTAGE, Instant.CURATIVE),
                Instant.AUTO, Set.of(Instant.AUTO),
                Instant.CURATIVE, Set.of(Instant.CURATIVE)
            );
            // We need to know the unit of the objective function, because a negative margin in A can be positive in MW
            // given different approximations, and vice versa
            return perimeterCnecs.stream()
                .filter(cnec -> allowedCnecInstantPerRaInstant.get(onFlowConstraintInCountry.getInstant()).contains(cnec.getState().getInstant()))
                .filter(cnec -> isCnecInCountry(cnec, onFlowConstraintInCountry.getCountry(), network))
                .anyMatch(cnec -> flowResult.getMargin(cnec, marginUnit) <= 0);
        }
    }

    public static boolean isCnecInCountry(Cnec<?> cnec, Country country, Network network) {
        return cnec.getLocation(network).stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .anyMatch(cnecCountry -> cnecCountry.equals(country));
    }

    /**
     * Returns the range action from optimizationContext that is available on the latest state
     * strictly before the given state, and that acts on the same network element as rangeAction.
     */
    public static Pair<RangeAction<?>, State> getLastAvailableRangeActionOnSameNetworkElement(OptimizationPerimeter optimizationContext, RangeAction<?> rangeAction, State state) {

        if (state.isPreventive() || state.equals(optimizationContext.getMainOptimizationState())) {
            // no previous instant
            return null;
        } else if (state.getInstant().equals(Instant.CURATIVE)) {

            // look if a preventive range action acts on the same network elements
            State preventiveState = optimizationContext.getMainOptimizationState();

            if (preventiveState.isPreventive()) {
                Optional<RangeAction<?>> correspondingRa = optimizationContext.getRangeActionsPerState().get(preventiveState).stream()
                    .filter(ra -> ra.getId().equals(rangeAction.getId()) || ra.getNetworkElements().equals(rangeAction.getNetworkElements()))
                    .findAny();

                if (correspondingRa.isPresent()) {
                    return Pair.of(correspondingRa.get(), preventiveState);
                }
            }
            return null;
        } else {
            throw new FaraoException("Linear optimization does not handle range actions which are neither PREVENTIVE nor CURATIVE.");
        }
    }

    public static double getLargestCnecThreshold(Set<FlowCnec> flowCnecs, Unit unit) {
        return flowCnecs.stream().filter(Cnec::isOptimized)
            .map(flowCnec ->
                flowCnec.getMonitoredSides().stream().map(side ->
                    Math.max(Math.abs(flowCnec.getUpperBound(side, unit).orElse(0.)), Math.abs(flowCnec.getLowerBound(side, unit).orElse(0.)))).max(Double::compare).orElse(0.))
            .max(Double::compare)
            .orElse(0.);
    }
}
