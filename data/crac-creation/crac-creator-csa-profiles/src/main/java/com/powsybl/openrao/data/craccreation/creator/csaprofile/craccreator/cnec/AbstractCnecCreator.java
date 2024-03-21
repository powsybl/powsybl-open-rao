package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.CnecAdder;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    protected AbstractCnecCreator(Crac crac, Network network, String assessedElementId, String nativeAssessedElementName, String assessedElementOperator, boolean inBaseCase, PropertyBag operationalLimitPropertyBag, List<Contingency> linkedContingencies, Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, CsaProfileCracCreationContext cracCreationContext, String rejectedLinksAssessedElementContingency) {
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
        return "AssessedElement " + assessedElementId + " ignored because " + reason;
    }

    protected String getCnecName(String instantId, Contingency contingency) {
        // Need to include the mRID in the name in case the AssessedElement's name is not unique
        return "%s (%s) - %s%s".formatted(assessedElementName, assessedElementId, contingency == null ? "" : contingency.getName() + " - ", instantId);
    }

    protected String getCnecName(String instantId, Contingency contingency, int tatlDuration) {
        // Add TATL duration in case to CNECs of the same instant are created with different TATLs
        return "%s - TATL %s".formatted(getCnecName(instantId, contingency), tatlDuration);
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

    protected void markCnecAsImportedAndHandleRejectedContingencies(String instantId, Contingency contingency) {
        String cnecName = getCnecName(instantId, contingency);
        if (rejectedLinksAssessedElementContingency.isEmpty()) {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "", false));
        } else {
            csaProfileCnecCreationContexts.add(CsaProfileElementaryCreationContext.imported(assessedElementId, cnecName, cnecName, "some cnec for the same assessed element are not imported because of incorrect data for assessed elements for contingencies : " + rejectedLinksAssessedElementContingency, true));
        }
    }
}
