/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.ElementCombinationConstraintKind;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ContingencyWithRemedialAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
public final class OnContingencyStateUsageRuleHelper {

    private OnContingencyStateUsageRuleHelper() { }

    public static Map<String, AssociationStatus> processContingenciesLinkedToRemedialAction(Crac crac, String remedialActionId, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions) {
        Map<String, AssociationStatus> contingencyStatusMap = new HashMap<>();

        for (ContingencyWithRemedialAction nativeContingencyWithRemedialAction : linkedContingencyWithRemedialActions) {
            if (contingencyStatusMap.containsKey(nativeContingencyWithRemedialAction.contingency())) {
                contingencyStatusMap.put(nativeContingencyWithRemedialAction.contingency(), new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action %s with contingency %s ignored because this contingency has several conflictual links to the remedial action.".formatted(remedialActionId, nativeContingencyWithRemedialAction.contingency())));
                continue;
            }

            Contingency contingency = crac.getContingency(nativeContingencyWithRemedialAction.contingency());
            if (contingency == null) {
                contingencyStatusMap.put(nativeContingencyWithRemedialAction.contingency(), new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action %s with contingency %s ignored because this contingency does not exist or was not imported by Open RAO.".formatted(remedialActionId, nativeContingencyWithRemedialAction.contingency())));
                continue;
            }

            if (!nativeContingencyWithRemedialAction.normalEnabled()) {
                contingencyStatusMap.put(nativeContingencyWithRemedialAction.contingency(), new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action %s with contingency %s ignored because the association is disabled.".formatted(remedialActionId, nativeContingencyWithRemedialAction.contingency())));
                continue;
            }

            if (!ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeContingencyWithRemedialAction.combinationConstraintKind()) && !ElementCombinationConstraintKind.CONSIDERED.toString().equals(nativeContingencyWithRemedialAction.combinationConstraintKind())) {
                contingencyStatusMap.put(nativeContingencyWithRemedialAction.contingency(), new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action %s with contingency %s ignored because of an illegal combinationConstraintKind.".formatted(remedialActionId, nativeContingencyWithRemedialAction.contingency())));
                continue;
            }

            contingencyStatusMap.put(nativeContingencyWithRemedialAction.contingency(), new AssociationStatus(true, ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeContingencyWithRemedialAction.combinationConstraintKind()) ? ElementCombinationConstraintKind.INCLUDED : ElementCombinationConstraintKind.CONSIDERED, ""));
        }

        return contingencyStatusMap;
    }
}
