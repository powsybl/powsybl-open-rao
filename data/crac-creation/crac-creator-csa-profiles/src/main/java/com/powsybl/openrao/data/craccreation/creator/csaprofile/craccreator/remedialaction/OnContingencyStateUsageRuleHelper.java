/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.COMBINATION_CONSTRAINT_KIND;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.REQUEST_CONTINGENCY;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
public final class OnContingencyStateUsageRuleHelper {

    private OnContingencyStateUsageRuleHelper() { }

    public static Map<String, AssociationStatus> processContingenciesLinkedToRemedialAction(Crac crac, String remedialActionId, Set<PropertyBag> linkedContingencyWithRemedialActions) {
        Map<String, AssociationStatus> contingencyStatusMap = new HashMap<>();

        for (PropertyBag contingencyWithRemedialActionPropertyBag : linkedContingencyWithRemedialActions) {
            String contingencyId = contingencyWithRemedialActionPropertyBag.getId(REQUEST_CONTINGENCY);
            Contingency contingency = crac.getContingency(contingencyId);
            String combinationConstraintKindStr = contingencyWithRemedialActionPropertyBag.get(COMBINATION_CONSTRAINT_KIND);
            Optional<String> normalEnabledOpt = Optional.ofNullable(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.NORMAL_ENABLED));

            if (contingency == null) {
                contingencyStatusMap.put(contingencyId, new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action %s with contingency %s ignored because this contingency does not exist or was not imported by Open RAO.".formatted(remedialActionId, contingencyId)));
                continue;
            }

            if (normalEnabledOpt.isPresent() && !Boolean.parseBoolean(normalEnabledOpt.get())) {
                contingencyStatusMap.put(contingencyId, new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action %s with contingency %s ignored because the association is disabled.".formatted(remedialActionId, contingencyId)));
                continue;
            }

            if (!combinationConstraintKindStr.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKindStr.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
                contingencyStatusMap.put(contingencyId, new AssociationStatus(false, null, "OnContingencyState usage rule for remedial action %s with contingency %s ignored because of an illegal combinationConstraintKind.".formatted(remedialActionId, contingencyId)));
                continue;
            }

            CsaProfileConstants.ElementCombinationConstraintKind combinationConstraintKind = CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(combinationConstraintKindStr) ? CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED : CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED;
            if (contingencyStatusMap.containsKey(contingencyId) && combinationConstraintKind != contingencyStatusMap.get(contingencyId).elementCombinationConstraintKind()) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action %s will not be imported because the ElementCombinationConstraintKinds that link the remedial action to the contingency %s are different".formatted(remedialActionId, contingencyId));
            }

            contingencyStatusMap.put(contingencyId, new AssociationStatus(true, combinationConstraintKind, ""));
        }

        return contingencyStatusMap;
    }
}
