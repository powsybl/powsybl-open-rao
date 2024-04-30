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
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.NcAggregator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.CurrentLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.VoltageLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithContingency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.VoltageAngleLimit;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCnecCreator {
    private final Crac crac;
    private final Network network;
    private final Set<AssessedElement> nativeAssessedElements;
    private final Map<String, Set<AssessedElementWithContingency>> nativeAssessedElementWithContingenciesPerNativeAssessedElement;
    private final Map<String, CurrentLimit> nativeCurrentLimitPerId;
    private final Map<String, VoltageLimit> nativeVoltageLimitPerId;
    private final Map<String, VoltageAngleLimit> nativeVoltageAngleLimitPerId;
    private Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts;
    private final CsaProfileCracCreationContext cracCreationContext;
    private final Set<Side> defaultMonitoredSides;
    private final String regionEic;

    public CsaProfileCnecCreator(Crac crac, Network network, Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithContingency> nativeAssessedElementsWithContingencies, Set<CurrentLimit> nativeCurrentLimits, Set<VoltageLimit> nativeVoltageLimits, Set<VoltageAngleLimit> nativeVoltageAngleLimits, CsaProfileCracCreationContext cracCreationContext, Set<Side> defaultMonitoredSides, String regionEic) {
        this.crac = crac;
        this.network = network;
        this.nativeAssessedElements = nativeAssessedElements;
        this.nativeAssessedElementWithContingenciesPerNativeAssessedElement = new NcAggregator<>(AssessedElementWithContingency::assessedElement).aggregate(nativeAssessedElementsWithContingencies);
        this.nativeCurrentLimitPerId = nativeCurrentLimits.stream().collect(Collectors.toMap(CurrentLimit::mrid, currentLimit -> currentLimit));
        this.nativeVoltageLimitPerId = nativeVoltageLimits.stream().collect(Collectors.toMap(VoltageLimit::mrid, voltageLimit -> voltageLimit));
        this.nativeVoltageAngleLimitPerId = nativeVoltageAngleLimits.stream().collect(Collectors.toMap(VoltageAngleLimit::mrid, voltageAngleLimit -> voltageAngleLimit));
        this.cracCreationContext = cracCreationContext;
        this.defaultMonitoredSides = defaultMonitoredSides;
        this.regionEic = regionEic;
        this.createAndAddCnecs();
    }

    private void createAndAddCnecs() {
        csaProfileCnecCreationContexts = new HashSet<>();

        for (AssessedElement nativeAssessedElement : nativeAssessedElements) {
            Set<AssessedElementWithContingency> nativeAssessedElementWithContingencies = nativeAssessedElementWithContingenciesPerNativeAssessedElement.getOrDefault(nativeAssessedElement.mrid(), Set.of());
            try {
                addCnec(nativeAssessedElement, nativeAssessedElementWithContingencies);
            } catch (OpenRaoImportException exception) {
                csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.notImported(nativeAssessedElement.mrid(), exception.getImportStatus(), exception.getMessage()));
            }
        }
        cracCreationContext.setCnecCreationContexts(csaProfileCnecCreationContexts);
    }

    private void addCnec(AssessedElement nativeAssessedElement, Set<AssessedElementWithContingency> nativeAssessedElementWithContingencies) {
        String rejectedLinksAssessedElementContingency = "";

        if (!nativeAssessedElement.normalEnabled()) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "AssessedElement %s ignored because it is not enabled".formatted(nativeAssessedElement.mrid()));
        }

        if (!nativeAssessedElement.inBaseCase() && !nativeAssessedElement.isCombinableWithContingency() && nativeAssessedElementWithContingencies.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement %s ignored because the assessed element is not in base case and not combinable with contingencies, but no explicit link to a contingency was found".formatted(nativeAssessedElement.mrid()));
        }

        Set<Contingency> combinableContingencies = nativeAssessedElement.isCombinableWithContingency() ? cracCreationContext.getCrac().getContingencies() : new HashSet<>();

        for (AssessedElementWithContingency assessedElementWithContingency : nativeAssessedElementWithContingencies) {
            if (!checkAndProcessCombinableContingencyFromExplicitAssociation(nativeAssessedElement.mrid(), assessedElementWithContingency, combinableContingencies)) {
                rejectedLinksAssessedElementContingency = rejectedLinksAssessedElementContingency.concat(assessedElementWithContingency.mrid() + " ");
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
            new FlowCnecCreator(crac, network, nativeAssessedElement, nativeCurrentLimitPerId.get(nativeAssessedElement.operationalLimit()), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, defaultMonitoredSides, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addFlowCnecs();
        } else if (CsaProfileConstants.LimitType.VOLTAGE.equals(limitType)) {
            new VoltageCnecCreator(crac, network, nativeAssessedElement, nativeVoltageLimitPerId.get(nativeAssessedElement.operationalLimit()), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addVoltageCnecs();
        } else {
            new AngleCnecCreator(crac, network, nativeAssessedElement, nativeVoltageAngleLimitPerId.get(nativeAssessedElement.operationalLimit()), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion).addAngleCnecs();
        }
    }

    private void checkAeScannedSecuredCoherence(AssessedElement nativeAssessedElement) {
        if (nativeAssessedElement.securedForRegion() != null && nativeAssessedElement.securedForRegion().equals(nativeAssessedElement.scannedForRegion())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "AssessedElement " + nativeAssessedElement.mrid() + " ignored because an AssessedElement cannot be optimized and monitored at the same time");
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

    private CsaProfileConstants.LimitType getLimit(AssessedElement nativeAssessedElement) {
        if (nativeCurrentLimitPerId.get(nativeAssessedElement.operationalLimit()) != null) {
            return CsaProfileConstants.LimitType.CURRENT;
        }
        if (nativeVoltageLimitPerId.get(nativeAssessedElement.operationalLimit()) != null) {
            return CsaProfileConstants.LimitType.VOLTAGE;
        }
        if (nativeVoltageAngleLimitPerId.get(nativeAssessedElement.operationalLimit()) != null) {
            return CsaProfileConstants.LimitType.ANGLE;
        }

        return null;
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
