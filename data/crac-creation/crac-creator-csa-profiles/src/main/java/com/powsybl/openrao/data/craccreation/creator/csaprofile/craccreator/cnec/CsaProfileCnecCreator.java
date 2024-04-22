/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.*;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec.nc.AssessedElementWithContingency;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;
import java.util.stream.Collectors;

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
    private final String regionEic;

    public CsaProfileCnecCreator(Crac crac, Network network, PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, PropertyBags voltageLimitsPropertyBags, PropertyBags angleLimitsPropertyBags, CsaProfileCracCreationContext cracCreationContext, Set<Side> defaultMonitoredSides, String regionEic) {
        this.crac = crac;
        this.network = network;
        this.assessedElementsPropertyBags = assessedElementsPropertyBags;
        this.assessedElementsWithContingenciesPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(assessedElementsWithContingenciesPropertyBags, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);
        this.currentLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(currentLimitsPropertyBags, CsaProfileConstants.REQUEST_CURRENT_LIMIT);
        this.voltageLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(voltageLimitsPropertyBags, CsaProfileConstants.REQUEST_VOLTAGE_LIMIT);
        this.angleLimitsPropertyBags = CsaProfileCracUtils.getMappedPropertyBagsSet(angleLimitsPropertyBags, CsaProfileConstants.REQUEST_VOLTAGE_ANGLE_LIMIT);
        this.cracCreationContext = cracCreationContext;
        this.defaultMonitoredSides = defaultMonitoredSides;
        this.regionEic = regionEic;
        this.createAndAddCnecs();
    }

    private void createAndAddCnecs() {
        csaProfileCnecCreationContexts = new HashSet<>();

        for (PropertyBag assessedElementPropertyBag : assessedElementsPropertyBags) {
            AssessedElement nativeAssessedElement = AssessedElement.fromPropertyBag(assessedElementPropertyBag);
            Set<AssessedElementWithContingency> nativeAssessedElementWithContingencies = assessedElementsWithContingenciesPropertyBags.getOrDefault(nativeAssessedElement.identifier(), Set.of()).stream().map(AssessedElementWithContingency::fromPropertyBag).collect(Collectors.toSet());
            try {
                addCnec(nativeAssessedElement, nativeAssessedElementWithContingencies);
            } catch (OpenRaoImportException exception) {
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(nativeAssessedElement.identifier(), exception.getImportStatus(), exception.getMessage()));
            }
        }
        cracCreationContext.setCnecCreationContexts(csaProfileCnecCreationContexts);
    }

    private void addCnec(AssessedElement nativeAssessedElement, Set<AssessedElementWithContingency> nativeAssessedElementWithContingencies) {
        String rejectedLinksAssessedElementContingency = "";

        if (!nativeAssessedElement.normalEnabled()) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "AssessedElement %s ignored because it is not enabled".formatted(nativeAssessedElement.identifier()));
        }

        if (!nativeAssessedElement.inBaseCase() && !nativeAssessedElement.isCombinableWithContingency() && nativeAssessedElementWithContingencies.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement %s ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found".formatted(nativeAssessedElement.identifier()));
        }

        Set<Contingency> combinableContingencies = nativeAssessedElement.isCombinableWithContingency() ? cracCreationContext.getCrac().getContingencies() : new HashSet<>();

        for (AssessedElementWithContingency assessedElementWithContingency : nativeAssessedElementWithContingencies) {
            if (!checkAndProcessCombinableContingencyFromExplicitAssociation(nativeAssessedElement.identifier(), assessedElementWithContingency, combinableContingencies)) {
                rejectedLinksAssessedElementContingency = rejectedLinksAssessedElementContingency.concat(assessedElementWithContingency.identifier() + " ");
            }
        }

        // We check whether the AssessedElement is defined using an OperationalLimit
        CsaProfileConstants.LimitType limitType = getLimit(nativeAssessedElement);

        checkAeScannedSecuredCoherence(nativeAssessedElement);

        boolean aeSecuredForRegion = isAeSecuredForRegion(nativeAssessedElement);
        boolean aeScannedForRegion = isAeScannedForRegion(nativeAssessedElement);

        // If not, we check if it is defined with a ConductingEquipment instead, otherwise we ignore
        if (limitType == null) {
            new FlowCnecCreator(crac, network, nativeAssessedElement, null, combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, defaultMonitoredSides, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addFlowCnecs();
            return;
        }

        if (CsaProfileConstants.LimitType.CURRENT.equals(limitType)) {
            new FlowCnecCreator(crac, network, nativeAssessedElement, getOperationalLimitPropertyBag(currentLimitsPropertyBags, nativeAssessedElement), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, defaultMonitoredSides, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addFlowCnecs();
        } else if (CsaProfileConstants.LimitType.VOLTAGE.equals(limitType)) {
            new VoltageCnecCreator(crac, network, nativeAssessedElement, getOperationalLimitPropertyBag(voltageLimitsPropertyBags, nativeAssessedElement), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addVoltageCnecs();
        } else {
            new AngleCnecCreator(crac, network, nativeAssessedElement, getOperationalLimitPropertyBag(angleLimitsPropertyBags, nativeAssessedElement), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addAngleCnecs();
        }
    }

    private void checkAeScannedSecuredCoherence(AssessedElement nativeAssessedElement) {
        if (nativeAssessedElement.securedForRegion() != null && nativeAssessedElement.securedForRegion().equals(nativeAssessedElement.scannedForRegion())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement " + nativeAssessedElement.identifier() + " ignored because an AssessedElement cannot be optimized and monitored at the same time");
        }
    }

    private boolean isAeSecuredForRegion(AssessedElement nativeAssessedElement) {
        String region = nativeAssessedElement.securedForRegion() == null ? null : CsaProfileCracUtils.getEicFromUrl(nativeAssessedElement.securedForRegion());
        return region != null && region.equals(regionEic);
    }

    private boolean isAeScannedForRegion(AssessedElement nativeAssessedElement) {
        String region = nativeAssessedElement.scannedForRegion() == null ? null : CsaProfileCracUtils.getEicFromUrl(nativeAssessedElement.scannedForRegion());
        return region != null && region.equals(regionEic);
    }

    private PropertyBag getOperationalLimitPropertyBag(Map<String, Set<PropertyBag>> operationalLimitPropertyBags, AssessedElement nativeAssessedElement) {
        return operationalLimitPropertyBags.get(nativeAssessedElement.operationalLimit()).stream().toList().get(0);
    }

    private CsaProfileConstants.LimitType getLimit(AssessedElement nativeAssessedElement) {
        if (checkLimit(this.currentLimitsPropertyBags, "current", nativeAssessedElement)) {
            return CsaProfileConstants.LimitType.CURRENT;
        }
        if (checkLimit(this.voltageLimitsPropertyBags, "voltage", nativeAssessedElement)) {
            return CsaProfileConstants.LimitType.VOLTAGE;
        }
        if (checkLimit(this.angleLimitsPropertyBags, "angle", nativeAssessedElement)) {
            return CsaProfileConstants.LimitType.ANGLE;
        }

        return null;
    }

    private boolean checkLimit(Map<String, Set<PropertyBag>> limitPropertyBags, String limitType, AssessedElement nativeAssessedElement) {
        Set<PropertyBag> limits = limitPropertyBags.get(nativeAssessedElement.operationalLimit());
        if (limits != null) {
            if (limits.size() != 1) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement %s ignored because more than one %s limit linked with the assessed element".formatted(nativeAssessedElement.identifier(), limitType));
            }
            return true;
        }
        return false;
    }

    private boolean checkAndProcessCombinableContingencyFromExplicitAssociation(String assessedElementId, AssessedElementWithContingency nativeAssessedElementWithContingency, Set<Contingency> combinableContingenciesSet) {
        Contingency contingencyToLink = crac.getContingency(nativeAssessedElementWithContingency.contingency());

        // Unknown contingency
        if (contingencyToLink == null) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "The contingency " + nativeAssessedElementWithContingency.contingency() + " linked to the assessed element does not exist in the CRAC"));
            return false;
        }

        // Illegal element combination constraint kind
        if (!CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeAssessedElementWithContingency.combinationConstraintKind())) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.INCONSISTENCY_IN_DATA, "The contingency " + nativeAssessedElementWithContingency.contingency() + " is linked to the assessed element with an illegal elementCombinationConstraint kind"));
            combinableContingenciesSet.remove(contingencyToLink);
            return false;
        }

        // Disabled link to contingency
        if (!nativeAssessedElementWithContingency.normalEnabled()) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(assessedElementId, ImportStatus.NOT_FOR_RAO, "The link between contingency " + nativeAssessedElementWithContingency.contingency() + " and the assessed element is disabled"));
            combinableContingenciesSet.remove(contingencyToLink);
            return false;
        }

        combinableContingenciesSet.add(contingencyToLink);
        return true;
    }
}
