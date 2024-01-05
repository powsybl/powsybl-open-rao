package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.CnecAdder;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.triplestore.api.PropertyBag;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

        if (networkElement instanceof DanglingLine) {
            Optional<TieLine> optionalTieLine = ((DanglingLine) networkElement).getTieLine();
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

    protected Set<Optional<Country>> getContingencyLocations(Contingency contingency) {
        Set<Optional<Country>> locations = new HashSet();
        contingency.getNetworkElements().forEach(networkElement -> locations.addAll(networkElement.getLocation(network)));
        return locations;
    }

    public Set<Optional<Country>> getNetworkElementLocation(String networkElementId) {
        Identifiable<?> ne = network.getIdentifiable(networkElementId);
        if (Objects.isNull(ne)) {
            throw new FaraoException("Network element " + networkElementId + " was not found in the network.");
        } else if (ne instanceof Branch) {
            Branch<?> branch = (Branch) ne;
            Optional<Country> country1 = getSubstationCountry(branch.getTerminal1().getVoltageLevel().getSubstation());
            Optional<Country> country2 = getSubstationCountry(branch.getTerminal2().getVoltageLevel().getSubstation());
            if (country1.equals(country2)) {
                return Set.of(country1);
            } else {
                return Set.of(country1, country2);
            }
        } else if (ne instanceof Switch) {
            return Set.of(getSubstationCountry(((Switch) ne).getVoltageLevel().getSubstation()));
        } else if (ne instanceof Injection) {
            return Set.of(getSubstationCountry(((Injection<?>) ne).getTerminal().getVoltageLevel().getSubstation()));
        } else if (ne instanceof Bus) {
            return Set.of(getSubstationCountry(((Bus) ne).getVoltageLevel().getSubstation()));
        } else if (ne instanceof VoltageLevel) {
            return Set.of(getSubstationCountry(((VoltageLevel) ne).getSubstation()));
        } else if (ne instanceof Substation) {
            return Set.of(((Substation) ne).getCountry());
        } else if (ne instanceof HvdcLine) {
            return Set.of(getSubstationCountry(((HvdcLine) ne).getConverterStation1().getTerminal().getVoltageLevel().getSubstation()), getSubstationCountry(((HvdcLine) ne).getConverterStation2().getTerminal().getVoltageLevel().getSubstation()));
        }  else {
            throw new NotImplementedException("Don't know how to figure out the location of " + ne.getId() + " of type " + ne.getClass());
        }
    }

    private Optional<Country> getSubstationCountry(Optional<Substation> substation) {
        if (substation.isPresent()) {
            return substation.get().getCountry();
        } else {
            return Optional.empty();
        }
    }

    protected boolean incompatibleLocationsBetweenCnecAndContingency(Set<String> cnecElementsIds, Contingency contingency) {
        if (contingency == null) {
            return false;
        }
        Set<Optional<Country>> contingencyLocations = getContingencyLocations(contingency);
        Set<Optional<Country>> cnecLocations = new HashSet<>();
        cnecElementsIds.forEach(ne -> cnecLocations.addAll(getNetworkElementLocation(ne)));
        // Intersect locations sets
        cnecLocations.retainAll(contingencyLocations);
        return cnecLocations.isEmpty();
    }

    protected boolean incompatibleLocationsBetweenCnecAndContingency(String cnecElementId, Contingency contingency) {
        return incompatibleLocationsBetweenCnecAndContingency(Set.of(cnecElementId), contingency);
    }
}
