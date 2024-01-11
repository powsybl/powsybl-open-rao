package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.open_rao.data.crac_api.Contingency;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.NetworkElement;
import com.powsybl.open_rao.data.crac_api.cnec.CnecAdder;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.powsybl.open_rao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractCnecCreator {
    protected final Crac crac;
    protected final Network network;
    protected final String assessedElementId;
    protected final String nativeAssessedElementName;
    protected final String assessedElementName;
    protected final String assessedElementOperator;
    protected final boolean inBaseCase;
    protected final List<Contingency> linkedContingencies;
    protected final PropertyBag operationalLimitPropertyBag;
    protected Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts;
    protected final CsaProfileCracCreationContext cracCreationContext;
    protected final String rejectedLinksAssessedElementContingency;
    private final boolean useGeographicalFilter;

    protected AbstractCnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag operationalLimitPropertyBag, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, String rejectedLinksAssessedElementContingency, boolean useGeographicalFilter) {
        this.crac = crac;
        this.network = network;
        this.assessedElementId = assessedElementId;
        this.nativeAssessedElementName = nativeAssessedElementName;
        this.assessedElementOperator = assessedElementOperator;
        this.assessedElementName = CsaProfileCracUtils.getUniqueName(assessedElementOperator, nativeAssessedElementName);
        this.inBaseCase = inBaseCase;
        this.operationalLimitPropertyBag = operationalLimitPropertyBag;
        this.linkedContingencies = linkedContingencies;
        this.csaProfileCnecCreationContexts = csaProfileCnecCreationContexts;
        this.cracCreationContext = cracCreationContext;
        this.rejectedLinksAssessedElementContingency = rejectedLinksAssessedElementContingency;
        this.useGeographicalFilter = useGeographicalFilter;
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
        return "Assessed Element " + assessedElementId + " ignored because " + reason + ".";
    }

    protected String getCnecName(String instantId, Contingency contingency) {
        // Need to include the mRID in the name in case the AssessedElement's name is not unique
        return assessedElementName + " (" + assessedElementId + ") - " + (contingency == null ? "" : contingency.getName() + " - ") + instantId;
    }

    protected String getCnecName(String instantId, Contingency contingency, int tatlDuration) {
        // Need to include the mRID in the name in case the AssessedElement's name is not unique
        return assessedElementName + " (" + assessedElementId + ") - " + (contingency == null ? "" : contingency.getName() + " - ") + instantId + " - TATL " + tatlDuration;
    }

    protected void addCnecBaseInformation(CnecAdder<?> cnecAdder, Contingency contingency, String instantId) {
        String cnecName = getCnecName(instantId, contingency);
        cnecAdder.withContingency(contingency == null ? null : contingency.getId())
                .withId(cnecName)
                .withName(cnecName)
                .withInstant(instantId);
    }

    protected void addCnecBaseInformation(CnecAdder<?> cnecAdder, Contingency contingency, String instantId, int tatlDuration) {
        String cnecName = getCnecName(instantId, contingency, tatlDuration);
        cnecAdder.withContingency(contingency == null ? null : contingency.getId())
                .withId(cnecName)
                .withName(cnecName)
                .withInstant(instantId);
    }

    protected void markCnecAsImportedAndHandleRejectedContingencies(String cnecName) {
        if (rejectedLinksAssessedElementContingency.isEmpty()) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "", false));
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "some cnec for the same assessed element are not imported because of incorrect data for assessed elements for contingencies : " + rejectedLinksAssessedElementContingency, true));
        }
    }

    protected boolean incompatibleLocationsBetweenCnecNetworkElementsAndContingency(Set<String> cnecElementsIds, Contingency contingency) {
        if (!useGeographicalFilter || contingency == null) {
            return false;
        }
        return !GeographicalFilter.networkElementsShareCommonCountry(cnecElementsIds, contingency.getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet()), network);
    }

    protected boolean incompatibleLocationsBetweenCnecNetworkElementsAndContingency(String cnecElementId, Contingency contingency) {
        return incompatibleLocationsBetweenCnecNetworkElementsAndContingency(Set.of(cnecElementId), contingency);
    }
}
