package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.TsoEICode;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
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

            if (canImportRemedialAction(parentRemedialActionPropertyBag)) {
                Optional<String> nativeRaNameOpt = Optional.ofNullable(parentRemedialActionPropertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME));
                Optional<String> tsoNameOpt = Optional.ofNullable(parentRemedialActionPropertyBag.get(CsaProfileConstants.TSO));
                Optional<String> targetRemedialActionNameOpt = createRemedialActionName(nativeRaNameOpt.orElse(null), tsoNameOpt.orElse(null));
                Optional<Integer> speedOpt = getSpeedOpt(parentRemedialActionPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT));

                NetworkActionAdder networkActionAdder = crac.newNetworkAction();
                networkActionAdder.withId(remedialActionId);
                targetRemedialActionNameOpt.ifPresent(networkActionAdder::withName);
                tsoNameOpt.ifPresent(tso -> networkActionAdder.withOperator(tso.substring(33)));
                speedOpt.ifPresent(networkActionAdder::withSpeed);

                List<String> elementaryActions = new ArrayList<>();

                try {
                    for (PropertyBag topologyActionPropertyBag : linkedTopologyActions.get(remedialActionId)) {
                        addElementaryActions(networkActionAdder, topologyActionPropertyBag, remedialActionId, elementaryActions);
                    }

                } catch (FaraoException e) {
                    csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage()));
                    continue;
                }

                if (linkedContingencyWithRAs.containsKey(remedialActionId)) {
                    try {
                        String randomCombinationConstraintKind = linkedContingencyWithRAs.get(remedialActionId).iterator().next().get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND);
                        checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(remedialActionId, linkedContingencyWithRAs.get(remedialActionId), randomCombinationConstraintKind);

                        List<String> faraoContingenciesIds = new ArrayList<>();
                        for (PropertyBag contingencyWithRemedialActionPropertyBag : linkedContingencyWithRAs.get(remedialActionId)) {
                            checkContingency(faraoContingenciesIds, contingencyWithRemedialActionPropertyBag, parentRemedialActionPropertyBag, remedialActionId, randomCombinationConstraintKind);
                        }

                        if (!faraoContingenciesIds.isEmpty()) {
                            fillContingencies(networkActionAdder, faraoContingenciesIds, randomCombinationConstraintKind);
                        } else {
                            //  fixme:  if ra was already imported with on state, --> not sure what to do here
                            csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "None of the remedial actions with contingency linked to the grid state alteration with id: " + remedialActionId + " matches a contingency that has has been imported"));
                        }

                    } catch (FaraoException e) {
                        csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage()));
                        continue;
                    }

                } else { // no linkedContingencyWithRAs --> on instant case
                    if (!elementaryActions.isEmpty()) {
                        String kind = parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);
                        if (kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
                            networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add();
                        } else {
                            networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.CURATIVE).add();
                        }
                    } else {
                        csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "None of the topology actions linked to the grid state alteration with id: " + remedialActionId + " has a Switch that matches a switch in the network model"));
                    }
                }

                networkActionAdder.add();
                csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), "", false));

            }
        }
        this.cracCreationContext.setRemedialActionCreationContext(csaProfileRemedialActionCreationContexts);
    }

    private void fillContingencies(NetworkActionAdder networkActionAdder, List<String> faraoContingenciesIds, String combinationConstraintKind) {
        AtomicBoolean atLeastOneContingencyAdded = new AtomicBoolean(false);
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = networkActionAdder.newOnContingencyStateUsageRule().withInstant(Instant.CURATIVE);
        faraoContingenciesIds.forEach(contingencyId -> {
            onContingencyStateAdder.withContingency(contingencyId);
            atLeastOneContingencyAdded.set(true);
        });

        if (atLeastOneContingencyAdded.get() && combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString())) {
            onContingencyStateAdder.withUsageMethod(UsageMethod.FORCED).add();

        }
        if (atLeastOneContingencyAdded.get() && combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
            onContingencyStateAdder.withUsageMethod(UsageMethod.AVAILABLE).add();
        }
        if (atLeastOneContingencyAdded.get() && combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
            onContingencyStateAdder.withUsageMethod(UsageMethod.UNAVAILABLE).add();
            networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.CURATIVE).add();
        }
    }

    private void checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(String
                                                                                  remedialActionId, Set<PropertyBag> linkedContingencyWithRAs, String firstKind) {
        for (PropertyBag propertyBag : linkedContingencyWithRAs) {
            if (!propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND).equals(firstKind)) {
                throw new FaraoException("Remedial Action: " + remedialActionId + " will not be imported because ElementCombinationConstraintKind of a remedial action linked to a contingency must be all of the same kind");
            }
        }
    }

    private boolean canImportRemedialAction(PropertyBag remedialActionPropertyBag) {
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

    private void checkContingency(List<String> faraoContingenciesIds, PropertyBag contingencyWithRemedialActionPropertyBag, PropertyBag parentRemedialActionPropertyBag, String remedialActionId, String combinationConstraintKind) {
        if (!parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND).equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
            throw new FaraoException("Remedial action" + remedialActionId + " will not be imported because it is linked to a contingency but it's kind is not curative");
        }

        if (!combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
            throw new FaraoException("Remedial action" + remedialActionId + " will not be imported because combinationConstraintKind of a ContingencyWithRemedialAction must be 'included, 'excluded' or 'considered', but it was: " + combinationConstraintKind);
        }

        String contingencyId = contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).substring(19);
        Optional<CsaProfileContingencyCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(contingencyId)).findAny();
        if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
            throw new FaraoException("Remedial action" + remedialActionId + " will not be imported because contingency" + contingencyId + "linked to that remedialAction does not exist or was not imported by farao");
        } else {
            String faraoContingencyId = importedCsaProfileContingencyCreationContextOpt.get().getContigencyId();
            Optional<String> normalEnabledOpt = Optional.ofNullable(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.CONTINGENCY_WITH_REMEDIAL_ACTION_NORMAL_ENABLED));
            if (normalEnabledOpt.isPresent() && !Boolean.parseBoolean(normalEnabledOpt.get())) {
                throw new FaraoException("Remedial action" + remedialActionId + " will not be imported because ContingencyWithRemedialAction normalEnabled must be true or empty");
            }
            faraoContingenciesIds.add(faraoContingencyId);
        }
    }

    private void addElementaryActions(NetworkActionAdder networkActionAdder, PropertyBag
            topologyActionPropertyBag, String remedialActionId, List<String> elementaryActions) {
        String switchId = topologyActionPropertyBag.getId(CsaProfileConstants.SWITCH);
        if (network.getSwitch(switchId) == null) {
            throw new FaraoException("Remedial Action: " + remedialActionId + " will not be imported because network model does not contain a switch with id: " + switchId);
        }
        String propertyReference = topologyActionPropertyBag.getId(CsaProfileConstants.GRID_ALTERATION_PROPERTY_REFERENCE);
        if (!propertyReference.equals(CsaProfileConstants.PROPERTY_REFERENCE_SWITCH_OPEN)) {
            // todo this is a temporary behaviour closing switch will be implemented in a later version
            throw new FaraoException("Remedial Action: " + remedialActionId + " will not be imported because only Switch.open propertyReference is supported in the current version");
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
