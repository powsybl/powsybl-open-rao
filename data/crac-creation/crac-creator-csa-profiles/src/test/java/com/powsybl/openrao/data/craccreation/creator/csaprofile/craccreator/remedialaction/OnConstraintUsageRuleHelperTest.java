/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.cracimpl.CracImpl;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
class OnConstraintUsageRuleHelperTest {
    private Crac crac;
    private Set<CsaProfileElementaryCreationContext> cnecCreationContexts;
    private PropertyBags assessedElementPropertyBags;
    private Set<PropertyBag> assessedElementWithRemedialActions;
    private Set<PropertyBag> contingencyWithRemedialActions;

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

        crac.newFlowCnec().withId("Line 1 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 1").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - preventive").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("preventive").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO2").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-2").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO3").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-3").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO4").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-4").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO5").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-5").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 2 - curative - CO6").withNominalVoltage(400d).withNetworkElement("Line 2").withInstant("curative").withContingency("contingency-6").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - preventive").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("preventive").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO2").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-2").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO3").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-3").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO4").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-4").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO5").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-5").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 3 - curative - CO6").withNominalVoltage(400d).withNetworkElement("Line 3").withInstant("curative").withContingency("contingency-6").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 4 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 4").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 6 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 6").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();
        crac.newFlowCnec().withId("Line 8 - curative - CO1").withNominalVoltage(400d).withNetworkElement("Line 8").withInstant("curative").withContingency("contingency-1").newThreshold().withSide(Side.LEFT).withMax(1000d).withUnit(Unit.AMPERE).add().add();

        // Add CNEC creation contexts

        cnecCreationContexts = new HashSet<>();
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-1", "Line 1 - curative - CO1", "Line 1 - curative - CO1", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-2", "Line 2 - preventive", "Line 2 - preventive", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-2", "Line 2 - curative - CO1", "Line 2 - curative - CO1", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-2", "Line 2 - curative - CO2", "Line 2 - curative - CO2", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-2", "Line 2 - curative - CO3", "Line 2 - curative - CO3", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-2", "Line 2 - curative - CO4", "Line 2 - curative - CO4", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-2", "Line 2 - curative - CO5", "Line 2 - curative - CO5", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-2", "Line 2 - curative - CO6", "Line 2 - curative - CO6", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-3", "Line 3 - preventive", "Line 3 - preventive", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-3", "Line 3 - curative - CO1", "Line 3 - curative - CO1", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-3", "Line 3 - curative - CO2", "Line 3 - curative - CO2", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-3", "Line 3 - curative - CO3", "Line 3 - curative - CO3", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-3", "Line 3 - curative - CO4", "Line 3 - curative - CO4", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-3", "Line 3 - curative - CO5", "Line 3 - curative - CO5", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-3", "Line 3 - curative - CO6", "Line 3 - curative - CO6", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-4", "Line 4 - curative - CO1", "Line 4 - curative - CO1", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-6", "Line 6 - curative - CO1", "Line 6 - curative - CO1", "", false));
        cnecCreationContexts.add(CsaProfileElementaryCreationContext.imported("assessed-element-8", "Line 8 - curative - CO1", "Line 8 - curative - CO1", "", false));

        // Add AssessedElement property bags

        PropertyBag assessedElement1PropertyBag = new PropertyBag(List.of("assessedElement", "isCombinableWithRemedialAction"), true, false);
        assessedElement1PropertyBag.put("assessedElement", "_assessed-element-1");
        assessedElement1PropertyBag.put("isCombinableWithRemedialAction", "false");

        PropertyBag assessedElement2PropertyBag = new PropertyBag(List.of("assessedElement", "isCombinableWithRemedialAction"), true, false);
        assessedElement2PropertyBag.put("assessedElement", "_assessed-element-2");
        assessedElement2PropertyBag.put("isCombinableWithRemedialAction", "true");

        PropertyBag assessedElement3PropertyBag = new PropertyBag(List.of("assessedElement", "isCombinableWithRemedialAction"), true, false);
        assessedElement3PropertyBag.put("assessedElement", "_assessed-element-3");
        assessedElement3PropertyBag.put("isCombinableWithRemedialAction", "true");

        PropertyBag assessedElement4PropertyBag = new PropertyBag(List.of("assessedElement", "isCombinableWithRemedialAction"), true, false);
        assessedElement4PropertyBag.put("assessedElement", "_assessed-element-4");
        assessedElement4PropertyBag.put("isCombinableWithRemedialAction", "false");

        PropertyBag assessedElement5PropertyBag = new PropertyBag(List.of("assessedElement", "isCombinableWithRemedialAction"), true, false);
        assessedElement5PropertyBag.put("assessedElement", "_assessed-element-5");
        assessedElement5PropertyBag.put("isCombinableWithRemedialAction", "false");

        PropertyBag assessedElement6PropertyBag = new PropertyBag(List.of("assessedElement", "isCombinableWithRemedialAction"), true, false);
        assessedElement6PropertyBag.put("assessedElement", "_assessed-element-6");
        assessedElement6PropertyBag.put("isCombinableWithRemedialAction", "false");

        PropertyBag assessedElement8PropertyBag = new PropertyBag(List.of("assessedElement", "isCombinableWithRemedialAction"), true, false);
        assessedElement8PropertyBag.put("assessedElement", "_assessed-element-8");
        assessedElement8PropertyBag.put("isCombinableWithRemedialAction", "false");

        assessedElementPropertyBags = new PropertyBags(Set.of(assessedElement1PropertyBag, assessedElement2PropertyBag, assessedElement3PropertyBag, assessedElement4PropertyBag, assessedElement5PropertyBag, assessedElement6PropertyBag, assessedElement8PropertyBag));

        // Add AssessedElementWithRemedialAction property bags

        PropertyBag ae1WithRaPropertyBag = new PropertyBag(List.of("assessedElementWithRemedialAction", "remedialAction", "assessedElement", "combinationConstraintKind"), true, false);
        ae1WithRaPropertyBag.put("assessedElementWithRemedialAction", "_ae1xra");
        ae1WithRaPropertyBag.put("remedialAction", "remedial-action");
        ae1WithRaPropertyBag.put("assessedElement", "assessed-element-1");
        ae1WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");

        PropertyBag ae2WithRaPropertyBag = new PropertyBag(List.of("assessedElementWithRemedialAction", "remedialAction", "assessedElement", "combinationConstraintKind", "normalEnabled"), true, false);
        ae2WithRaPropertyBag.put("assessedElementWithRemedialAction", "_ae2xra");
        ae2WithRaPropertyBag.put("remedialAction", "remedial-action");
        ae2WithRaPropertyBag.put("assessedElement", "assessed-element-2");
        ae2WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");
        ae2WithRaPropertyBag.put("normalEnabled", "true");

        PropertyBag ae4WithRaPropertyBag = new PropertyBag(List.of("assessedElementWithRemedialAction", "remedialAction", "assessedElement", "combinationConstraintKind", "normalEnabled"), true, false);
        ae4WithRaPropertyBag.put("assessedElementWithRemedialAction", "_ae4xra");
        ae4WithRaPropertyBag.put("remedialAction", "remedial-action");
        ae4WithRaPropertyBag.put("assessedElement", "assessed-element-4");
        ae4WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered");
        ae4WithRaPropertyBag.put("normalEnabled", "false");

        PropertyBag ae5WithRaPropertyBag1 = new PropertyBag(List.of("assessedElementWithRemedialAction", "remedialAction", "assessedElement", "combinationConstraintKind"), true, false);
        ae5WithRaPropertyBag1.put("assessedElementWithRemedialAction", "_ae5xra-included");
        ae5WithRaPropertyBag1.put("remedialAction", "remedial-action");
        ae5WithRaPropertyBag1.put("assessedElement", "assessed-element-5");
        ae5WithRaPropertyBag1.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");
        ae5WithRaPropertyBag1.setResourceNames(List.of("remedialAction", "assessedElement", "combinationConstraintKind"));

        PropertyBag ae5WithRaPropertyBag2 = new PropertyBag(List.of("assessedElementWithRemedialAction", "remedialAction", "assessedElement", "combinationConstraintKind"), true, false);
        ae5WithRaPropertyBag2.put("assessedElementWithRemedialAction", "_ae5xra-considered");
        ae5WithRaPropertyBag2.put("remedialAction", "remedial-action");
        ae5WithRaPropertyBag2.put("assessedElement", "assessed-element-5");
        ae5WithRaPropertyBag2.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered");
        ae5WithRaPropertyBag2.setResourceNames(List.of("remedialAction", "assessedElement", "combinationConstraintKind"));

        PropertyBag ae6WithRaPropertyBag = new PropertyBag(List.of("assessedElementWithRemedialAction", "remedialAction", "assessedElement", "combinationConstraintKind"), true, false);
        ae6WithRaPropertyBag.put("assessedElementWithRemedialAction", "_ae6xra");
        ae6WithRaPropertyBag.put("remedialAction", "remedial-action");
        ae6WithRaPropertyBag.put("assessedElement", "assessed-element-6");
        ae6WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.excluded");
        ae6WithRaPropertyBag.setResourceNames(List.of("remedialAction", "assessedElement", "combinationConstraintKind"));

        PropertyBag ae7WithRaPropertyBag = new PropertyBag(List.of("assessedElementWithRemedialAction", "remedialAction", "assessedElement", "combinationConstraintKind"), true, false);
        ae7WithRaPropertyBag.put("assessedElementWithRemedialAction", "_ae7xra");
        ae7WithRaPropertyBag.put("remedialAction", "remedial-action");
        ae7WithRaPropertyBag.put("assessedElement", "assessed-element-7");
        ae7WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");

        PropertyBag ae8WithRaPropertyBagIncluded = new PropertyBag(List.of("assessedElementWithRemedialAction", "remedialAction", "assessedElement", "combinationConstraintKind"), true, false);
        ae8WithRaPropertyBagIncluded.put("assessedElementWithRemedialAction", "_ae8xra-included");
        ae8WithRaPropertyBagIncluded.put("remedialAction", "remedial-action");
        ae8WithRaPropertyBagIncluded.put("assessedElement", "assessed-element-8");
        ae8WithRaPropertyBagIncluded.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");

        PropertyBag ae8WithRaPropertyBagConsidered = new PropertyBag(List.of("assessedElementWithRemedialAction", "remedialAction", "assessedElement", "combinationConstraintKind"), true, false);
        ae8WithRaPropertyBagConsidered.put("assessedElementWithRemedialAction", "_ae8xra-considered");
        ae8WithRaPropertyBagConsidered.put("remedialAction", "remedial-action");
        ae8WithRaPropertyBagConsidered.put("assessedElement", "assessed-element-8");
        ae8WithRaPropertyBagConsidered.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered");

        assessedElementWithRemedialActions = Set.of(ae1WithRaPropertyBag, ae2WithRaPropertyBag, ae4WithRaPropertyBag, ae5WithRaPropertyBag1, ae5WithRaPropertyBag2, ae6WithRaPropertyBag, ae7WithRaPropertyBag, ae8WithRaPropertyBagIncluded, ae8WithRaPropertyBagConsidered);

        // Add ContingencyWithRemedialAction property bags

        PropertyBag co1WithRaPropertyBag = new PropertyBag(List.of("contingencyWithRemedialAction", "mRID", "remedialAction", "contingency", "combinationConstraintKind", "normalEnabled"), true, false);
        co1WithRaPropertyBag.put("contingencyWithRemedialAction", "_co1xra");
        co1WithRaPropertyBag.put("mRID", "_co1xra");
        co1WithRaPropertyBag.put("remedialAction", "remedial-action");
        co1WithRaPropertyBag.put("contingency", "contingency-1");
        co1WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");
        co1WithRaPropertyBag.put("normalEnabled", "true");

        PropertyBag co2WithRaPropertyBag = new PropertyBag(List.of("contingencyWithRemedialAction", "mRID", "remedialAction", "contingency", "combinationConstraintKind", "normalEnabled"), true, false);
        co2WithRaPropertyBag.put("contingencyWithRemedialAction", "_co2xra");
        co2WithRaPropertyBag.put("mRID", "_co2xra");
        co2WithRaPropertyBag.put("remedialAction", "remedial-action");
        co2WithRaPropertyBag.put("contingency", "contingency-2");
        co2WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered");
        co2WithRaPropertyBag.put("normalEnabled", "true");

        PropertyBag co3WithRaPropertyBag = new PropertyBag(List.of("contingencyWithRemedialAction", "mRID", "remedialAction", "contingency", "combinationConstraintKind", "normalEnabled"), true, false);
        co3WithRaPropertyBag.put("contingencyWithRemedialAction", "_co3xra");
        co3WithRaPropertyBag.put("mRID", "_co3xra");
        co3WithRaPropertyBag.put("remedialAction", "remedial-action");
        co3WithRaPropertyBag.put("contingency", "contingency-3");
        co3WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");
        co3WithRaPropertyBag.put("normalEnabled", "true");

        PropertyBag co4WithRaPropertyBag = new PropertyBag(List.of("contingencyWithRemedialAction", "mRID", "remedialAction", "contingency", "combinationConstraintKind", "normalEnabled"), true, false);
        co4WithRaPropertyBag.put("contingencyWithRemedialAction", "_co4xra");
        co4WithRaPropertyBag.put("mRID", "_co4xra");
        co4WithRaPropertyBag.put("remedialAction", "remedial-action");
        co4WithRaPropertyBag.put("contingency", "contingency-4");
        co4WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered");
        co4WithRaPropertyBag.put("normalEnabled", "true");

        contingencyWithRemedialActions = Set.of(co1WithRaPropertyBag, co2WithRaPropertyBag, co3WithRaPropertyBag, co4WithRaPropertyBag);
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
            OnConstraintUsageRuleHelper.getCnecsBuiltFromAssessedElementsCombinableWithRemedialActions(crac, cnecCreationContexts, assessedElementPropertyBags)
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
        expectedResult.put("Line 1 - curative - CO1", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO1", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO2", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with CNEC Line 2 - curative - CO2 ignored because the combinationConstraintKinds between of the AssessedElementWithRemedialAction for assessed element assessed-element-2 and the ContingencyWithRemedialAction for contingency contingency-2 are different."));
        expectedResult.put("Line 2 - curative - CO3", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO4", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with CNEC Line 2 - curative - CO4 ignored because the combinationConstraintKinds between of the AssessedElementWithRemedialAction for assessed element assessed-element-2 and the ContingencyWithRemedialAction for contingency contingency-4 are different."));
        expectedResult.put("Line 3 - curative - CO1", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 3 - curative - CO2", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO3", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 3 - curative - CO4", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("assessed-element-4", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-4 ignored because the association is disabled."));
        expectedResult.put("assessed-element-5", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-5 ignored because this assessed element has several conflictual links to the remedial action."));
        expectedResult.put("assessed-element-6", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-6 ignored because of an illegal combinationConstraintKind."));
        expectedResult.put("assessed-element-7", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-7 ignored because no CNEC was imported by Open RAO from this assessed element."));
        expectedResult.put("assessed-element-8", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-8 ignored because this assessed element has several conflictual links to the remedial action."));

        assertEquals(expectedResult, OnConstraintUsageRuleHelper.processCnecsLinkedToRemedialAction(crac, "remedial-action", assessedElementPropertyBags, assessedElementWithRemedialActions, contingencyWithRemedialActions, cnecCreationContexts));
    }

    @Test
    void processCnecsLinkedToRemedialActionWithoutContingencies() {
        Map<String, AssociationStatus> expectedResult = new HashMap<>();
        expectedResult.put("Line 1 - curative - CO1", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - preventive", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO1", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO2", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO3", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO4", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO5", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 2 - curative - CO6", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""));
        expectedResult.put("Line 3 - preventive", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO1", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO2", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO3", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO4", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO5", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("Line 3 - curative - CO6", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""));
        expectedResult.put("assessed-element-4", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-4 ignored because the association is disabled."));
        expectedResult.put("assessed-element-5", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-5 ignored because this assessed element has several conflictual links to the remedial action."));
        expectedResult.put("assessed-element-6", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-6 ignored because of an illegal combinationConstraintKind."));
        expectedResult.put("assessed-element-7", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-7 ignored because no CNEC was imported by Open RAO from this assessed element."));
        expectedResult.put("assessed-element-8", new AssociationStatus(false, null, "OnConstraint usage rule for remedial action remedial-action with assessed element assessed-element-8 ignored because this assessed element has several conflictual links to the remedial action."));

        assertEquals(expectedResult, OnConstraintUsageRuleHelper.processCnecsLinkedToRemedialAction(crac, "remedial-action", assessedElementPropertyBags, assessedElementWithRemedialActions, Set.of(), cnecCreationContexts));
    }
}
