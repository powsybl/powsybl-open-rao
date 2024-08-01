/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.ElementCombinationConstraintKind;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.cracimpl.CracImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
class OnContingencyStateUsageRuleHelperTest {

    @Test
    void processContingenciesLinkedToRemedialAction() {
        CracImpl crac = new CracImpl("crac");
        crac.newContingency().withId("contingency-1").add();
        crac.newContingency().withId("contingency-2").add();
        crac.newContingency().withId("contingency-3").add();
        crac.newContingency().withId("contingency-4").add();
        crac.newContingency().withId("contingency-6").add();

        ContingencyWithRemedialAction contingency1WithRemedialAction = new ContingencyWithRemedialAction("co1xra", "contingency-1", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        ContingencyWithRemedialAction contingency2WithRemedialAction = new ContingencyWithRemedialAction("co2xra", "contingency-2", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered", true);
        ContingencyWithRemedialAction contingency3WithRemedialAction = new ContingencyWithRemedialAction("co3xra", "contingency-3", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", false);
        ContingencyWithRemedialAction contingency4WithRemedialAction = new ContingencyWithRemedialAction("co4xra", "contingency-4", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.excluded", true);
        ContingencyWithRemedialAction contingency5WithRemedialAction = new ContingencyWithRemedialAction("co5xra", "contingency-5", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        ContingencyWithRemedialAction contingency6WithRemedialActionIncluded = new ContingencyWithRemedialAction("co6xra-included", "contingency-6", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included", true);
        ContingencyWithRemedialAction contingency6WithRemedialActionConsidered = new ContingencyWithRemedialAction("co6xra-considered", "contingency-6", "remedial-action", "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered", true);

        Map<String, AssociationStatus> contingencyStatusMap = OnContingencyStateUsageRuleHelper.processContingenciesLinkedToRemedialAction(crac, "remedial-action", Set.of(contingency1WithRemedialAction, contingency2WithRemedialAction, contingency3WithRemedialAction, contingency4WithRemedialAction, contingency5WithRemedialAction, contingency6WithRemedialActionIncluded, contingency6WithRemedialActionConsidered));
        assertEquals(
            Map.of(
                "contingency-1", new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED, ""),
                "contingency-2", new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, ""),
                "contingency-3", new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action remedial-action with contingency contingency-3 ignored because the association is disabled."),
                "contingency-4", new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action remedial-action with contingency contingency-4 ignored because of an illegal combinationConstraintKind."),
                "contingency-5", new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action remedial-action with contingency contingency-5 ignored because this contingency does not exist or was not imported by Open RAO."),
                "contingency-6", new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action remedial-action with contingency contingency-6 ignored because this contingency has several conflictual links to the remedial action.")
            ),
            contingencyStatusMap
        );
    }
}
