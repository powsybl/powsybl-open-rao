/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec.CsaProfileCnecCreationContext;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class OnConstraintUsageRuleHelper {
    private final Set<CsaProfileCnecCreationContext> csaProfileCnecCreationContexts;
    private final PropertyBags assessedElements;
    private final PropertyBags assessedElementsWithRemedialAction;

    private Map<String, Set<String>> importedFlowCnecsNativeIdsToFaraoNamesAndCombinableWithRa = new HashMap<>();
    private final Map<String, String> excludedAssessedElementsByRemedialAction = new HashMap<>();
    private final Map<String, String> includedAssessedElementsByRemedialAction = new HashMap<>();
    private final Map<String, String> consideredAssessedElementsByRemedialAction = new HashMap<>();

    public OnConstraintUsageRuleHelper(Set<CsaProfileCnecCreationContext> csaProfileCnecCreationContexts, PropertyBags assessedElements, PropertyBags assessedElementsWithRemedialAction) {
        this.csaProfileCnecCreationContexts = csaProfileCnecCreationContexts;
        this.assessedElements = assessedElements;
        this.assessedElementsWithRemedialAction = assessedElementsWithRemedialAction;
        readAssessedElements();
        readAssessedElementsWithRemedialAction();
    }

    public void readAssessedElementsWithRemedialAction() {
        try {
            assessedElementsWithRemedialAction.stream().filter(this::checkNormalEnabled).forEach(propertyBag -> {
                String combinationConstraintKind = propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND);
                if (combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
                    excludedAssessedElementsByRemedialAction.put(removePrefix(propertyBag.get(CsaProfileConstants.REQUEST_REMEDIAL_ACTION)), removePrefix(propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT)));
                } else if (combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString())) {
                    includedAssessedElementsByRemedialAction.put(removePrefix(propertyBag.get(CsaProfileConstants.REQUEST_REMEDIAL_ACTION)), removePrefix(propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT)));
                } else if (combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
                    consideredAssessedElementsByRemedialAction.put(removePrefix(propertyBag.get(CsaProfileConstants.REQUEST_REMEDIAL_ACTION)), removePrefix(propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT)));
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElementsWithRemedialAction: " + propertyBag.get("assessedElementWithRemedialAction") + " will not be imported because combinationConstraintKind is not in ['INCLUDED', 'EXCLUDED', 'CONSIDERED']");
                }
            });
        } catch (Exception e) {
            // FIXME:  how to say usage rule not added in context
        }
    }

    boolean checkNormalEnabled(PropertyBag propertyBag) {
        Optional<String> normalEnabledOpt = Optional.ofNullable(propertyBag.get(CsaProfileConstants.NORMAL_ENABLED));
        if (normalEnabledOpt.isPresent() && !Boolean.parseBoolean(normalEnabledOpt.get())) {
            throw new FaraoImportException(ImportStatus.NOT_FOR_RAO, String.format("AssessedElementWithRemedialAction '%s' will not be imported because field 'normalEnabled' in '%s' must be true or empty", propertyBag.get("mRID"), "AssessedElementWithRemedialAction"));
        }
        return true;
    }

    private void readAssessedElements() {
        importedFlowCnecsNativeIdsToFaraoNamesAndCombinableWithRa = this.getImportedFlowCnecsNativeIdsToFaraoIds();

        List<String> nativeIdsOfCnecsCombinableWithRas = assessedElements.stream()
                .filter(element -> element.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_REMEDIAL_ACTION) != null && Boolean.parseBoolean(element.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_REMEDIAL_ACTION)))
                .map(element -> removePrefix(element.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT))).collect(Collectors.toList());

        importedFlowCnecsNativeIdsToFaraoNamesAndCombinableWithRa.keySet().retainAll(nativeIdsOfCnecsCombinableWithRas);
    }

    public Map<String, Set<String>> getImportedFlowCnecsNativeIdsToFaraoNamesAndCombinableWithRa() {
        return this.importedFlowCnecsNativeIdsToFaraoNamesAndCombinableWithRa;
    }

    public Map<String, Set<String>> getImportedFlowCnecsNativeIdsToFaraoIds() {
        return csaProfileCnecCreationContexts.stream()
                .filter(CsaProfileCnecCreationContext::isImported)
                .collect(Collectors.groupingBy(
                        CsaProfileCnecCreationContext::getNativeId,
                        Collectors.mapping(CsaProfileCnecCreationContext::getFlowCnecName, Collectors.toSet())
                ));
    }

    public Map<String, String> getExcludedAssessedElementsByRemedialAction() {
        return this.excludedAssessedElementsByRemedialAction;
    }

    public Map<String, String> getIncludedAssessedElementsByRemedialAction() {
        return this.includedAssessedElementsByRemedialAction;
    }

    public Map<String, String> getConsideredAssessedElementsByRemedialAction() {
        return this.consideredAssessedElementsByRemedialAction;
    }

    private String removePrefix(String mridWithPrefix) {
        return mridWithPrefix.substring(mridWithPrefix.lastIndexOf("_") + 1);
    }
}
