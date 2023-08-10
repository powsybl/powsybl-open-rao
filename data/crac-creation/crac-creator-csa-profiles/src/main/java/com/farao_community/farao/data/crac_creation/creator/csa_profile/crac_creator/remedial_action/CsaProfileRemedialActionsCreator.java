/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnContingencyStateAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.FaraoImportException;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency.CsaProfileContingencyCreationContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
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

            try {
                if (!linkedTopologyActions.containsKey(remedialActionId)) {
                    csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because there is no topology actions linked to that RA"));
                    continue;
                }

                checkRemedialActionCanBeImported(parentRemedialActionPropertyBag);

                Optional<String> nativeRaNameOpt = Optional.ofNullable(parentRemedialActionPropertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME));
                Optional<String> tsoNameOpt = Optional.ofNullable(parentRemedialActionPropertyBag.get(CsaProfileConstants.TSO));
                Optional<String> targetRemedialActionNameOpt = CsaProfileCracUtils.createRemedialActionName(nativeRaNameOpt.orElse(null), tsoNameOpt.orElse(null));
                Optional<Integer> speedOpt = getSpeedOpt(parentRemedialActionPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT));

                NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(remedialActionId);
                targetRemedialActionNameOpt.ifPresent(networkActionAdder::withName);
                tsoNameOpt.ifPresent(tso -> networkActionAdder.withOperator(tso.substring(tso.lastIndexOf("/") + 1)));
                speedOpt.ifPresent(networkActionAdder::withSpeed);

                List<String> elementaryActions = new ArrayList<>();
                for (PropertyBag topologyActionPropertyBag : linkedTopologyActions.get(remedialActionId)) {
                    addElementaryActions(networkActionAdder, topologyActionPropertyBag, remedialActionId, elementaryActions);
                }

                if (elementaryActions.isEmpty()) {
                    csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because none of the topology actions linked to that RA has a Switch that matches a switch in the network model"));
                    continue;
                }

                if (linkedContingencyWithRAs.containsKey(remedialActionId)) {

                    String randomCombinationConstraintKind = linkedContingencyWithRAs.get(remedialActionId).iterator().next().get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND);
                    checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(remedialActionId, linkedContingencyWithRAs.get(remedialActionId), randomCombinationConstraintKind);

                    List<String> faraoContingenciesIds = new ArrayList<>();
                    for (PropertyBag contingencyWithRemedialActionPropertyBag : linkedContingencyWithRAs.get(remedialActionId)) {
                        checkContingencyAndFillImportedCoIds(faraoContingenciesIds, contingencyWithRemedialActionPropertyBag, parentRemedialActionPropertyBag, remedialActionId, randomCombinationConstraintKind);
                    }

                    addOnContingencyStateUsageRules(networkActionAdder, faraoContingenciesIds, randomCombinationConstraintKind);
                } else { // no contingency linked to RA --> on instant case
                    String kind = parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);
                    if (kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
                        networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add();
                    } else {
                        networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.CURATIVE).add();
                    }
                }

                networkActionAdder.add();
                csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), "", false));

            } catch (FaraoImportException e) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileRemedialActionCreationContext.notImported(remedialActionId, e.getImportStatus(), e.getMessage()));
            }
        }
        this.cracCreationContext.setRemedialActionCreationContext(csaProfileRemedialActionCreationContexts);
    }

    private void addOnContingencyStateUsageRules(NetworkActionAdder networkActionAdder, List<String> faraoContingenciesIds, String randomCombinationConstraintKind) {
        faraoContingenciesIds.forEach(faraoContingenciesId -> {
            OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = networkActionAdder.newOnContingencyStateUsageRule().withInstant(Instant.CURATIVE).withContingency(faraoContingenciesId);

            if (randomCombinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString())) {
                onContingencyStateAdder.withUsageMethod(UsageMethod.FORCED).add();

            }
            if (randomCombinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
                onContingencyStateAdder.withUsageMethod(UsageMethod.AVAILABLE).add();
            }
            if (randomCombinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
                onContingencyStateAdder.withUsageMethod(UsageMethod.UNAVAILABLE).add();
                networkActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.CURATIVE).add();
            }
        });

    }

    private void checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(String
                                                                                  remedialActionId, Set<PropertyBag> linkedContingencyWithRAs, String firstKind) {
        for (PropertyBag propertyBag : linkedContingencyWithRAs) {
            if (!propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND).equals(firstKind)) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because ElementCombinationConstraintKind of a remedial action linked to a contingency must be all of the same kind");
            }
        }
    }

    private void checkRemedialActionCanBeImported(PropertyBag remedialActionPropertyBag) {
        String keyword = remedialActionPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_KEYWORD);
        String startTime = remedialActionPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = remedialActionPropertyBag.get(CsaProfileConstants.REQUEST_HEADER_END_DATE);
        String remedialActionId = remedialActionPropertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        String kind = remedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);

        boolean normalAvailable = Boolean.parseBoolean(remedialActionPropertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE));

        if (!keyword.equals(CsaProfileConstants.REMEDIAL_ACTION_FILE_KEYWORD)) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because Model.keyword must be RA, but it is " + keyword);
        }
        if (!CsaProfileCracUtils.isValidInterval(cracCreationContext.getTimeStamp(), startTime, endTime)) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because required timestamp does not fall between Model.startDate and Model.endDate");

        }
        if (!normalAvailable) {
            throw new FaraoImportException(ImportStatus.NOT_FOR_RAO, "Remedial Action: " + remedialActionId + " will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");
        }
        if (!kind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString()) && !kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because Unsupported kind for remedial action" + remedialActionId);
        }
    }

    private void checkContingencyAndFillImportedCoIds(List<String> faraoContingenciesIds, PropertyBag
            contingencyWithRemedialActionPropertyBag, PropertyBag parentRemedialActionPropertyBag, String
                                                              remedialActionId, String combinationConstraintKind) {
        if (!parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND).equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action" + remedialActionId + " will not be imported because it is linked to a contingency but it's kind is not curative");
        }

        if (!combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action" + remedialActionId + " will not be imported because combinationConstraintKind of a ContingencyWithRemedialAction must be 'included, 'excluded' or 'considered', but it was: " + combinationConstraintKind);
        }

        String contingencyId = contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).substring(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).lastIndexOf("_") + 1);
        Optional<CsaProfileContingencyCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(contingencyId)).findAny();
        if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action" + remedialActionId + " will not be imported because contingency" + contingencyId + "linked to that remedialAction does not exist or was not imported by farao");
        } else {
            String faraoContingencyId = importedCsaProfileContingencyCreationContextOpt.get().getContigencyId();
            Optional<String> normalEnabledOpt = Optional.ofNullable(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.CONTINGENCY_WITH_REMEDIAL_ACTION_NORMAL_ENABLED));
            if (normalEnabledOpt.isPresent() && !Boolean.parseBoolean(normalEnabledOpt.get())) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action" + remedialActionId + " will not be imported because ContingencyWithRemedialAction normalEnabled must be true or empty");
            }
            faraoContingenciesIds.add(faraoContingencyId);
        }
    }

    private void addElementaryActions(NetworkActionAdder networkActionAdder, PropertyBag
            topologyActionPropertyBag, String remedialActionId, List<String> elementaryActions) {
        String switchId = topologyActionPropertyBag.getId(CsaProfileConstants.SWITCH);
        if (network.getSwitch(switchId) == null) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because network model does not contain a switch with id: " + switchId);
        }
        String propertyReference = topologyActionPropertyBag.getId(CsaProfileConstants.GRID_ALTERATION_PROPERTY_REFERENCE);
        if (!propertyReference.equals(CsaProfileConstants.PROPERTY_REFERENCE_SWITCH_OPEN)) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial Action: " + remedialActionId + " will not be imported because only Switch.open propertyReference is supported in the current version");
        }
        networkActionAdder.newTopologicalAction()
                .withNetworkElement(switchId)
                // todo this is a temporary behaviour closing switch will be implemented in a later version
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

}
