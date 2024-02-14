/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.OnAngleConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.OnContingencyState;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertAngleCnecEquality;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertCnecNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertContingencyEquality;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertContingencyNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getNetworkFromResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
class CsaProfileSsiTest {

    private CsaProfileCracCreationContext cracCreationContext;
    private Crac crac;
    private final Network network = getNetworkFromResource("/TestCase16NodesWithShuntCompensator.zip");

    @Test
    void activateDeactivateContingency() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-1_Contingency.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();
        assertEquals(1, crac.getContingencies().size());
        assertContingencyEquality(crac.getContingencies().iterator().next(), "contingency-1", "RTE_CO1", 1, List.of("FFR1AA1  FFR2AA1  1"));
        assertContingencyNotImported(cracCreationContext, "contingency-2", ImportStatus.NOT_FOR_RAO, "contingency.mustStudy is false");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-1_Contingency.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();
        assertEquals(1, crac.getContingencies().size());
        assertContingencyEquality(crac.getContingencies().iterator().next(), "contingency-2", "RTE_CO2", 1, List.of("FFR1AA1  FFR3AA1  1"));
        assertContingencyNotImported(cracCreationContext, "contingency-1", ImportStatus.NOT_FOR_RAO, "contingency.mustStudy is false");
    }

    @Test
    void activateDeactivateRemedialAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-2_RemedialAction.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-1", remedialAction.getId());
        assertEquals("RTE_RA1", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        assertEquals(1, remedialAction.getUsageRules().size());
        assertEquals(crac.getInstant("preventive"), remedialAction.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, remedialAction.getUsageRules().iterator().next().getUsageMethod());

        assertRaNotImported(cracCreationContext, "remedial-action-2", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-2 will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-2_RemedialAction.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-2", remedialAction.getId());
        assertEquals("RTE_RA2", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("DDE3AA1  DDE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        assertEquals(1, remedialAction.getUsageRules().size());
        assertEquals(crac.getInstant("preventive"), remedialAction.getUsageRules().iterator().next().getInstant());
        assertEquals(UsageMethod.AVAILABLE, remedialAction.getUsageRules().iterator().next().getUsageMethod());

        assertRaNotImported(cracCreationContext, "remedial-action-1", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-1 will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");
    }

    @Test
    void changeTopologyActionType() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-3_ChangeTopologicalActionType.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-1", remedialAction.getId());
        assertEquals("RTE_RA1", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-3_ChangeTopologicalActionType.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-1", remedialAction.getId());
        assertEquals("RTE_RA1", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.CLOSE, ((TopologicalAction) elementaryAction).getActionType());
    }

    @Test
    void restrictPstActionRange() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-4_RestrictPSTActionRange.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getPstRangeActions().size());
        PstRangeAction remedialAction = crac.getPstRangeActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", remedialAction.getNetworkElement().getId());
        assertEquals(1, remedialAction.getRanges().size());
        assertEquals(-16, remedialAction.getRanges().iterator().next().getMinTap());
        assertEquals(16, remedialAction.getRanges().iterator().next().getMaxTap());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-4_RestrictPSTActionRange.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getPstRangeActions().size());
        remedialAction = crac.getPstRangeActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", remedialAction.getNetworkElement().getId());
        assertEquals(1, remedialAction.getRanges().size());
        assertEquals(-5, remedialAction.getRanges().iterator().next().getMinTap());
        assertEquals(10, remedialAction.getRanges().iterator().next().getMaxTap());
    }

    @Test
    void changeRotatingMachineactionSetpoint() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-5_ChangeRotatingMachineActionSetpoint.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("FFR1AA1 _generator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(75d, ((InjectionSetpoint) elementaryAction).getSetpoint());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-5_ChangeRotatingMachineActionSetpoint.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("FFR1AA1 _generator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(100d, ((InjectionSetpoint) elementaryAction).getSetpoint());
    }

    @Test
    void activateDeactivateTopologyAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-6_TopologyAction.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryAction).getActionType());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-6_TopologyAction.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals("DDE3AA1  DDE4AA1  1", ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(ActionType.CLOSE, ((TopologicalAction) elementaryAction).getActionType());
    }

    @Test
    void activateDeactivateRotatingMachineAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-7_RotatingMachineAction.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("FFR1AA1 _generator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(75d, ((InjectionSetpoint) elementaryAction).getSetpoint());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-7_RotatingMachineAction.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("FFR2AA1 _generator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(100d, ((InjectionSetpoint) elementaryAction).getSetpoint());
    }

    @Test
    void activateTapPositionAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-8_ActivateTapPositionAction.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(0, crac.getPstRangeActions().size());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-8_ActivateTapPositionAction.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getPstRangeActions().size());
        PstRangeAction remedialAction = crac.getPstRangeActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", remedialAction.getNetworkElement().getId());
        assertEquals(1, remedialAction.getRanges().size());
        assertEquals(-4, remedialAction.getRanges().iterator().next().getMinTap());
        assertEquals(8, remedialAction.getRanges().iterator().next().getMaxTap());
    }

    @Test
    void deactivateTapPositionAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-9_DeactivateTapPositionAction.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getPstRangeActions().size());
        PstRangeAction remedialAction = crac.getPstRangeActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", remedialAction.getNetworkElement().getId());
        assertEquals(1, remedialAction.getRanges().size());
        assertEquals(-4, remedialAction.getRanges().iterator().next().getMinTap());
        assertEquals(8, remedialAction.getRanges().iterator().next().getMaxTap());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-9_DeactivateTapPositionAction.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(0, crac.getPstRangeActions().size());
    }

    @Test
    void activateDeactivateShuntCompensatorModification() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-10_ShuntCompensatorModification.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        ElementaryAction elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("shunt-compensator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(1, ((InjectionSetpoint) elementaryAction).getSetpoint());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-10_ShuntCompensatorModification.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action", remedialAction.getId());
        assertEquals("RTE_RA", remedialAction.getName());

        assertEquals(1, remedialAction.getElementaryActions().size());
        elementaryAction = remedialAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals("shunt-compensator", ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(3, ((InjectionSetpoint) elementaryAction).getSetpoint());
    }

    @Test
    void activateDeactivateAllElementaryActions() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-11_ElementaryActions.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        NetworkAction remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-1", remedialAction.getId());
        assertEquals("RTE_RA1", remedialAction.getName());

        List<ElementaryAction> elementaryActions = remedialAction.getElementaryActions().stream().sorted(Comparator.comparing(ElementaryAction::toString)).toList();
        assertEquals(3, elementaryActions.size());

        assertTrue(elementaryActions.get(0) instanceof InjectionSetpoint);
        assertEquals("FFR1AA1 _generator", ((InjectionSetpoint) elementaryActions.get(0)).getNetworkElement().getId());
        assertEquals(75d, ((InjectionSetpoint) elementaryActions.get(0)).getSetpoint());

        assertTrue(elementaryActions.get(1) instanceof InjectionSetpoint);
        assertEquals("shunt-compensator", ((InjectionSetpoint) elementaryActions.get(1)).getNetworkElement().getId());
        assertEquals(1, ((InjectionSetpoint) elementaryActions.get(1)).getSetpoint());

        assertTrue(elementaryActions.get(2) instanceof TopologicalAction);
        assertEquals("BBE1AA1  BBE4AA1  1", ((TopologicalAction) elementaryActions.get(2)).getNetworkElement().getId());
        assertEquals(ActionType.OPEN, ((TopologicalAction) elementaryActions.get(2)).getActionType());

        assertRaNotImported(cracCreationContext, "remedial-action-2", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-2 will not be imported because it has no elementary action");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-11_ElementaryActions.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getNetworkActions().size());
        remedialAction = crac.getNetworkActions().iterator().next();
        assertEquals("remedial-action-2", remedialAction.getId());
        assertEquals("RTE_RA2", remedialAction.getName());

        elementaryActions = remedialAction.getElementaryActions().stream().sorted(Comparator.comparing(ElementaryAction::toString)).toList();
        assertEquals(3, elementaryActions.size());

        assertTrue(elementaryActions.get(0) instanceof InjectionSetpoint);
        assertEquals("shunt-compensator", ((InjectionSetpoint) elementaryActions.get(0)).getNetworkElement().getId());
        assertEquals(4, ((InjectionSetpoint) elementaryActions.get(0)).getSetpoint());

        assertTrue(elementaryActions.get(1) instanceof InjectionSetpoint);
        assertEquals("FFR2AA1 _generator", ((InjectionSetpoint) elementaryActions.get(1)).getNetworkElement().getId());
        assertEquals(100d, ((InjectionSetpoint) elementaryActions.get(1)).getSetpoint());

        assertTrue(elementaryActions.get(2) instanceof TopologicalAction);
        assertEquals("DDE3AA1  DDE4AA1  1", ((TopologicalAction) elementaryActions.get(2)).getNetworkElement().getId());
        assertEquals(ActionType.CLOSE, ((TopologicalAction) elementaryActions.get(2)).getActionType());

        assertRaNotImported(cracCreationContext, "remedial-action-1", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-1 will not be imported because it has no elementary action");
    }

    @Test
    void activateDeactivateContingencyWithRemedialAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-12_ContingencyWithRemedialAction.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        List<Contingency> contingencies = crac.getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).toList();
        assertEquals(3, contingencies.size());
        assertContingencyEquality(contingencies.get(0), "contingency-1", "RTE_CO1", 1, List.of("FFR1AA1  FFR2AA1  1"));
        assertContingencyEquality(contingencies.get(1), "contingency-2", "RTE_CO2", 1, List.of("FFR1AA1  FFR3AA1  1"));
        assertContingencyEquality(contingencies.get(2), "contingency-3", "RTE_CO3", 1, List.of("FFR1AA1  FFR4AA1  1"));

        List<NetworkAction> networkActions = crac.getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(1, networkActions.size());

        assertRaNotImported(cracCreationContext, "remedial-action-1", ImportStatus.NOT_FOR_RAO, "Remedial action 'remedial-action-1' will not be imported because field 'normalEnabled' in 'ContingencyWithRemedialAction' must be true or empty");

        assertEquals("remedial-action-2", networkActions.get(0).getId());
        assertEquals("RTE_RA2", networkActions.get(0).getName());
        assertEquals(1, networkActions.get(0).getUsageRules().size());
        assertTrue(networkActions.get(0).getUsageRules().iterator().next() instanceof OnContingencyState);
        assertEquals(crac.getInstant("curative"), networkActions.get(0).getUsageRules().iterator().next().getInstant());
        assertEquals("contingency-3", ((OnContingencyState) networkActions.get(0).getUsageRules().iterator().next()).getContingency().getId());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-12_ContingencyWithRemedialAction.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        contingencies = crac.getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).toList();
        assertEquals(2, contingencies.size());
        assertContingencyEquality(contingencies.get(0), "contingency-1", "RTE_CO1", 1, List.of("FFR1AA1  FFR2AA1  1"));
        assertContingencyEquality(contingencies.get(1), "contingency-2", "RTE_CO2", 1, List.of("FFR1AA1  FFR3AA1  1"));
        assertContingencyNotImported(cracCreationContext, "contingency-3", ImportStatus.NOT_FOR_RAO, "contingency.mustStudy is false");

        networkActions = crac.getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(1, networkActions.size());

        assertRaNotImported(cracCreationContext, "remedial-action-1", ImportStatus.NOT_FOR_RAO, "Remedial action 'remedial-action-1' will not be imported because field 'normalEnabled' in 'ContingencyWithRemedialAction' must be true or empty");

        assertEquals("remedial-action-2", networkActions.get(0).getId());
        assertEquals("RTE_RA2", networkActions.get(0).getName());
        assertEquals(0, networkActions.get(0).getUsageRules().size());
    }

    @Test
    void activateDeactivateAngleCnec() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-13_AngleCNEC.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("preventive"), null, 30d, -30d, true);
        assertCnecNotImported(cracCreationContext, "assessed-element-2", ImportStatus.NOT_FOR_RAO, "AssessedElement.normalEnabled is false");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-13_AngleCNEC.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", crac.getInstant("preventive"), null, 45d, -45d, true);
        assertCnecNotImported(cracCreationContext, "assessed-element-1", ImportStatus.NOT_FOR_RAO, "AssessedElement.normalEnabled is false");
    }

    @Test
    void changeAngleCnecThreshold() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-14_VoltageAngleLimit.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE (assessed-element) - preventive"), "RTE_AE (assessed-element) - preventive", "RTE_AE (assessed-element) - preventive", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("preventive"), null, 30d, -30d, true);

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-14_VoltageAngleLimit.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE (assessed-element) - preventive"), "RTE_AE (assessed-element) - preventive", "RTE_AE (assessed-element) - preventive", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("preventive"), null, 45d, -45d, true);
    }

    @Test
    void activateDeactivateAssessedElementWithContingency() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-15_AssessedElementWithContingency.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(4, crac.getAngleCnecs().size());
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("preventive"), null, 30d, -30d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - RTE_CO1 - curative"), "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", "RTE_AE1 (assessed-element-1) - RTE_CO1 - curative", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("curative"), "contingency-1", 30d, -30d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", crac.getInstant("preventive"), null, 45d, -45d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - RTE_CO3 - curative"), "RTE_AE2 (assessed-element-2) - RTE_CO3 - curative", "RTE_AE2 (assessed-element-2) - RTE_CO3 - curative", "BBE4AA1 ", "BBE1AA1 ", crac.getInstant("curative"), "contingency-3", 45d, -45d, true);
        assertCnecNotImported(cracCreationContext, "assessed-element-1", ImportStatus.NOT_FOR_RAO, "AssessedElementWithContingency.normalEnabled is false for contingency contingency-2");

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-15_AssessedElementWithContingency.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(3, crac.getAngleCnecs().size());
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("preventive"), null, 30d, -30d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - RTE_CO2 - curative"), "RTE_AE1 (assessed-element-1) - RTE_CO2 - curative", "RTE_AE1 (assessed-element-1) - RTE_CO2 - curative", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("curative"), "contingency-2", 30d, -30d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", crac.getInstant("preventive"), null, 45d, -45d, true);
        assertCnecNotImported(cracCreationContext, "assessed-element-1", ImportStatus.NOT_FOR_RAO, "AssessedElementWithContingency.normalEnabled is false for contingency contingency-1");
        assertCnecNotImported(cracCreationContext, "assessed-element-2", ImportStatus.INCONSISTENCY_IN_DATA, "the contingency contingency-3 linked to the assessed element doesn't exist in the CRAC");
    }

    @Test
    void activateDeactivateAssessedElementWithRemedialAction() {
        // General case
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-16_AssessedElementWithRemedialAction.zip", network, "2023-01-01T22:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getContingencies().size());
        assertContingencyEquality(crac.getContingencies().iterator().next(), "contingency", "RTE_CO", 1, List.of("FFR1AA1  FFR2AA1  1"));

        assertEquals(6, crac.getAngleCnecs().size());
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("preventive"), null, 30d, -30d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - RTE_CO - curative"), "RTE_AE1 (assessed-element-1) - RTE_CO - curative", "RTE_AE1 (assessed-element-1) - RTE_CO - curative", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("curative"), "contingency", 30d, -30d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", crac.getInstant("preventive"), null, 45d, -45d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - RTE_CO - curative"), "RTE_AE2 (assessed-element-2) - RTE_CO - curative", "RTE_AE2 (assessed-element-2) - RTE_CO - curative", "BBE4AA1 ", "BBE1AA1 ", crac.getInstant("curative"), "contingency", 45d, -45d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE3 (assessed-element-3) - preventive"), "RTE_AE3 (assessed-element-3) - preventive", "RTE_AE3 (assessed-element-3) - preventive", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("preventive"), null, 15d, -15d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE3 (assessed-element-3) - RTE_CO - curative"), "RTE_AE3 (assessed-element-3) - RTE_CO - curative", "RTE_AE3 (assessed-element-3) - RTE_CO - curative", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("curative"), "contingency", 15d, -15d, true);

        List<NetworkAction> remedialActions = crac.getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(2, remedialActions.size());

        assertEquals("remedial-action-1", remedialActions.get(0).getId());
        assertEquals("RTE_RA1", remedialActions.get(0).getName());
        assertEquals(1, remedialActions.get(0).getUsageRules().size());
        assertTrue(remedialActions.get(0).getUsageRules().iterator().next() instanceof OnAngleConstraint);
        assertEquals(crac.getInstant("curative"), remedialActions.get(0).getUsageRules().iterator().next().getInstant());
        assertEquals("RTE_AE1 (assessed-element-1) - RTE_CO - curative", ((OnAngleConstraint) remedialActions.get(0).getUsageRules().iterator().next()).getAngleCnec().getId());

        assertEquals("remedial-action-2", remedialActions.get(1).getId());
        assertEquals("RTE_RA2", remedialActions.get(1).getName());
        assertEquals(1, remedialActions.get(1).getUsageRules().size());
        assertTrue(remedialActions.get(1).getUsageRules().iterator().next() instanceof OnAngleConstraint);
        assertEquals(crac.getInstant("curative"), remedialActions.get(1).getUsageRules().iterator().next().getInstant());
        assertEquals("RTE_AE3 (assessed-element-3) - RTE_CO - curative", ((OnAngleConstraint) remedialActions.get(1).getUsageRules().iterator().next()).getAngleCnec().getId());

        // With SSI
        cracCreationContext = getCsaCracCreationContext("/ssi/SSI-16_AssessedElementWithRemedialAction.zip", network, "2024-01-31T12:30Z");
        crac = cracCreationContext.getCrac();

        assertEquals(1, crac.getContingencies().size());
        assertContingencyEquality(crac.getContingencies().iterator().next(), "contingency", "RTE_CO", 1, List.of("FFR1AA1  FFR2AA1  1"));

        assertEquals(4, crac.getAngleCnecs().size());
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - preventive"), "RTE_AE1 (assessed-element-1) - preventive", "RTE_AE1 (assessed-element-1) - preventive", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("preventive"), null, 30d, -30d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE1 (assessed-element-1) - RTE_CO - curative"), "RTE_AE1 (assessed-element-1) - RTE_CO - curative", "RTE_AE1 (assessed-element-1) - RTE_CO - curative", "BBE1AA1 ", "BBE4AA1 ", crac.getInstant("curative"), "contingency", 30d, -30d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - preventive"), "RTE_AE2 (assessed-element-2) - preventive", "RTE_AE2 (assessed-element-2) - preventive", "BBE4AA1 ", "BBE1AA1 ", crac.getInstant("preventive"), null, 45d, -45d, true);
        assertAngleCnecEquality(crac.getAngleCnec("RTE_AE2 (assessed-element-2) - RTE_CO - curative"), "RTE_AE2 (assessed-element-2) - RTE_CO - curative", "RTE_AE2 (assessed-element-2) - RTE_CO - curative", "BBE4AA1 ", "BBE1AA1 ", crac.getInstant("curative"), "contingency", 45d, -45d, true);

        remedialActions = crac.getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(2, remedialActions.size());

        assertEquals("remedial-action-1", remedialActions.get(0).getId());
        assertEquals("RTE_RA1", remedialActions.get(0).getName());
        assertEquals(1, remedialActions.get(0).getUsageRules().size());
        assertTrue(remedialActions.get(0).getUsageRules().iterator().next() instanceof OnAngleConstraint);
        assertEquals(crac.getInstant("curative"), remedialActions.get(0).getUsageRules().iterator().next().getInstant());
        assertEquals("RTE_AE2 (assessed-element-2) - RTE_CO - curative", ((OnAngleConstraint) remedialActions.get(0).getUsageRules().iterator().next()).getAngleCnec().getId());

        assertEquals("remedial-action-2", remedialActions.get(1).getId());
        assertEquals("RTE_RA2", remedialActions.get(1).getName());
        assertEquals(0, remedialActions.get(1).getUsageRules().size());
    }
}
