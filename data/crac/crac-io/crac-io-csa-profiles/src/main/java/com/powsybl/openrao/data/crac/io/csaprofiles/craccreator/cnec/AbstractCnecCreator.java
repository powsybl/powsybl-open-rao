/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.cnec;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.crac.io.csaprofiles.nc.AssessedElement;
import com.powsybl.openrao.data.crac.io.csaprofiles.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.CnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractCnecCreator {
    protected final Crac crac;
    protected final Network network;
    protected final AssessedElement nativeAssessedElement;
    protected final Set<Contingency> linkedContingencies;
    protected final Set<ElementaryCreationContext> csaProfileCnecCreationContexts;
    protected final String rejectedLinksAssessedElementContingency;
    protected final boolean aeSecuredForRegion;
    protected final boolean aeScannedForRegion;
    protected final String border;

    protected AbstractCnecCreator(Crac crac, Network network, AssessedElement nativeAssessedElement, Set<Contingency> linkedContingencies, Set<ElementaryCreationContext> csaProfileCnecCreationContexts, String rejectedLinksAssessedElementContingency, CracCreationParameters cracCreationParameters, Map<String, String> borderPerTso, Map<String, String> borderPerEic) {
        this.crac = crac;
        this.network = network;
        this.nativeAssessedElement = nativeAssessedElement;
        this.linkedContingencies = linkedContingencies;
        this.csaProfileCnecCreationContexts = csaProfileCnecCreationContexts;
        this.rejectedLinksAssessedElementContingency = rejectedLinksAssessedElementContingency;
        String regionEic = cracCreationParameters.getExtension(CsaCracCreationParameters.class).getCapacityCalculationRegionEicCode();
        this.aeSecuredForRegion = isAeSecuredForRegion(regionEic);
        this.aeScannedForRegion = isAeScannedForRegion(regionEic);
        this.border = getCnecBorder(borderPerTso, borderPerEic);
    }

    private boolean isAeSecuredForRegion(String regionEic) {
        String region = nativeAssessedElement.securedForRegion() == null ? null : CsaProfileCracUtils.getEicFromUrl(nativeAssessedElement.securedForRegion());
        return region != null && region.equals(regionEic);
    }

    private boolean isAeScannedForRegion(String regionEic) {
        String region = nativeAssessedElement.scannedForRegion() == null ? null : CsaProfileCracUtils.getEicFromUrl(nativeAssessedElement.scannedForRegion());
        return region != null && region.equals(regionEic);
    }

    protected Identifiable<?> getNetworkElementInNetwork(String networkElementId) {
        Identifiable<?> networkElement = network.getIdentifiable(networkElementId);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementId, network);
            if (cgmesBranchHelper.isValid()) {
                networkElement = cgmesBranchHelper.getBranch();
            }
        }

        if (networkElement instanceof DanglingLine danglingLine) {
            Optional<TieLine> optionalTieLine = danglingLine.getTieLine();
            if (optionalTieLine.isPresent()) {
                networkElement = optionalTieLine.get();
            }
        }
        return networkElement;
    }

    protected String writeAssessedElementIgnoredReasonMessage(String reason) {
        return "AssessedElement " + nativeAssessedElement.mrid() + " ignored because " + reason;
    }

    protected String getCnecName(String instantId, Contingency contingency) {
        // Need to include the mRID in the name in case the AssessedElement's name is not unique
        return "%s (%s) - %s%s".formatted(nativeAssessedElement.getUniqueName(), nativeAssessedElement.mrid(), contingency == null ? "" : contingency.getName().orElse(contingency.getId()) + " - ", instantId);
    }

    protected String getCnecName(String instantId, Contingency contingency, TwoSides side, int acceptableDuration) {
        // Need to include the mRID in the name in case the AssessedElement's name is not unique
        // Add TATL duration in case to CNECs of the same instant are created with different TATLs
        return "%s (%s) - %s%s - %s%s".formatted(nativeAssessedElement.getUniqueName(), nativeAssessedElement.mrid(), contingency == null ? "" : contingency.getName().orElse(contingency.getId()) + " - ", instantId, side.name(), acceptableDuration == Integer.MAX_VALUE ? "" : " - TATL " + acceptableDuration);
    }

    protected void addCnecBaseInformation(CnecAdder<?> cnecAdder, Contingency contingency, String instantId) {
        String cnecName = getCnecName(instantId, contingency);
        initCnecAdder(cnecAdder, contingency, instantId, cnecName);
    }

    protected void addCnecBaseInformation(CnecAdder<?> cnecAdder, Contingency contingency, String instantId, TwoSides side, int acceptableDuration) {
        initCnecAdder(cnecAdder, contingency, instantId, getCnecName(instantId, contingency, side, acceptableDuration));
    }

    private void initCnecAdder(CnecAdder<?> cnecAdder, Contingency contingency, String instantId, String cnecName) {
        cnecAdder.withContingency(contingency == null ? null : contingency.getId())
            .withId(cnecName)
            .withName(cnecName)
            .withInstant(instantId)
            .withOperator(CsaProfileCracUtils.getTsoNameFromUrl(nativeAssessedElement.operator()))
            .withBorder(border);
        if (cnecAdder instanceof FlowCnecAdder) {
            // The following 2 lines mustn't be called for angle & voltage CNECs
            cnecAdder.withOptimized(aeSecuredForRegion)
                .withMonitored(aeScannedForRegion);
        }
    }

    protected void markCnecAsImportedAndHandleRejectedContingencies(String cnecName) {
        if (rejectedLinksAssessedElementContingency.isEmpty()) {
            csaProfileCnecCreationContexts.add(StandardElementaryCreationContext.imported(nativeAssessedElement.mrid(), cnecName, cnecName, false, ""));
        } else {
            csaProfileCnecCreationContexts.add(StandardElementaryCreationContext.imported(nativeAssessedElement.mrid(), cnecName, cnecName, true, "some cnec for the same assessed element are not imported because of incorrect data for assessed elements for contingencies : " + rejectedLinksAssessedElementContingency));
        }
    }

    protected String getCnecBorder(Map<String, String> borderPerTso, Map<String, String> borderPerEic) {
        if (nativeAssessedElement.overlappingZone() != null) {
            return borderPerEic.getOrDefault(CsaProfileCracUtils.getEicFromUrl(nativeAssessedElement.overlappingZone()), null);
        }
        return borderPerTso.getOrDefault(CsaProfileCracUtils.getTsoNameFromUrl(nativeAssessedElement.operator()), null);
    }
}
