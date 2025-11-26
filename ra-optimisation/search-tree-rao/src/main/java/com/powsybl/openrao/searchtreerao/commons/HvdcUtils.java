/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.action.HvdcAction;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.impl.*;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmHvdcHelper;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.data.crac.io.commons.iidm.IidmHvdcHelper.setActivePowerSetpointOnHvdcLine;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class HvdcUtils {

    private HvdcUtils() {
    }

    /**
     * Add to the CRAC a network action that deactivate AC emulation for each HVDC line in AC emulation mode
     * that has at least one HVDC range action associated
     */
    public static void addNetworkActionAssociatedWithHvdcRangeAction(Crac crac, Network network) {
        // for each HVDC range action
        crac.getHvdcRangeActions().forEach(hvdcRangeAction -> {
            // get associated HVDC line id
            String hvdcLineId = hvdcRangeAction.getNetworkElement().getId();

            // Check if an AC emulation deactivation network action has already been created
            Set<NetworkAction> acEmulationDeactivationActionOnHvdcLine = crac.getNetworkActions().stream()
                .filter(ra -> ra.getElementaryActions().stream().allMatch(action -> action instanceof HvdcAction))
                .filter(ra -> ra.getElementaryActions().stream().allMatch(action ->
                    ((HvdcAction) action).getHvdcId().equals(hvdcLineId)
                )).collect(Collectors.toSet());

            // if AC emulation is activated on HVDC line
            if (hvdcRangeAction.isAngleDroopActivePowerControlEnabled(network)) {

                String networkActionId = String.format("%s_%s", "acEmulationDeactivation", hvdcLineId);
                if (acEmulationDeactivationActionOnHvdcLine.isEmpty()) {
                    // If the network action doesn't exist yet
                    // create a network action hvdcLineId_acEmulationDeactivation
                    // with one elementary action (an acEmulationDeactivationAction that uses under the hood powsybl's hvdcAction)
                    // and the same usage rule as the HVDC range action
                    NetworkActionAdder acEmulationDeactivationActionAdder = crac.newNetworkAction()
                        .withId(networkActionId)
                        .withOperator(hvdcRangeAction.getOperator())
                        .newAcEmulationDeactivationAction()
                        .withNetworkElement(hvdcLineId)
                        .add();
                    addAllUsageRules(hvdcRangeAction, acEmulationDeactivationActionAdder);
                    acEmulationDeactivationActionAdder.add();
                } else {
                    // If the network action already exists, just update the usage rules by adding hvdcRangeAction's ones.
                    NetworkAction acEmulationDeactivationAction = acEmulationDeactivationActionOnHvdcLine.iterator().next();
                    hvdcRangeAction.getUsageRules().stream().forEach(
                        usageRule -> acEmulationDeactivationAction.addUsageRule(usageRule)
                    );
                }
            }
        });
    }

    /***
     * Add all the usage rules of the range action to the network action
     *
     * @param hvdcRangeAction the range action for which the usage rules are added to the network action
     * @param acEmulationDeactivationActionAdder the network action adder that will be used to add the usage rules
     */
    static void addAllUsageRules(HvdcRangeAction hvdcRangeAction, NetworkActionAdder acEmulationDeactivationActionAdder) {
        hvdcRangeAction.getUsageRules().forEach(
            usageRule -> {
                if (usageRule.getClass().equals(OnInstantImpl.class)) {
                    acEmulationDeactivationActionAdder
                        .newOnInstantUsageRule()
                        .withInstant(usageRule.getInstant().getId())
                        .add();
                } else if (usageRule.getClass().equals(OnConstraintImpl.class)) {
                    OnConstraint<?> onConstraint = (OnConstraint<?>) usageRule;
                    acEmulationDeactivationActionAdder.newOnConstraintUsageRule()
                        .withInstant(onConstraint.getInstant().getId())
                        .withCnec(onConstraint.getCnec().getId())
                        .add();
                } else if (usageRule.getClass().equals(OnContingencyStateImpl.class)) {
                    OnContingencyState onContingencyState = (OnContingencyState) usageRule;
                    acEmulationDeactivationActionAdder.newOnContingencyStateUsageRule()
                        .withContingency(onContingencyState.getContingency().getId())
                        .withInstant(onContingencyState.getInstant().getId())
                        .add();
                } else if (usageRule.getClass().equals(OnFlowConstraintInCountryImpl.class)) {
                    OnFlowConstraintInCountry onFlowConstraintInCountry = (OnFlowConstraintInCountry) usageRule;
                    acEmulationDeactivationActionAdder.newOnFlowConstraintInCountryUsageRule()
                        .withCountry(onFlowConstraintInCountry.getCountry())
                        .withInstant(onFlowConstraintInCountry.getInstant().getId())
                        .withContingency(onFlowConstraintInCountry.getContingency().get().getId())
                        .add();
                }
            }
        );
    }

    /**
     * Run a load flow to update the initial set-points of the HVDC range actions assiociated to HVDC line in AC emulation mode
     */
    static void updateHvdcRangeActionInitialSetpoint(Crac crac, Network network, RaoParameters raoParameters) {
        // Run load flow to update flow on all the lines of the network
        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = new LoadFlowAndSensitivityParameters();
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            loadFlowAndSensitivityParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters();
        }

        Set<HvdcRangeAction> hvdcRasOnHvdcLinesInAcEmulation = getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network);
        Map<HvdcRangeAction, Double> activePowerSetpoints = new HashMap<>();
        if (!hvdcRasOnHvdcLinesInAcEmulation.isEmpty()) {
            activePowerSetpoints = runLoadFlowAndUpdateHvdcActivePowerSetpoint(
                network,
                crac.getPreventiveState(),
                loadFlowAndSensitivityParameters.getLoadFlowProvider(),
                loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters().getLoadFlowParameters(),
                hvdcRasOnHvdcLinesInAcEmulation
            );
        }

        activePowerSetpoints.forEach((hvdcRa, activePowerSetpoint) -> {
            hvdcRa.setInitialSetpoint(activePowerSetpoint);
        });
    }

    /**
     * Get all the HVDC range actions in the input hvdcRangeActions set defined on a HVDC line in AC emulation in the network
     *
     * @param hvdcRangeActions set of HVDC range actions to filter
     * @param network
     * @return
     */
    public static Set<HvdcRangeAction> getHvdcRangeActionsOnHvdcLineInAcEmulation(Set<HvdcRangeAction> hvdcRangeActions, Network network) {
        return hvdcRangeActions.stream()
            .filter(hvdcRangeAction -> hvdcRangeAction.isAngleDroopActivePowerControlEnabled(network))
            .collect(Collectors.toSet());
    }

    /**
     * Filter out from rangeActions set all the HVDC range actions that are associated with HVDC Line AC Emulation
     * @param rangeActions
     * @param network
     * @return
     */
    public static Set<RangeAction<?>> filterOutHvdcRangeActionsOnHvdcLineInAcEmulation(Set<RangeAction<?>> rangeActions, Network network) {
        return rangeActions.stream().filter(ra -> {
                    if (ra instanceof HvdcRangeAction) {
                        return !((HvdcRangeAction) ra).isAngleDroopActivePowerControlEnabled(network);
                    }
                    return true;
                }).collect(Collectors.toSet());
    }


    /**
     * Run load flow and update the active power setpoints of the HVDC range actions associated with HVDC lines in AC emulation mode
     *
     * @param network
     * @param optimizationState used to get contingency to apply
     * @param loadFlowProvider
     * @param loadFlowParameters
     * @param hvdcRangeActionsWithHvdcLineInAcEmulation
     * @return A map of HVDC Range Action to their updated computed active power setpoints
     */
    public static Map<HvdcRangeAction, Double> runLoadFlowAndUpdateHvdcActivePowerSetpoint(
        Network network,
        State optimizationState,
        String loadFlowProvider,
        LoadFlowParameters loadFlowParameters,
        Set<HvdcRangeAction> hvdcRangeActionsWithHvdcLineInAcEmulation
    ) {

        Map<HvdcRangeAction, Double> activePowerSetpoints = new HashMap<>();

        TECHNICAL_LOGS.debug(
            "Running load-flow computation to access HvdcAngleDroopActivePowerControl set-point values."
        );

        Map<String, Double> controls = computeHvdcAngleDroopActivePowerControlValues(
                network,
                optimizationState,
                loadFlowProvider,
                loadFlowParameters
        );

        // Set active power set-points
        hvdcRangeActionsWithHvdcLineInAcEmulation.forEach(hvdcRa -> {
            String hvdcLineId = hvdcRa.getNetworkElement().getId();
            HvdcLine hvdcLine = IidmHvdcHelper.getHvdcLine(network, hvdcLineId);
            double activePowerSetpoint = controls.get(hvdcLineId);

            // Valid only if not NaN and within admissible range
            boolean isValid = !Double.isNaN(activePowerSetpoint) // NaN if HVDC line is disconnected
                    && activePowerSetpoint >= hvdcRa.getMinAdmissibleSetpoint(activePowerSetpoint)
                    && activePowerSetpoint <= hvdcRa.getMaxAdmissibleSetpoint(activePowerSetpoint);

            if (isValid) {
                TECHNICAL_LOGS.debug(String.format("" +
                    "HVDC line %s active power setpoint is set to (%.1f)", hvdcLineId, activePowerSetpoint));

                activePowerSetpoints.put(hvdcRa, activePowerSetpoint);
                setActivePowerSetpointOnHvdcLine(hvdcLine, activePowerSetpoint);
            } else {
                TECHNICAL_LOGS.info(String.format(
                    "HVDC line %s active setpoint could not be updated because its new set-point "
                            + "(%.1f) does not fall within its allowed range (%.1f - %.1f)",
                    hvdcLineId,
                    activePowerSetpoint,
                    hvdcRa.getMinAdmissibleSetpoint(activePowerSetpoint),
                    hvdcRa.getMaxAdmissibleSetpoint(activePowerSetpoint)
                ));
            }
        });

        return activePowerSetpoints;
    }

    /**
     * Computes the active power setpoints for HVDC lines with {@code HvdcAngleDroopActivePowerControl} enabled,
     * by creating a temporary network variant, applying the given contingency state, and running a load-flow calculation.
     * Restores the original network state after computation.
     *
     * @return A map of HVDC line IDs to their computed active power setpoints
     * @throws OpenRaoException If a required contingency is invalid or cannot be applied
     */
    public static Map<String, Double> computeHvdcAngleDroopActivePowerControlValues(Network network, State state, String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        // Create a temporary variant to apply contingency and compute load-flow on
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String tmpVariant = RandomizedString.getRandomizedString("HVDC_LF", network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(initialVariantId, tmpVariant);
        network.getVariantManager().setWorkingVariant(tmpVariant);

        // Apply contingency and compute load-flow
        if (state.getContingency().isPresent()) {
            Contingency contingency = state.getContingency().orElseThrow();
            if (!contingency.isValid(network)) {
                throw new OpenRaoException("Unable to apply contingency " + contingency.getId());
            }
            contingency.toModification().apply(network, (ComputationManager) null);
        }
        LoadFlow.find(loadFlowProvider).run(network, loadFlowParameters);

        // Compute HvdcAngleDroopActivePowerControl values of HVDC lines
        Map<String, Double> controls = network.getHvdcLineStream()
            .filter(hvdcLine -> hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class) != null)
            .collect(Collectors.toMap(com.powsybl.iidm.network.Identifiable::getId, IidmHvdcHelper::computeActivePowerSetpointOnHvdcLine));

        // Reset working variant
        network.getVariantManager().setWorkingVariant(initialVariantId);
        network.getVariantManager().removeVariant(tmpVariant);

        return controls;
    }

}
