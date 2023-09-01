package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec.CsaProfileCnecCreationContext;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;
import java.util.stream.Collectors;

public class OnConstraintUsageRuleHelper {
    private final Set<CsaProfileCnecCreationContext> csaProfileCnecCreationContexts;
    private final PropertyBags assessedElements;
    private final PropertyBags assessedElementsWithRemedialAction;

    private Map<String, Set<String>> importedFlowCnecsNativeIdsToFaraoIdsAndCombinableWithRa = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> importedFlowCnecsNativeIdsToFaraoIds = new HashMap<>();

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
                    excludedAssessedElementsByRemedialAction.put(propertyBag.get("remedialAction"), propertyBag.get("assessedElement"));
                } else if (combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString())) {
                    includedAssessedElementsByRemedialAction.put(propertyBag.get("remedialAction"), propertyBag.get("assessedElement"));
                } else if (combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
                    consideredAssessedElementsByRemedialAction.put(propertyBag.get("remedialAction"), propertyBag.get("assessedElement"));
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElementsWithRemedialAction: " + propertyBag.get("mRID") + " will not be imported because combinationConstraintKind is not in ['INCLUDED', 'EXCLUDED', 'CONSIDERED']");
                }
            });
        } catch (Exception e) {
            // TODO add not imported to context ?
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
        importedFlowCnecsNativeIdsToFaraoIdsAndCombinableWithRa = fillNativeToFaraoFlowCnecIds();

        List<String> nativeIdsOfCnecsCombinableWithRas = assessedElements.stream()
                .filter(element -> Boolean.parseBoolean(element.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_REMEDIAL_ACTION)))
                .map(element -> element.get("mRID")).collect(Collectors.toList());

        importedFlowCnecsNativeIdsToFaraoIdsAndCombinableWithRa.keySet().retainAll(nativeIdsOfCnecsCombinableWithRas);
    }

    private Map<String, Set<String>> fillNativeToFaraoFlowCnecIds() {
        importedFlowCnecsNativeIdsToFaraoIds = csaProfileCnecCreationContexts.stream()
                .filter(CsaProfileCnecCreationContext::isImported)
                .collect(Collectors.groupingBy(
                        CsaProfileCnecCreationContext::getNativeId,
                        Collectors.mapping(CsaProfileCnecCreationContext::getFlowCnecId, Collectors.toSet())
                ));
        return importedFlowCnecsNativeIdsToFaraoIds;
    }

    public Map<String, Set<String>> getImportedFlowCnecsNativeIdsToFaraoIdsAndCombinableWithRa() {
        return importedFlowCnecsNativeIdsToFaraoIdsAndCombinableWithRa;
    }

    public Map<String, Set<String>> getImportedFlowCnecsNativeIdsToFaraoIds() {
        return importedFlowCnecsNativeIdsToFaraoIds;
    }

    public Map<String, String> getExcludedAssessedElementsByRemedialAction() {
        return excludedAssessedElementsByRemedialAction;
    }

    public Map<String, String> getIncludedAssessedElementsByRemedialAction() {
        return includedAssessedElementsByRemedialAction;
    }

    public Map<String, String> getConsideredAssessedElementsByRemedialAction() {
        return consideredAssessedElementsByRemedialAction;
    }
}
