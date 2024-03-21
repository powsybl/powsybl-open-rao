/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.*;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.*;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCnecCreator {
    private final Crac crac;
    private final Network network;
    private final PropertyBags assessedElementsPropertyBags;
    private final Map<String, Set<PropertyBag>> assessedElementsWithContingenciesPropertyBags;
    private final Map<String, Set<PropertyBag>> currentLimitsPropertyBags;
    private final Map<String, Set<PropertyBag>> voltageLimitsPropertyBags;
    private final Map<String, Set<PropertyBag>> angleLimitsPropertyBags;
    private Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts;
    private final CsaProfileCracCreationContext cracCreationContext;
    private final Set<Side> defaultMonitoredSides;

    public CsaProfileCnecCreator(Crac crac, Network network, PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, PropertyBags voltageLimitsPropertyBags, PropertyBags angleLimitsPropertyBags, CsaProfileCracCreationContext cracCreationContext, Set<Side> defaultMonitoredSides) {
        this.crac = crac;
        this.network = network;
        this.assessedElementsPropertyBags = assessedElementsPropertyBags;
        this.assessedElementsWithContingenciesPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(assessedElementsWithContingenciesPropertyBags, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
        this.currentLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(currentLimitsPropertyBags, CsaProfileConstants.REQUEST_CURRENT_LIMIT);
        this.voltageLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(voltageLimitsPropertyBags, CsaProfileConstants.REQUEST_VOLTAGE_LIMIT);
        this.angleLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(angleLimitsPropertyBags, CsaProfileConstants.REQUEST_VOLTAGE_ANGLE_LIMIT);
        this.cracCreationContext = cracCreationContext;
        this.defaultMonitoredSides = defaultMonitoredSides;
        this.createAndAddCnecs();
    }

    private void createAndAddCnecs() {
        this.csaProfileCnecCreationContexts = new HashSet<>();

        for (PropertyBag assessedElementPropertyBag : assessedElementsPropertyBags) {
            this.addCnec(assessedElementPropertyBag);
        }
        this.cracCreationContext.setCnecCreationContexts(this.csaProfileCnecCreationContexts);
    }

    private void addCnec(PropertyBag assessedElementPropertyBag) {
        String rejectedLinksAssessedElementContingency = "";
        String assessedElementId = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
        boolean isAeProfileDataCheckOk = this.aeProfileDataCheck(assessedElementId, assessedElementPropertyBag);

        if (!isAeProfileDataCheckOk) {
            return;
        }

        String inBaseCaseStr = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IN_BASE_CASE);
        boolean inBaseCase = Boolean.parseBoolean(inBaseCaseStr);

        Set<PropertyBag> assessedElementsWithContingencies = this.assessedElementsWithContingenciesPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT));

        String isCombinableWithContingencyStr = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_CONTINGENCY);
        boolean isCombinableWithContingency = Boolean.parseBoolean(isCombinableWithContingencyStr);

        if (!inBaseCase && !isCombinableWithContingency && assessedElementsWithContingencies == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement %s ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found".formatted(assessedElementId)));
            return;
        }

        Set<Contingency> combinableContingencies;
        if (isCombinableWithContingency) {
            combinableContingencies = cracCreationContext.getCrac().getContingencies();
        } else {
            combinableContingencies = new HashSet<>();
        }

        String nativeAssessedElementName = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NAME);
        String assessedSystemOperator = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATOR);

        if (assessedElementsWithContingencies != null) {
            for (PropertyBag assessedElementWithContingencies : assessedElementsWithContingencies) {
                boolean isCheckLinkOk = this.checkLinkAssessedElementContingency(assessedElementId, assessedElementWithContingencies, combinableContingencies, isCombinableWithContingency);
                if (!isCheckLinkOk) {
                    rejectedLinksAssessedElementContingency = rejectedLinksAssessedElementContingency.concat(assessedElementWithContingencies.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY) + " ");
                }
            }
        }

        // We check whether the AssessedElement is defined using an OperationalLimit
        CsaProfileConstants.LimitType limitType = getLimit(assessedElementId, assessedElementPropertyBag);
        String conductingEquipment = assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_CONDUCTING_EQUIPMENT);

        // If not, we check if it is defined with a ConductingEquipment instead, otherwise we ignore
        if (limitType == null) {
            new FlowCnecCreator(crac, network, assessedElementId, nativeAssessedElementName, assessedSystemOperator, inBaseCase, null, conductingEquipment, combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, defaultMonitoredSides, rejectedLinksAssessedElementContingency).addFlowCnecs();
            return;
        }

        if (CsaProfileConstants.LimitType.CURRENT.equals(limitType)) {
            new FlowCnecCreator(crac, network, assessedElementId, nativeAssessedElementName, assessedSystemOperator, inBaseCase, getOperationalLimitPropertyBag(currentLimitsPropertyBags, assessedElementPropertyBag), conductingEquipment, combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, defaultMonitoredSides, rejectedLinksAssessedElementContingency).addFlowCnecs();
        } else if (CsaProfileConstants.LimitType.VOLTAGE.equals(limitType)) {
            new VoltageCnecCreator(crac, network, assessedElementId, nativeAssessedElementName, assessedSystemOperator, inBaseCase, getOperationalLimitPropertyBag(voltageLimitsPropertyBags, assessedElementPropertyBag), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency).addVoltageCnecs();
        } else {
            new AngleCnecCreator(crac, network, assessedElementId, nativeAssessedElementName, assessedSystemOperator, inBaseCase, getOperationalLimitPropertyBag(angleLimitsPropertyBags, assessedElementPropertyBag), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency).addAngleCnecs();
        }
    }

    private PropertyBag getOperationalLimitPropertyBag(Map<String, Set<PropertyBag>> operationalLimitPropertyBags, PropertyBag assessedElementPropertyBag) {
        return operationalLimitPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT)).stream().toList().get(0);
    }

    private boolean aeProfileDataCheck(String assessedElementId, PropertyBag assessedElementPropertyBag) {
        String normalEnabled = assessedElementPropertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NORMAL_ENABLED);

        if (normalEnabled != null && !Boolean.parseBoolean(normalEnabled)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "AssessedElement.normalEnabled is false"));
            return false;
        }
        return true;
    }

    private CsaProfileConstants.LimitType getLimit(String assessedElementId, PropertyBag assessedElementPropertyBag) {
        if (checkLimit(this.currentLimitsPropertyBags, "current", assessedElementId, assessedElementPropertyBag)) {
            return CsaProfileConstants.LimitType.CURRENT;
        }
        if (checkLimit(this.voltageLimitsPropertyBags, "voltage", assessedElementId, assessedElementPropertyBag)) {
            return CsaProfileConstants.LimitType.VOLTAGE;
        }
        if (checkLimit(this.angleLimitsPropertyBags, "angle", assessedElementId, assessedElementPropertyBag)) {
            return CsaProfileConstants.LimitType.ANGLE;
        }

        return null;
    }

    private boolean checkLimit(Map<String, Set<PropertyBag>> limitPropertyBags, String limitType, String assessedElementId, PropertyBag assessedElementPropertyBag) {
        Set<PropertyBag> limits = limitPropertyBags.get(assessedElementPropertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT));
        if (limits != null) {
            if (limits.size() != 1) {
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "more than one " + limitType + " limit linked with the assessed element"));
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean checkLinkAssessedElementContingency(String assessedElementId, PropertyBag assessedElementWithContingencies, Set<Contingency> combinableContingenciesSet, boolean isCombinableWithContingency) {
        String normalEnabledWithContingencies = assessedElementWithContingencies.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_NORMAL_ENABLED);
        String contingencyId = assessedElementWithContingencies.getId(CsaProfileConstants.REQUEST_CONTINGENCY);

        if (normalEnabledWithContingencies != null && !Boolean.parseBoolean(normalEnabledWithContingencies)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "AssessedElementWithContingency.normalEnabled is false for contingency " + contingencyId));
            return false;
        }

        String combinationConstraintKind = assessedElementWithContingencies.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_COMBINATION_CONSTRAINT_KIND);
        if (CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString().equals(combinationConstraintKind)) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElementWithContingency.combinationConstraintKind is considered"));
            return false;
        }
        if (CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(combinationConstraintKind) && !isCombinableWithContingency) {
            Contingency contingencyToLink = crac.getContingency(contingencyId);
            if (contingencyToLink == null) {
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "the contingency "
                        + contingencyId + " linked to the assessed element doesn't exist in the CRAC"));
                return false;
            } else {
                combinableContingenciesSet.add(contingencyToLink);
                return true;
            }
        }
        if (CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString().equals(combinationConstraintKind) && isCombinableWithContingency) {
            Contingency contingencyToRemove = crac.getContingency(contingencyId);
            if (contingencyToRemove == null) {
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "the contingency "
                        + contingencyId + " excluded from the contingencies linked to the assessed element doesn't exist in the CRAC"));
                return false;
            } else {
                combinableContingenciesSet.remove(contingencyToRemove);
                return true;
            }
        }
        csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElementWithContingency.combinationConstraintKind = "
                + combinationConstraintKind + " and AssessedElement.isCombinableWithContingency = " + isCombinableWithContingency + " have inconsistent values"));
        return false;
    }
}
