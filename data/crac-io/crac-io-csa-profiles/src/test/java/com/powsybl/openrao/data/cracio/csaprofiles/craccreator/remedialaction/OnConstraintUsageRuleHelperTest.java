/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.ElementCombinationConstraintKind;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElement;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.cracimpl.CracImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
class OnConstraintUsageRuleHelperTest {
    private Crac crac;
    private Set<ElementaryCreationContext> cnecCreationContexts;
    private Set<AssessedElement> assessedElements;
    private Set<AssessedElementWithRemedialAction> assessedElementWithRemedialActions;
    private Set<ContingencyWithRemedialAction> contingencyWithRemedialActions;

    @BeforeEach
    void setUp() {
        crac = new CracImpl("crac");

        // Set-up instants

        crac.newInstant("preventive", InstantKind.PREVENTIVE);
        crac.newInstant("outage", InstantKind.OUTAGE);
        crac.newInstant("auto", InstantKind.AUTO);
        crac.newInstant("curative", InstantKind.CURATIVE);

        // Add contingencies

        crac.newContingency().withId("contingency-1").add();
        crac.newContingency().withId("contingency-2").add();
        crac.newContingency().withId("contingency-3").add();
        crac.newContingency().withId("contingency-4").add();
        crac.newContingency().withId("contingency-5").add();
        crac.newContingency().withId("contingency-6").add();

        // Add FlowCNECs

        crac.newFlowCnec().withId("Line 1 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 1").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - preventive").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("preventive").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO2").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-2").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO3").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-3").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO4").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-4").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO5").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-5").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO6").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-6").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - preventive").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("preventive").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO2").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-2").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO3").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-3").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO4").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-4").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO5").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-5").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO6").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-6").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 4 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 4").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 6 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 6").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 8 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 8").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(TwoSides.ONE).withMax(1000d).withUnit(Unit.AMPERE).add().add();

        // Add CNEC creation contexts

        cnecCreationContexts = new HashSet<>();
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-1", null, "Line 1 - curative - CO1", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-2", null, "Line 2 - preventive", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-2", null, "Line 2 - curative - CO1", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-2", null, "Line 2 - curative - CO2", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-2", null, "Line 2 - curative - CO3", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-2", null, "Line 2 - curative - CO4", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-2", null, "Line 2 - curative - CO5", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-2", null, "Line 2 - curative - CO6", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-3", null, "Line 3 - preventive", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-3", null, "Line 3 - curative - CO1", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-3", null, "Line 3 - curative - CO2", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-3", null, "Line 3 - curative - CO3", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-3", null, "Line 3 - curative - CO4", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-3", null, "Line 3 - curative - CO5", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-3", null, "Line 3 - curative - CO6", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-4", null, "Line 4 - curative - CO1", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-6", null, "Line 6 - curative - CO1", false, ""));
        cnecCreationContexts.add(StandardElementaryCreationContext.imported("assessed-element-8", null, "Line 8 - curative - CO1", false, ""));

        // Add AssessedElement property bags

        AssessedElement assessedElement1 = new AssessedElement("assessed-element-1", true, null, null, null, null, false, false, true, null, null, 0d, null);
        AssessedElement assessedElement2 = new AssessedElement("assessed-element-2", true, null, null, null, null, false, true, true, null, null, 0d, null);
        AssessedElement assessedElement3 = new AssessedElement("assessed-element-3", true, null, null, null, null, false, true, true, null, null, 0d, null);
        AssessedElement assessedElement4 = new AssessedElement("assessed-element-4", true, null, null, null, null, false, false, true, null, null, 0d, null);
        AssessedElement assessedElement5 = new AssessedElement("assessed-element-5", true, null, null, null, null, false, false, true, null, null, 0d, null);
        AssessedElement assessedElement6 = new AssessedElement("assessed-element-6", true, null, null, null, null, false, false, true, null, null, 0d, null);
        AssessedElement assessedElement8 = new AssessedElement("assessed-element-8", true, null, null, null, null, false, false, true, null, null, 0d, null);

        assessedElements = Set.of(assessedElement1, assessedElement2, assessedElement3, assessedElement4, assessedElement5, assessedElement6, assessedElement8);

        // Add AssessedElementWithRemedialAction property bags

        AssessedElementWithRemedialAction assessedElement1WithRemedialAction = new AssessedElementWithRemedialAction("ae1xra", "assessed-element-1", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        AssessedElementWithRemedialAction assessedElement2WithRemedialAction = new AssessedElementWithRemedialAction("ae2xra", "assessed-element-2", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        AssessedElementWithRemedialAction assessedElement4WithRemedialAction = new AssessedElementWithRemedialAction("ae4xra", "assessed-element-4", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered", false);
        AssessedElementWithRemedialAction assessedElement5WithRemedialActionIncluded = new AssessedElementWithRemedialAction("ae5xra-included", "assessed-element-5", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        AssessedElementWithRemedialAction assessedElement5WithRemedialActionConsidered = new AssessedElementWithRemedialAction("ae5xra-considered", "assessed-element-5", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered", true);
        AssessedElementWithRemedialAction assessedElement6WithRemedialAction = new AssessedElementWithRemedialAction("ae6xra", "assessed-element-6", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.excluded", true);
        AssessedElementWithRemedialAction assessedElement7WithRemedialAction = new AssessedElementWithRemedialAction("ae7xra", "assessed-element-7", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        AssessedElementWithRemedialAction assessedElement8WithRemedialActionIncluded = new AssessedElementWithRemedialAction("ae8xra-included", "assessed-element-8", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        AssessedElementWithRemedialAction assessedElement8WithRemedialActionConsidered = new AssessedElementWithRemedialAction("ae8xra-considered", "assessed-element-8", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered", true);

        assessedElementWithRemedialActions = Set.of(assessedElement1WithRemedialAction, assessedElement2WithRemedialAction, assessedElement4WithRemedialAction, assessedElement5WithRemedialActionIncluded, assessedElement5WithRemedialActionConsidered, assessedElement6WithRemedialAction, assessedElement7WithRemedialAction, assessedElement8WithRemedialActionIncluded, assessedElement8WithRemedialActionConsidered);

        // Add ContingencyWithRemedialAction property bags

        ContingencyWithRemedialAction contingency1WithRemedialAction = new ContingencyWithRemedialAction("co1xra", "contingency-1", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        ContingencyWithRemedialAction contingency2WithRemedialAction = new ContingencyWithRemedialAction("co2xra", "contingency-2", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered", true);
        ContingencyWithRemedialAction contingency3WithRemedialAction = new ContingencyWithRemedialAction("co3xra", "contingency-3", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        ContingencyWithRemedialAction contingency4WithRemedialAction = new ContingencyWithRemedialAction("co4xra", "contingency-4", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered", true);

        contingencyWithRemedialActions = Set.of(contingency1WithRemedialAction, contingency2WithRemedialAction, contingency3WithRemedialAction, contingency4WithRemedialAction);
    }

    @Test
    void getImportedCnecFromAssessedElementId() {
        assertEquals(
            Set.of(crac.getFlowCnec("Line 2 - preventive"), crac.getFlowCnec("Line 2 - curative - CO1"), crac.getFlowCnec("Line 2 - curative - CO2"), crac.getFlowCnec("Line 2 - curative - CO3"), crac.getFlowCnec("Line 2 - curative - CO4"), crac.getFlowCnec("Line 2 - curative - CO5"), crac.getFlowCnec("Line 2 - curative - CO6")),
            OnConstraintUsageRuleHelper.getImportedCnecFromAssessedElementId("assessed-element-2", crac, cnecCreationContexts)
        );
    }

    @Test
    void getCnecsBuiltFromAssessedElementsCombinableWithRemedialActions() {
        assertEquals(
            Set.of(crac.getFlowCnec("Line 2 - preventive"), crac.getFlowCnec("Line 2 - curative - CO1"), crac.getFlowCnec("Line 2 - curative - CO2"), crac.getFlowCnec("Line 2 - curative - CO3"), crac.getFlowCnec("Line 2 - curative - CO4"), crac.getFlowCnec("Line 2 - curative - CO5"), crac.getFlowCnec("Line 2 - curative - CO6"), crac.getFlowCnec("Line 3 - preventive"), crac.getFlowCnec("Line 3 - curative - CO1"), crac.getFlowCnec("Line 3 - curative - CO2"), crac.getFlowCnec("Line 3 - curative - CO3"), crac.getFlowCnec("Line 3 - curative - CO4"), crac.getFlowCnec("Line 3 - curative - CO5"), crac.getFlowCnec("Line 3 - curative - CO6")),
            OnConstraintUsageRuleHelper.getCnecsBuiltFromAssessedElementsCombinableWithRemedialActions(crac, cnecCreationContexts, assessedElements)
        );
    }

    @Test
    void filterCnecsThatHaveGivenContingencies() {
        assertEquals(
            Set.of(crac.getFlowCnec("Line 2 - curative - CO3"), crac.getFlowCnec("Line 3 - curative - CO3"), crac.getFlowCnec("Line 2 - curative - CO5"), crac.getFlowCnec("Line 3 - curative - CO5")),
            OnConstraintUsageRuleHelper.filterCnecsThatHaveGivenContingencies(crac.getCnecs(), Set.of("contingency-3", "contingency-5"))
        );
    }

    @Test
    void processCnecsLinkedToRemedialActionWithContingencies() {
        Map<String, AssociationStatus> expectedResult = new HashMap<>();
        expectedResult.put("Line 1 - curative - CO1", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO1", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO2", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with CNEC Line 2 - curative - CO2 ignored because the combinationConstraintKinds between of the AssessedElementWithRemedialAction for assessed element assessed-element-2 and the ContingencyWithRemedialAction for contingency contingency-2 are different."));
        expectedResult.put("Line 2 - curative - CO3", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO4", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with CNEC Line 2 - curative - CO4 ignored because the combinationConstraintKinds between of the AssessedElementWithRemedialAction for assessed element assessed-element-2 and the ContingencyWithRemedialAction for contingency contingency-4 are different."));
        expectedResult.put("Line 3 - curative - CO1", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 3 - curative - CO2", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO3", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 3 - curative - CO4", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("assessed-element-4", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-4 ignored because the association is disabled."));
        expectedResult.put("assessed-element-5", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-5 ignored because this assessed element has several conflictual links to the remedial action."));
        expectedResult.put("assessed-element-6", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-6 ignored because of an illegal combinationConstraintKind."));
        expectedResult.put("assessed-element-7", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-7 ignored because no CNEC was imported by Open RAO from this assessed element."));
        expectedResult.put("assessed-element-8", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-8 ignored because this assessed element has several conflictual links to the remedial action."));

        assertEquals(expectedResult, OnConstraintUsageRuleHelper.processCnecsLinkedToRemedialAction(crac, "remedial-action", assessedElements, assessedElementWithRemedialActions, contingencyWithRemedialActions, cnecCreationContexts));
    }

    @Test
    void processCnecsLinkedToRemedialActionWithoutContingencies() {
        Map<String, AssociationStatus> expectedResult = new HashMap<>();
        expectedResult.put("Line 1 - curative - CO1", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - preventive", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO1", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO2", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO3", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO4", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO5", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO6", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 3 - preventive", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO1", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO2", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO3", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO4", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO5", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO6", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("assessed-element-4", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-4 ignored because the association is disabled."));
        expectedResult.put("assessed-element-5", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-5 ignored because this assessed element has several conflictual links to the remedial action."));
        expectedResult.put("assessed-element-6", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-6 ignored because of an illegal combinationConstraintKind."));
        expectedResult.put("assessed-element-7", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-7 ignored because no CNEC was imported by Open RAO from this assessed element."));
        expectedResult.put("assessed-element-8", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-8 ignored because this assessed element has several conflictual links to the remedial action."));

        assertEquals(expectedResult, OnConstraintUsageRuleHelper.processCnecsLinkedToRemedialAction(crac, "remedial-action", assessedElements, assessedElementWithRemedialActions, Set.of(), cnecCreationContexts));
    }
}
