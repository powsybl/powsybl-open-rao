/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.cnec;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.CnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.cgmes.CgmesBranchHelper;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.objects.AssessedElement;
import com.powsybl.openrao.data.crac.io.nc.parameters.CapacityCalculationRegion;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;

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
    protected final Set<ElementaryCreationContext> ncCnecCreationContexts;
    protected final String rejectedLinksAssessedElementContingency;
    protected final boolean aeSecuredForRegion;
    protected final boolean aeScannedForRegion;
    protected final String border;
    protected final CracCreationParameters cracCreationParameters;

    protected AbstractCnecCreator(Crac crac,
                                  Network network,
                                  AssessedElement nativeAssessedElement,
                                  Set<Contingency> linkedContingencies,
                                  Set<ElementaryCreationContext> ncCnecCreationContexts,
                                  String rejectedLinksAssessedElementContingency,
                                  CracCreationParameters cracCreationParameters) {
        this.crac = crac;
        this.network = network;
        this.nativeAssessedElement = nativeAssessedElement;
        this.linkedContingencies = linkedContingencies;
        this.ncCnecCreationContexts = ncCnecCreationContexts;
        this.rejectedLinksAssessedElementContingency = rejectedLinksAssessedElementContingency;
        CapacityCalculationRegion capacityCalculationRegion = cracCreationParameters.getExtension(NcCracCreationParameters.class).getCapacityCalculationRegion();
        this.aeSecuredForRegion = isAeSecuredForRegion(capacityCalculationRegion);
        this.aeScannedForRegion = isAeScannedForRegion(capacityCalculationRegion);
        this.border = getBorderEIC(nativeAssessedElement.overlappingZone());
        this.cracCreationParameters = cracCreationParameters;
    }

    private boolean isAeSecuredForRegion(CapacityCalculationRegion capacityCalculationRegion) {
        String region = nativeAssessedElement.securedForRegion() == null ? null : NcCracUtils.getEicFromUrl(nativeAssessedElement.securedForRegion());
        return region != null && region.equals(capacityCalculationRegion.getEIC());
    }

    private boolean isAeScannedForRegion(CapacityCalculationRegion capacityCalculationRegion) {
        String region = nativeAssessedElement.scannedForRegion() == null ? null : NcCracUtils.getEicFromUrl(nativeAssessedElement.scannedForRegion());
        return region != null && region.equals(capacityCalculationRegion.getEIC());
    }

    private static String getBorderEIC(String overlappingZone) {
        return overlappingZone == null ? null : NcCracUtils.getEicFromUrl(overlappingZone);
    }

    protected Identifiable<?> getNetworkElementInNetwork(String networkElementId) {
        Identifiable<?> networkElement = network.getIdentifiable(networkElementId);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementId, network);
            if (cgmesBranchHelper.isValid()) {
                networkElement = cgmesBranchHelper.getBranch();
            }
        }

        if (networkElement instanceof BoundaryLine boundaryLine) {
            Optional<TieLine> optionalTieLine = boundaryLine.getTieLine();
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
        return "%s (%s) - %s%s".formatted(
            nativeAssessedElement.getUniqueName(),
            nativeAssessedElement.mrid(),
            contingency == null ? "" : contingency.getName().orElse(contingency.getId()) + " - ",
            instantId
        );
    }

    protected String getCnecName(String instantId, Contingency contingency, int acceptableDuration) {
        // Need to include the mRID in the name in case the AssessedElement's name is not unique
        // Add TATL duration in case to CNECs of the same instant are created with different TATLs
        String operationalLimitSuffix = acceptableDuration == Integer.MAX_VALUE ? "PATL" : "TATL " + acceptableDuration;
        return getCnecName(instantId, contingency) + " - " + operationalLimitSuffix;
    }

    protected void addCnecBaseInformation(CnecAdder<?> cnecAdder, Contingency contingency, String instantId) {
        String cnecName = getCnecName(instantId, contingency);
        initCnecAdder(cnecAdder, contingency, instantId, cnecName);
    }

    protected void addCnecBaseInformation(CnecAdder<?> cnecAdder, Contingency contingency, String instantId, int acceptableDuration) {
        initCnecAdder(cnecAdder, contingency, instantId, getCnecName(instantId, contingency, acceptableDuration));
    }

    private void initCnecAdder(CnecAdder<?> cnecAdder, Contingency contingency, String instantId, String cnecName) {
        cnecAdder.withContingency(contingency == null ? null : contingency.getId())
            .withId(cnecName)
            .withName(cnecName)
            .withInstant(instantId)
            .withOperator(NcCracUtils.getTsoNameFromUrl(nativeAssessedElement.operator()))
            .withBorder(border);
        if (cnecAdder instanceof FlowCnecAdder) {
            // The following 2 lines mustn't be called for angle & voltage CNECs
            cnecAdder.withOptimized(aeSecuredForRegion)
                .withMonitored(aeScannedForRegion);
        }
    }

    protected void markCnecAsImportedAndHandleRejectedContingencies(String cnecName) {
        if (rejectedLinksAssessedElementContingency.isEmpty()) {
            ncCnecCreationContexts.add(StandardElementaryCreationContext.imported(nativeAssessedElement.mrid(), cnecName, cnecName, false, ""));
        } else {
            ncCnecCreationContexts.add(StandardElementaryCreationContext.imported(
                nativeAssessedElement.mrid(), cnecName, cnecName, true,
                "some cnec for the same assessed element are not imported because of incorrect data for assessed elements for contingencies : " + rejectedLinksAssessedElementContingency
            ));
        }
    }

    protected VoltageLevel getVoltageLevel(Identifiable<?> networkElement) {
        if (networkElement.getType().equals(IdentifiableType.BUS)) {
            return network.getBusBreakerView().getBus(networkElement.getId()).getVoltageLevel();
        }
        if (networkElement.getType().equals(IdentifiableType.BUSBAR_SECTION)) {
            return network.getBusbarSection(networkElement.getId()).getTerminal().getVoltageLevel();
        }
        throw new OpenRaoImportException(
            ImportStatus.INCONSISTENCY_IN_DATA,
            writeAssessedElementIgnoredReasonMessage("the network element " + networkElement.getId() + " is neither a bus nor a bus bar section")
        );
    }
}
