package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.CnecAdder;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class CnecCreator {
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

    protected CnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag operationalLimitPropertyBag, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext) {
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
    }

    protected Identifiable<?> getNetworkElementInNetwork(String networkElementId) {
        Identifiable<?> networkElement = network.getIdentifiable(networkElementId);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementId, network);
            if (cgmesBranchHelper.isValid()) {
                networkElement = cgmesBranchHelper.getBranch();
            }
        }

        if (networkElement instanceof DanglingLine) {
            Optional<TieLine> optionalTieLine = ((DanglingLine) networkElement).getTieLine();
            if (optionalTieLine.isPresent()) {
                networkElement = optionalTieLine.get();
            }
        }
        return networkElement;
    }

    // TODO: return rejected co

    protected String getCnecName(Instant instant) {
        return assessedElementName + " - " + instant;
    }

    protected void addCnecData(CnecAdder<?> cnecAdder, Contingency contingency, Instant instant) {
        String cnecName = getCnecName(instant);
        cnecAdder.withContingency(contingency == null ? null : contingency.getId())
                    .withId(cnecName)
                    .withName(cnecName)
                    .withInstant(instant);
    }

    protected void handleRejectedLinksAssessedElementContingency(String assessedElementId, String cnecName, String rejectedLinksAssessedElementContingency) {
        if (rejectedLinksAssessedElementContingency.isEmpty()) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "", false));
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "some cnec for the same assessed element are not imported because of incorrect data for assessed elements for contingencies : " + rejectedLinksAssessedElementContingency, true));
        }
    }
}
