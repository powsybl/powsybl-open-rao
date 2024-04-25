/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.cracimpl.CracImpl;
import com.powsybl.triplestore.api.PropertyBag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
class OnContingencyStateUsageRuleHelperTest {
    private final List<String> properties = List.of("mRID", "remedialAction", "contingency", "combinationConstraintKind", "normalEnabled");

    @Test
    void processContingenciesLinkedToRemedialAction() {
        CracImpl crac = new CracImpl("crac");
        crac.newContingency().withId("contingency-1").add();
        crac.newContingency().withId("contingency-2").add();
        crac.newContingency().withId("contingency-3").add();
        crac.newContingency().withId("contingency-4").add();
        crac.newContingency().withId("contingency-6").add();

        PropertyBag co1WithRaPropertyBag = new PropertyBag(properties, true, false);
        co1WithRaPropertyBag.put("mRID", "_co1xra");
        co1WithRaPropertyBag.put("remedialAction", "remedial-action");
        co1WithRaPropertyBag.put("contingency", "contingency-1");
        co1WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");
        co1WithRaPropertyBag.put("normalEnabled", "true");
        co1WithRaPropertyBag.setClassPropertyNames(List.of("mRID", "normalEnabled"));
        co1WithRaPropertyBag.setResourceNames(List.of("remedialAction", "contingency", "combinationConstraintKind"));

        PropertyBag co2WithRaPropertyBag = new PropertyBag(List.of("mRID", "remedialAction", "contingency", "combinationConstraintKind"), false);
        co2WithRaPropertyBag.put("mRID", "_co2xra");
        co2WithRaPropertyBag.put("remedialAction", "remedial-action");
        co2WithRaPropertyBag.put("contingency", "contingency-2");
        co2WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered");
        co2WithRaPropertyBag.setClassPropertyNames(List.of("mRID", "normalEnabled"));
        co2WithRaPropertyBag.setResourceNames(List.of("remedialAction", "contingency", "combinationConstraintKind"));

        PropertyBag co3WithRaPropertyBag = new PropertyBag(properties, true, false);
        co3WithRaPropertyBag.put("mRID", "_co3xra");
        co3WithRaPropertyBag.put("remedialAction", "remedial-action");
        co3WithRaPropertyBag.put("contingency", "contingency-3");
        co3WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");
        co3WithRaPropertyBag.put("normalEnabled", "false");
        co3WithRaPropertyBag.setClassPropertyNames(List.of("mRID", "normalEnabled"));
        co3WithRaPropertyBag.setResourceNames(List.of("remedialAction", "contingency", "combinationConstraintKind"));

        PropertyBag co4WithRaPropertyBag = new PropertyBag(properties, true, false);
        co4WithRaPropertyBag.put("mRID", "_co4xra");
        co4WithRaPropertyBag.put("remedialAction", "remedial-action");
        co4WithRaPropertyBag.put("contingency", "contingency-4");
        co4WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.excluded");
        co4WithRaPropertyBag.put("normalEnabled", "true");
        co4WithRaPropertyBag.setClassPropertyNames(List.of("mRID", "normalEnabled"));
        co4WithRaPropertyBag.setResourceNames(List.of("remedialAction", "contingency", "combinationConstraintKind"));

        PropertyBag co5WithRaPropertyBag = new PropertyBag(properties, true, false);
        co5WithRaPropertyBag.put("mRID", "_co5xra");
        co5WithRaPropertyBag.put("remedialAction", "remedial-action");
        co5WithRaPropertyBag.put("contingency", "contingency-5");
        co5WithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");
        co5WithRaPropertyBag.put("normalEnabled", "true");
        co5WithRaPropertyBag.setClassPropertyNames(List.of("mRID", "normalEnabled"));
        co5WithRaPropertyBag.setResourceNames(List.of("remedialAction", "contingency", "combinationConstraintKind"));

        PropertyBag co6IncludedWithRaPropertyBag = new PropertyBag(properties, true, false);
        co6IncludedWithRaPropertyBag.put("mRID", "_co6xra-included");
        co6IncludedWithRaPropertyBag.put("remedialAction", "remedial-action");
        co6IncludedWithRaPropertyBag.put("contingency", "contingency-6");
        co6IncludedWithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");
        co6IncludedWithRaPropertyBag.put("normalEnabled", "true");
        co6IncludedWithRaPropertyBag.setClassPropertyNames(List.of("mRID", "normalEnabled"));
        co6IncludedWithRaPropertyBag.setResourceNames(List.of("remedialAction", "contingency", "combinationConstraintKind"));

        PropertyBag co6ConsideredWithRaPropertyBag = new PropertyBag(List.of("mRID", "remedialAction", "contingency", "combinationConstraintKind"), true, false);
        co6ConsideredWithRaPropertyBag.put("mRID", "_co6xra-considered");
        co6ConsideredWithRaPropertyBag.put("remedialAction", "remedial-action");
        co6ConsideredWithRaPropertyBag.put("contingency", "contingency-6");
        co6ConsideredWithRaPropertyBag.put("combinationConstraintKind", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included");
        co6ConsideredWithRaPropertyBag.setClassPropertyNames(List.of("mRID", "normalEnabled"));
        co6ConsideredWithRaPropertyBag.setResourceNames(List.of("remedialAction", "contingency", "combinationConstraintKind"));

        Map<String, AssociationStatus> contingencyStatusMap = OnContingencyStateUsageRuleHelper.processContingenciesLinkedToRemedialAction(crac, "remedial-action", Set.of(co1WithRaPropertyBag, co2WithRaPropertyBag, co3WithRaPropertyBag, co4WithRaPropertyBag, co5WithRaPropertyBag, co6IncludedWithRaPropertyBag, co6ConsideredWithRaPropertyBag));
        assertEquals(
            Map.of(
                "contingency-1", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED, ""),
                "contingency-2", new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, ""),
                "contingency-3", new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action remedial-action with contingency contingency-3 ignored because the association is disabled."),
                "contingency-4", new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action remedial-action with contingency contingency-4 ignored because of an illegal combinationConstraintKind."),
                "contingency-5", new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action remedial-action with contingency contingency-5 ignored because this contingency does not exist or was not imported by Open RAO."),
                "contingency-6", new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action remedial-action with contingency contingency-6 ignored because this contingency has several conflictual links to the remedial action.")
            ),
            contingencyStatusMap
        );
    }
}
