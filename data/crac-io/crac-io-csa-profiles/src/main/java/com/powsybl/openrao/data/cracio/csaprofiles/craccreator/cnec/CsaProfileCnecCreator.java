/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.cnec;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.cracio.csaprofiles.CsaProfileCrac;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.NcAggregator;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.ElementCombinationConstraintKind;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.CurrentLimit;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.VoltageLimit;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.LimitType;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracUtils;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElement;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElementWithContingency;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.VoltageAngleLimit;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.Border;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.cracio.commons.OpenRaoImportException;

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
    private final Map<String, com.powsybl.openrao.data.cracio.csaprofiles.nc.VoltageAngleLimit> nativeVoltageAngleLimitPerId;
    private Set<ElementaryCreationContext> csaProfileCnecCreationContexts;
    private final CsaProfileCracCreationContext cracCreationContext;
    private final CracCreationParameters cracCreationParameters;
    private final String regionEic;
    private final Map<String, String> borderPerTso;
    private final Map<String, String> borderPerEic;

    public CsaProfileCnecCreator(Crac crac, Network network, CsaProfileCrac nativeCrac, CsaProfileCracCreationContext cracCreationContext, CracCreationParameters cracCreationParameters) {
        this.crac = crac;
        this.network = network;
        this.nativeAssessedElements = nativeCrac.getAssessedElements();
        this.nativeAssessedElementWithContingenciesPerNativeAssessedElement = new NcAggregator<>(AssessedElementWithContingency::assessedElement).aggregate(nativeCrac.getAssessedElementWithContingencies());
        this.nativeCurrentLimitPerId = nativeCrac.getCurrentLimits().stream().collect(Collectors.toMap(CurrentLimit::mrid, currentLimit -> currentLimit));
        this.nativeVoltageLimitPerId = nativeCrac.getVoltageLimits().stream().collect(Collectors.toMap(VoltageLimit::mrid, voltageLimit -> voltageLimit));
        this.nativeVoltageAngleLimitPerId = nativeCrac.getVoltageAngleLimits().stream().collect(Collectors.toMap(VoltageAngleLimit::mrid, voltageAngleLimit -> voltageAngleLimit));
        this.cracCreationContext = cracCreationContext;
        this.cracCreationParameters = cracCreationParameters;
        this.regionEic = cracCreationParameters.getExtension(CsaCracCreationParameters.class).getCapacityCalculationRegionEicCode();
        this.borderPerTso = cracCreationParameters.getExtension(CsaCracCreationParameters.class).getBorders().stream().collect(Collectors.toMap(Border::defaultForTso, Border::name));
        this.borderPerEic = cracCreationParameters.getExtension(CsaCracCreationParameters.class).getBorders().stream().collect(Collectors.toMap(Border::eic, Border::name));
        this.createAndAddCnecs();
    }

    private void createAndAddCnecs() {
        csaProfileCnecCreationContexts = new HashSet<>();

        for (AssessedElement nativeAssessedElement : nativeAssessedElements) {
            Set<AssessedElementWithContingency> nativeAssessedElementWithContingencies = nativeAssessedElementWithContingenciesPerNativeAssessedElement.getOrDefault(nativeAssessedElement.mrid(), Set.of());
            try {
                addCnec(nativeAssessedElement, nativeAssessedElementWithContingencies);
            } catch (OpenRaoImportException exception) {
                csaProfileCnecCreationContexts.add(StandardElementaryCreationContext.notImported(nativeAssessedElement.mrid(), null, exception.getImportStatus(), exception.getMessage()));
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
        com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.LimitType limitType = getLimit(nativeAssessedElement);

        checkAeScannedSecuredCoherence(nativeAssessedElement);

        boolean aeSecuredForRegion = isAeSecuredForRegion(nativeAssessedElement);
        boolean aeScannedForRegion = isAeScannedForRegion(nativeAssessedElement);

        // If not, we check if it is defined with a ConductingEquipment instead, otherwise we ignore
        if (limitType == null) {
            new FlowCnecCreator(crac, network, nativeAssessedElement, null, combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion, cracCreationParameters, borderPerTso, borderPerEic).addFlowCnecs();
            return;
        }

        if (com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.LimitType.CURRENT.equals(limitType)) {
            new FlowCnecCreator(crac, network, nativeAssessedElement, nativeCurrentLimitPerId.get(nativeAssessedElement.operationalLimit()), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion, cracCreationParameters, borderPerTso, borderPerEic).addFlowCnecs();
        } else if (com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.LimitType.VOLTAGE.equals(limitType)) {
            new VoltageCnecCreator(crac, network, nativeAssessedElement, nativeVoltageLimitPerId.get(nativeAssessedElement.operationalLimit()), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion, borderPerTso, borderPerEic).addVoltageCnecs();
        } else {
            new AngleCnecCreator(crac, network, nativeAssessedElement, nativeVoltageAngleLimitPerId.get(nativeAssessedElement.operationalLimit()), combinableContingencies.stream().toList(), csaProfileCnecCreationContexts, cracCreationContext, rejectedLinksAssessedElementContingency, aeSecuredForRegion, aeScannedForRegion, borderPerTso, borderPerEic).addAngleCnecs();
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

    private com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.LimitType getLimit(AssessedElement nativeAssessedElement) {
        if (nativeCurrentLimitPerId.get(nativeAssessedElement.operationalLimit()) != null) {
            return com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.LimitType.CURRENT;
        }
        if (nativeVoltageLimitPerId.get(nativeAssessedElement.operationalLimit()) != null) {
            return com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.LimitType.VOLTAGE;
        }
        if (nativeVoltageAngleLimitPerId.get(nativeAssessedElement.operationalLimit()) != null) {
            return LimitType.ANGLE;
        }

        return null;
    }

    private boolean checkAndProcessCombinableContingencyFromExplicitAssociation(String assessedElementId, AssessedElementWithContingency nativeAssessedElementWithContingency, Set<Contingency> combinableContingenciesSet) {
        Contingency contingencyToLink = crac.getContingency(nativeAssessedElementWithContingency.contingency());

        // Unknown contingency
        if (contingencyToLink == null) {
            csaProfileCnecCreationContexts.add(StandardElementaryCreationContext.notImported(assessedElementId, null, ImportStatus.INCONSISTENCY_IN_DATA, "The contingency " + nativeAssessedElementWithContingency.contingency() + " linked to the assessed element does not exist in the CRAC"));
            return false;
        }

        // Illegal element combination constraint kind
        if (!ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeAssessedElementWithContingency.combinationConstraintKind())) {
            csaProfileCnecCreationContexts.add(StandardElementaryCreationContext.notImported(assessedElementId, null, ImportStatus.INCONSISTENCY_IN_DATA, "The contingency " + nativeAssessedElementWithContingency.contingency() + " is linked to the assessed element with an illegal elementCombinationConstraint kind"));
            combinableContingenciesSet.remove(contingencyToLink);
            return false;
        }

        // Disabled link to contingency
        if (!nativeAssessedElementWithContingency.normalEnabled()) {
            csaProfileCnecCreationContexts.add(StandardElementaryCreationContext.notImported(assessedElementId, null, ImportStatus.NOT_FOR_RAO, "The link between contingency " + nativeAssessedElementWithContingency.contingency() + " and the assessed element is disabled"));
            combinableContingenciesSet.remove(contingencyToLink);
            return false;
        }

        combinableContingenciesSet.add(contingencyToLink);
        return true;
    }
}
