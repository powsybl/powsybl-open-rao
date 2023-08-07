package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnContingencyStateAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency.CsaProfileContingencyCreationContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;

public class CsaProfileRemedialActionsCreator {
    private final Crac crac;
    private final Network network;
    private final PropertyBags gridStateAlterationRemedialActionPropertyBags;

    private final PropertyBags topologyActionsPropertyBags;

    private final PropertyBags contingencyWithRemedialActionsPropertyBags;

    private final CsaProfileCracCreationContext cracCreationContext;
    Set<CsaProfileRemedialActionCreationContext> csaProfileRemedialActionCreationContexts = new HashSet<>();

    public CsaProfileRemedialActionsCreator(Crac crac, Network network, CsaProfileCracCreationContext cracCreationContext, PropertyBags gridStateAlterationRemedialActionPropertyBags, PropertyBags topologyActionsPropertyBags, PropertyBags contingencyWithRemedialActionsPropertyBags) {
        this.crac = crac;
        this.network = network;
        this.gridStateAlterationRemedialActionPropertyBags = gridStateAlterationRemedialActionPropertyBags;
        this.topologyActionsPropertyBags = topologyActionsPropertyBags;
        this.contingencyWithRemedialActionsPropertyBags = contingencyWithRemedialActionsPropertyBags;
        this.cracCreationContext = cracCreationContext;
        createAndAddRemedialActions();
    }

    private void createAndAddRemedialActions() {

        Map<String, Set<PropertyBag>> linkedTopologyActions = CsaProfileCracUtils.getMappedPropertyBags(topologyActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedContingencyWithRAs = CsaProfileCracUtils.getMappedPropertyBags(contingencyWithRemedialActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);

        for (PropertyBag parentRemedialActionPropertyBag : gridStateAlterationRemedialActionPropertyBags) {
            String remedialActionId = parentRemedialActionPropertyBag.get("mRID");

            if (!linkedTopologyActions.containsKey(remedialActionId)) {
                continue;
            }

            if (importRemedialAction(parentRemedialActionPropertyBag)) {
                Optional<String> nativeRaNameOpt = Optional.ofNullable(parentRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_REMEDIAL_ACTION_NAME));
                Optional<String> tsoNameOpt = Optional.ofNullable(parentRemedialActionPropertyBag.get(CsaProfileConstants.TSO));
                Optional<String> targetRemedialActionNameOpt = createRemedialActionName(nativeRaNameOpt.orElse(null), tsoNameOpt.orElse(null));
                Optional<Integer> speedOpt = getSpeedOpt(parentRemedialActionPropertyBag.get("timeToImplement"));

                NetworkActionAdder networkActionAdder = crac.newNetworkAction();
                networkActionAdder.withId(remedialActionId);
                targetRemedialActionNameOpt.ifPresent(networkActionAdder::withName);
                tsoNameOpt.ifPresent(networkActionAdder::withOperator);
                speedOpt.ifPresent(networkActionAdder::withSpeed);

                if (linkedContingencyWithRAs.containsKey(remedialActionId)) {
                    String combinationConstraintKind = linkedContingencyWithRAs.get(remedialActionId).iterator().next().get("combinationConstraintKind"); // any kind
                    if (!checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(remedialActionId, linkedContingencyWithRAs.get(remedialActionId), combinationConstraintKind)) {
                        return;
                    }

                    List<String> faraoContingenciesIds = new ArrayList<>();
                    for (PropertyBag contingencyWithRemedialActionPropertyBag : linkedContingencyWithRAs.get(remedialActionId)) {
                        importOnStateRa(faraoContingenciesIds, contingencyWithRemedialActionPropertyBag, parentRemedialActionPropertyBag, remedialActionId, combinationConstraintKind);
                    }

                    if (!faraoContingenciesIds.isEmpty()) {
                        if (combinationConstraintKind.equals("http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included")) {
                            OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = fillContingencies(networkActionAdder, faraoContingenciesIds, linkedTopologyActions.get(remedialActionId));
                            onContingencyStateAdder.withUsageMethod(UsageMethod.FORCED).add().add();
                        }
                        if (combinationConstraintKind.equals("http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered")) {
                            OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = fillContingencies(networkActionAdder, faraoContingenciesIds, linkedTopologyActions.get(remedialActionId));
                            onContingencyStateAdder.withUsageMethod(UsageMethod.AVAILABLE).add().add();
                        }
                    } else {
                        // todo log issue
                        // todo not imported
                    }

                    if (combinationConstraintKind.equals("http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.excluded")) {
                        networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.UNAVAILABLE).withInstant(Instant.CURATIVE);
                        // todo not sure: ticket says: If ElementCombinationConstraintKind.excluded  then import the remedial action as an on-instant RA and add an UNAVAILABLE usage method for this specific contingency (UsageMethod.UNAVAILABLE )
                    }

                } else {
                    List<String> elementaryActions = new ArrayList<>();
                    for (PropertyBag topologyActionPropertyBag : linkedTopologyActions.get(remedialActionId)) {
                        importOnInstant(networkActionAdder, topologyActionPropertyBag, remedialActionId, elementaryActions);
                    }

                    if (!elementaryActions.isEmpty()) {
                        String kind = parentRemedialActionPropertyBag.get("kind");
                        if (kind.equals(CsaProfileConstants.KIND_PREVENTIVE)) {
                            networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add().add();
                        } else {
                            networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.CURATIVE).add().add();
                        }
                    } else {
                        // todo log issue
                        // todo not imported
                    }

                }
                csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), "", false));
            }
        }

        this.cracCreationContext.setRemedialActionCreationContext(csaProfileRemedialActionCreationContexts);

    }

    private OnContingencyStateAdder<NetworkActionAdder> fillContingencies(NetworkActionAdder networkActionAdder, List<String> faraoContingenciesIds, Set<PropertyBag> topologyActionPropertyBag) {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = networkActionAdder.newOnContingencyStateUsageRule().withInstant(Instant.CURATIVE);
        faraoContingenciesIds.forEach(contingencyId -> {
            String switchId = topologyActionPropertyBag.iterator().next().getId(CsaProfileConstants.SWITCH);
            if (network.getSwitch(switchId) == null) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(topologyActionPropertyBag.iterator().next().getId(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION), ImportStatus.INCONSISTENCY_IN_DATA, "No switch with id: " + switchId + " found in network"));
                return;
            }
            networkActionAdder.newTopologicalAction().withActionType(ActionType.OPEN)
                    .withNetworkElement(switchId).add();
            onContingencyStateAdder.withContingency(contingencyId);
        });
        return onContingencyStateAdder;
    }

    private boolean checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(String raId, Set<PropertyBag> linkedContingencyWithRAs, String firstKind) {
        for (PropertyBag propertyBag : linkedContingencyWithRAs) {
            if (!propertyBag.get("combinationConstraintKind").equals(firstKind)) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(raId, ImportStatus.INCONSISTENCY_IN_DATA, "ElementCombinationConstraintKind of a remedial action linked to a contingency must be the same kind"));
                return false;
            }
        }
        return true;
    }

    private boolean importRemedialAction(PropertyBag remedialActionPropertyBag) {
        String keyword = remedialActionPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_KEYWORD);
        String startTime = remedialActionPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = remedialActionPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_END_DATE);
        String remedialActionId = remedialActionPropertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        String kind = remedialActionPropertyBag.get(CsaProfileConstants.REQUEST_KIND);

        boolean normalAvailable = Boolean.parseBoolean(remedialActionPropertyBag.get(CsaProfileConstants.REQUEST_RA_NORMAL_AVAILABLE));

        if (!keyword.equals(CsaProfileConstants.REMEDIAL_ACTION_FILE_KEYWORD)) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Model.keyword must be RA, but it is " + keyword));
            return false;
        }

        if (!CsaProfileCracUtils.isValidInterval(cracCreationContext.getTimeStamp(), startTime, endTime)) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "Required timestamp does not fall between Model.startDate and Model.endDate"));
            return false;
        }

        if (!normalAvailable) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "RemedialAction.normalAvailable must be 'true' to be imported"));
            return false;
        }
        if (!kind.equals(CsaProfileConstants.KIND_CURATIVE) && !kind.equals(CsaProfileConstants.KIND_PREVENTIVE)) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Unsupported kind for remedial action" + remedialActionId));
            return false;
        }

        return true;
    }

    private void importOnStateRa(List<String> faraoContingenciesIds, PropertyBag contingencyWithRemedialActionPropertyBag, PropertyBag parentRemedialActionPropertyBag, String remedialActionId, String combinationConstraintKind) {
        // check that parent ra is curative
        if (!parentRemedialActionPropertyBag.get("kind").equals(CsaProfileConstants.KIND_CURATIVE)) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action" + remedialActionId + "is linked to a contingency but it's kind is not curative"));
        }

        // check combinationConstraintKind is handled
        if (!combinationConstraintKind.equals("http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included") && !combinationConstraintKind.equals("http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.excluded") && !combinationConstraintKind.equals("http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered")) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "combinationConstraintKind of a ContingencyWithRemedialAction must be 'included, 'excluded' or 'considered', but it was: " + combinationConstraintKind));
            return;
        }

        String contingencyId = contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).substring(19);
        Optional<CsaProfileContingencyCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(contingencyId)).findAny();
        if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Contingency" + contingencyId + "linked to RemedialAction" + remedialActionId + " does not exist or was not imported by farao"));
            return;
        } else {
            String faraoContingencyId = importedCsaProfileContingencyCreationContextOpt.get().getContigencyId();
            Optional<String> normalEnabledOpt = Optional.ofNullable(contingencyWithRemedialActionPropertyBag.get("normalEnabled"));
            if (normalEnabledOpt.isPresent() && normalEnabledOpt.get().equals("false")) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "normalEnabled must be true or empty"));
            } else {
                // todo check with po's if this check is necessary
                Set<NetworkElement> networkElements = crac.getContingency(contingencyId).getNetworkElements();
                if (networkElements.isEmpty()) {
                    csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action" + remedialActionId + "has to have at least one ElementaryAction"));
                    return;
                }
            }
            faraoContingenciesIds.add(faraoContingencyId);
        }
    }

    private void importOnInstant(NetworkActionAdder networkActionAdder, PropertyBag topologyActionPropertyBag, String remedialActionId, List<String> elementaryActions) {
        String switchId = topologyActionPropertyBag.getId(CsaProfileConstants.SWITCH);
        if (network.getSwitch(switchId) == null) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "No switch with id: " + switchId + " found in network"));
            return;
        }
        String propertyReference = topologyActionPropertyBag.getId(CsaProfileConstants.GRID_ALTERATION_PROPERTY_REFERENCE);
        if (!propertyReference.equals("http://energy.referencedata.eu/PropertyReference/Switch.open")) {
            // todo this is a temporary behaviour "Switch.close" will be implemented in a later version
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Only Switch.open propertyReference is supported"));
            return;
        }

        networkActionAdder.newTopologicalAction()
                .withNetworkElement(switchId)
                .withActionType(ActionType.OPEN).add();
        elementaryActions.add(switchId);
    }

    private Optional<Integer> getSpeedOpt(String timeToImplement) {
        if (timeToImplement != null) {
            return Optional.of(CsaProfileCracUtils.convertDurationToSeconds(timeToImplement));
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> createRemedialActionName(String nativeRemedialActionName, String tsoName) {
        if (nativeRemedialActionName != null) {
            if (tsoName != null) {
                // TODO add conversion tso eic to tso name
                return Optional.of(tsoName + "_" + nativeRemedialActionName);
            }
            return Optional.of(nativeRemedialActionName);
        } else {
            return Optional.empty();
        }
    }

}
