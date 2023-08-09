package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.commons.TsoEICode;
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
import java.util.concurrent.atomic.AtomicBoolean;

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

        Map<String, Set<PropertyBag>> linkedTopologyActions = CsaProfileCracUtils.getMappedPropertyBagsSet(topologyActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedContingencyWithRAs = CsaProfileCracUtils.getMappedPropertyBagsSet(contingencyWithRemedialActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);

        for (PropertyBag parentRemedialActionPropertyBag : gridStateAlterationRemedialActionPropertyBags) {
            String remedialActionId = parentRemedialActionPropertyBag.get(CsaProfileConstants.MRID);

            if (!linkedTopologyActions.containsKey(remedialActionId)) {
                continue;
            }

            if (importRemedialAction(parentRemedialActionPropertyBag)) {
                Optional<String> nativeRaNameOpt = Optional.ofNullable(parentRemedialActionPropertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME));
                Optional<String> tsoNameOpt = Optional.ofNullable(parentRemedialActionPropertyBag.get(CsaProfileConstants.TSO));
                Optional<String> targetRemedialActionNameOpt = createRemedialActionName(nativeRaNameOpt.orElse(null), tsoNameOpt.orElse(null));
                Optional<Integer> speedOpt = getSpeedOpt(parentRemedialActionPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT));

                NetworkActionAdder networkActionAdder = crac.newNetworkAction();
                networkActionAdder.withId(remedialActionId);
                targetRemedialActionNameOpt.ifPresent(networkActionAdder::withName);
                tsoNameOpt.ifPresent(tso -> networkActionAdder.withOperator(tso.substring(33)));
                speedOpt.ifPresent(networkActionAdder::withSpeed);

                if (linkedContingencyWithRAs.containsKey(remedialActionId)) {
                    String combinationConstraintKind = linkedContingencyWithRAs.get(remedialActionId).iterator().next().get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND); // any kind
                    if (!checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(remedialActionId, linkedContingencyWithRAs.get(remedialActionId), combinationConstraintKind)) {
                        return;
                    }

                    List<String> faraoContingenciesIds = new ArrayList<>();
                    for (PropertyBag contingencyWithRemedialActionPropertyBag : linkedContingencyWithRAs.get(remedialActionId)) {
                        importOnStateRa(faraoContingenciesIds, contingencyWithRemedialActionPropertyBag, parentRemedialActionPropertyBag, remedialActionId, combinationConstraintKind);
                    }

                    if (!faraoContingenciesIds.isEmpty()) {
                        fillContingencies(networkActionAdder, faraoContingenciesIds, linkedTopologyActions.get(remedialActionId), combinationConstraintKind);
                    } else {
                        csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "None of the remedial actions with contingency linked to the grid state alteration with id: " + remedialActionId + " matches a contingency that has has imported"));
                    }

                } else {
                    List<String> elementaryActions = new ArrayList<>();
                    for (PropertyBag topologyActionPropertyBag : linkedTopologyActions.get(remedialActionId)) {
                        importOnInstant(networkActionAdder, topologyActionPropertyBag, remedialActionId, elementaryActions);
                    }

                    if (!elementaryActions.isEmpty()) {
                        String kind = parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);
                        if (kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
                            networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add().add();
                        } else {
                            networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.CURATIVE).add().add();
                        }
                    } else {
                        csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "None of the topology actions linked to the grid state alteration with id: " + remedialActionId + " has a Switch that matches a switch in the network model"));
                    }

                }
                csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), "", false));
            }
        }

        this.cracCreationContext.setRemedialActionCreationContext(csaProfileRemedialActionCreationContexts);

    }

    private void fillContingencies(NetworkActionAdder networkActionAdder, List<String> faraoContingenciesIds, Set<PropertyBag> topologyActionPropertyBag, String combinationConstraintKind) {
        AtomicBoolean atLeastOneContingencyAdded = new AtomicBoolean(false);
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
            atLeastOneContingencyAdded.set(true);
        });

        if (atLeastOneContingencyAdded.get() && combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString())) {
            onContingencyStateAdder.withUsageMethod(UsageMethod.FORCED).add().add();

        }
        if (atLeastOneContingencyAdded.get() && combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
            onContingencyStateAdder.withUsageMethod(UsageMethod.AVAILABLE).add().add();
        }

        if (atLeastOneContingencyAdded.get() && combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
            onContingencyStateAdder.withUsageMethod(UsageMethod.UNAVAILABLE).add();
            networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.CURATIVE).add().add();
        }
    }

    private boolean checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(String raId, Set<PropertyBag> linkedContingencyWithRAs, String firstKind) {
        for (PropertyBag propertyBag : linkedContingencyWithRAs) {
            if (!propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND).equals(firstKind)) {
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
        String kind = remedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);

        boolean normalAvailable = Boolean.parseBoolean(remedialActionPropertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE));

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
        if (!kind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString()) && !kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Unsupported kind for remedial action" + remedialActionId));
            return false;
        }

        return true;
    }

    private void importOnStateRa(List<String> faraoContingenciesIds, PropertyBag contingencyWithRemedialActionPropertyBag, PropertyBag parentRemedialActionPropertyBag, String remedialActionId, String combinationConstraintKind) {
        // check that parent ra is curative
        if (!parentRemedialActionPropertyBag.get("kind").equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action" + remedialActionId + "is linked to a contingency but it's kind is not curative"));
        }

        // check combinationConstraintKind is handled
        if (!combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
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
            Optional<String> normalEnabledOpt = Optional.ofNullable(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.CONTINGENCY_WITH_REMEDIAL_ACTION_NORMAL_ENABLED));
            if (normalEnabledOpt.isPresent() && !Boolean.parseBoolean(normalEnabledOpt.get())) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "normalEnabled must be true or empty"));
            } else {
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
        if (!propertyReference.equals(CsaProfileConstants.PROPERTY_REFERENCE_SWITCH_OPEN)) {
            // todo this is a temporary behaviour closing switch will be implemented in a later version
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
                return Optional.of(TsoEICode.fromEICode(tsoName.substring(33)).getDisplayName() + "_" + nativeRemedialActionName);
            }
            return Optional.of(nativeRemedialActionName);
        } else {
            return Optional.empty();
        }
    }

}
