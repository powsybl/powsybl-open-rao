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
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.impl.*;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmHvdcHelper;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.AutomatonSimulator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.data.crac.io.commons.iidm.IidmHvdcHelper.setActivePowerSetpointOnHvdcLine;
import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class HvdcUtils {

    /**
     * Add to the crac a network action that deactivate AC emulation for each HVDC line in AC emulation mode
     * that has at least one hvdc range action associated
     */
    public static void addNetworkActionAssociatedWithHvdcRangeAction(Crac crac, Network network) {
        // for each hvdc range action
        crac.getHvdcRangeActions().forEach(hvdcRangeAction -> {
            // get associated hvdc line id
            String hvdcLineId = hvdcRangeAction.getNetworkElement().getId();

            // Check if an AC emulation deactivation network action has already been created
            Set<NetworkAction> acEmulationDeactivationActionOnHvdcLine = crac.getNetworkActions().stream()
                .filter(ra -> ra.getElementaryActions().stream().allMatch(action -> action instanceof HvdcAction))
                .filter(ra -> ra.getElementaryActions().stream().allMatch(action ->
                    ((HvdcAction) action).getHvdcId().equals(hvdcLineId)
                )).collect(Collectors.toSet());

            // get HVDC line's HvdcAngleDroopActivePowerControl extension
            HvdcAngleDroopActivePowerControl hvdcAngleDoopActivePowerControl = IidmHvdcHelper.getHvdcLine(network, hvdcLineId).getExtension(HvdcAngleDroopActivePowerControl.class);

            // if AC emulation is activated on HVDC line
            if (hvdcAngleDoopActivePowerControl != null && hvdcAngleDoopActivePowerControl.isEnabled()) {

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

    // Add all the usage rules of the range action to the network action
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

    static void updateHvdcRangeActionInitialSetpoint(Crac crac, Network network, RaoParameters raoParameters) {
        // Run load flow to update flow on all the lines of the network
        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = new LoadFlowAndSensitivityParameters();
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)){
            loadFlowAndSensitivityParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters();
        }

        Set<HvdcRangeActionImpl> hvdcRasOnHvdcLinesInAcEmulation = getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network);
        Map<HvdcRangeAction, Double> activePowerSetpoints = new HashMap<>();
        if (!hvdcRasOnHvdcLinesInAcEmulation.isEmpty()) {
            activePowerSetpoints = runLoadFlowAndUpdateHvdcActiveSetpoint(
                network,
                crac.getPreventiveState(),
                loadFlowAndSensitivityParameters.getLoadFlowProvider(),
                loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters().getLoadFlowParameters(),
                hvdcRasOnHvdcLinesInAcEmulation
            );
        }

        activePowerSetpoints.forEach((hvdcRa, activePowerSetpoint) -> {
            HvdcRangeActionImpl hvdcRaImpl = (HvdcRangeActionImpl) hvdcRa;
            hvdcRaImpl.setInitialSetpoint(activePowerSetpoint);
        });



    }

    public static Set<HvdcRangeActionImpl> getHvdcRangeActionsOnHvdcLineInAcEmulation(Set<HvdcRangeAction> hvdcRangeActions, Network network) {
        // return all the HVDC range action in the CRAC defined on HVDC line in AC emulation in the network
        return hvdcRangeActions.stream()
            .map(HvdcRangeActionImpl.class::cast)
            .filter(hvdcRangeAction -> hvdcRangeAction.isAngleDroopActivePowerControlEnabled(network))
            .collect(Collectors.toSet());
    }

    public static Map<HvdcRangeAction, Double> runLoadFlowAndUpdateHvdcActiveSetpoint(
        Network network,
        State optimizationState,
        String loadFlowProvider,
        LoadFlowParameters loadFlowParameters,
        Set<HvdcRangeActionImpl> hvdcRangeActionsWithHvdcLineInAcEmulation
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

            // Disable AngleDroopActivePowerControl on HVDCs and set active power set-points
        hvdcRangeActionsWithHvdcLineInAcEmulation.forEach(hvdcRa -> {
            String hvdcLineId = hvdcRa.getNetworkElement().getId();
            HvdcLine hvdcLine = IidmHvdcHelper.getHvdcLine(network, hvdcLineId);
            double activePowerSetpoint = controls.get(hvdcLineId);

            // Valid only if not NaN and within admissible range
            boolean isValid = !Double.isNaN(activePowerSetpoint)
                    && activePowerSetpoint >= hvdcRa.getMinAdmissibleSetpoint(activePowerSetpoint)
                    && activePowerSetpoint <= hvdcRa.getMaxAdmissibleSetpoint(activePowerSetpoint);

            if (isValid) {
                    activePowerSetpoints.put(hvdcRa, activePowerSetpoint);
                    setActivePowerSetpointOnHvdcLine(hvdcLine, activePowerSetpoint);
            } else {
                TECHNICAL_LOGS.info(String.format(
                    "HVDC range action %s could not be activated because its initial set-point "
                            + "(%.1f) does not fall within its allowed range (%.1f - %.1f)",
                    hvdcRa.getId(),
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


    /**
     * Disables AC emulation on a given HVDC line by applying the corresponding deactivation action,
     * updates the topo simulation result by adding said network action
     */
    public static void disableAcEmulationAndSetHvdcActivePowerSetpoint(Network network, Crac crac, AutomatonSimulator.TopoAutomatonSimulationResult topoSimulationResult, String hvdcLineId, double activePowerSetpoint) {
        TECHNICAL_LOGS.debug("Disabling HvdcAngleDroopActivePowerControl on HVDC line {}", hvdcLineId, activePowerSetpoint);
        // get AC emulation deactivation action that acts on hvdc line
        NetworkAction acEmulationDeactivationAction = getAcEmulationDeactivationActionOnHvdcLine(crac, hvdcLineId);
        // deactivate AC emulation using the acEmulationDeactivationAction found above
        acEmulationDeactivationAction.apply(network);
        // add network action to topoSimulationResult !
        topoSimulationResult.addActivatedNetworkActions(Set.of(acEmulationDeactivationAction));
    }


    /**
     * Retrieves the AC emulation deactivation {@link NetworkAction} associated with a specific HVDC line
     * from the given {@link Crac} instance. The method works as follows:
     * <ul>
     *     <li>It filters the set of all network actions in the CRAC to find those whose associated network elements
     *     match exactly the provided HVDC line ID.</li>
     *     <li>It further restricts the selection to actions composed exclusively of {@link HvdcAction} elementary actions.</li>
     *     <li>There should only be one acEmulationDeactivationAction per HVDC line; if not, it logs a warning.</li>
     * </ul>d
     */
    private static NetworkAction getAcEmulationDeactivationActionOnHvdcLine(Crac crac, String hvdcLineId) {
        Set<NetworkAction> acEmulationDeactivationActionsOnHvdcLine = crac.getNetworkActions().stream()
            .filter(ra -> ra.getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet()).equals(Set.of(hvdcLineId)))
            .filter(ra -> ra.getElementaryActions().stream()
                .allMatch(ea -> ea instanceof HvdcAction)).collect(Collectors.toSet());

        if (acEmulationDeactivationActionsOnHvdcLine.size() != 1) {
            TECHNICAL_LOGS.warn("Expected exactly one acEmulationDeactivationAction for HVDC line {}, but found {}.", hvdcLineId, acEmulationDeactivationActionsOnHvdcLine.size());
        }

        return acEmulationDeactivationActionsOnHvdcLine.iterator().next();
    }

}