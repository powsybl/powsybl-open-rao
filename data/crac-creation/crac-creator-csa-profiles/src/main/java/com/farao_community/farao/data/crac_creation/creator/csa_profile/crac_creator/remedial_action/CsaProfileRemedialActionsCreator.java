/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.TsoEICode;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;
import java.util.function.Function;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaProfileRemedialActionsCreator {
    final Set<CsaProfileElementaryCreationContext> csaProfileRemedialActionCreationContexts = new HashSet<>();
    private final Crac crac;
    private final Network network;
    private final PropertyBags gridStateAlterationRemedialActionPropertyBags;
    private final PropertyBags topologyActionsPropertyBags;
    private final PropertyBags rotatingMachineActionsPropertyBags;
    private final PropertyBags tapPositionPropertyBags;
    private final PropertyBags staticPropertyRangesPropertyBags;
    private final PropertyBags contingencyWithRemedialActionsPropertyBags;
    private final OnConstraintUsageRuleHelper onConstraintUsageRuleHelper;
    private final CsaProfileCracCreationContext cracCreationContext;

    public CsaProfileRemedialActionsCreator(Crac crac, Network network, CsaProfileCracCreationContext cracCreationContext, PropertyBags gridStateAlterationRemedialActionPropertyBags, PropertyBags contingencyWithRemedialActionsPropertyBags,
                                            PropertyBags topologyActionsPropertyBags,
                                            PropertyBags rotatingMachineActionsPropertyBags,
                                            PropertyBags tapPositionPropertyBags,
                                            PropertyBags staticPropertyRangesPropertyBags,
                                            OnConstraintUsageRuleHelper onConstraintUsageRuleHelper) {
        this.crac = crac;
        this.network = network;
        this.gridStateAlterationRemedialActionPropertyBags = gridStateAlterationRemedialActionPropertyBags;
        this.contingencyWithRemedialActionsPropertyBags = contingencyWithRemedialActionsPropertyBags;
        this.topologyActionsPropertyBags = topologyActionsPropertyBags;
        this.rotatingMachineActionsPropertyBags = rotatingMachineActionsPropertyBags;
        this.tapPositionPropertyBags = tapPositionPropertyBags;
        this.staticPropertyRangesPropertyBags = staticPropertyRangesPropertyBags;
        this.cracCreationContext = cracCreationContext;
        this.onConstraintUsageRuleHelper = onConstraintUsageRuleHelper;
        createAndAddRemedialActions();
    }

    private static boolean isOnConstraintInstantCoherent(InstantKind cnecInstantKind, Instant remedialInstant) {
        return switch (remedialInstant.getInstantKind()) {
            case PREVENTIVE ->
                cnecInstantKind == InstantKind.PREVENTIVE || cnecInstantKind == InstantKind.OUTAGE || cnecInstantKind == InstantKind.CURATIVE;
            case AUTO -> cnecInstantKind == InstantKind.AUTO;
            case CURATIVE -> cnecInstantKind == InstantKind.CURATIVE;
            default -> false;
        };
    }

    private void createAndAddRemedialActions() {
        Map<String, Set<PropertyBag>> linkedContingencyWithRAs = CsaProfileCracUtils.getMappedPropertyBagsSet(contingencyWithRemedialActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedTopologyActions = CsaProfileCracUtils.getMappedPropertyBagsSet(topologyActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedRotatingMachineActions = CsaProfileCracUtils.getMappedPropertyBagsSet(rotatingMachineActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedTapPositionActions = CsaProfileCracUtils.getMappedPropertyBagsSet(tapPositionPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedStaticPropertyRanges = CsaProfileCracUtils.getMappedPropertyBagsSet(staticPropertyRangesPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)

        NetworkActionCreator networkActionCreator = new NetworkActionCreator(crac, network);
        PstRangeActionCreator pstRangeActionCreator = new PstRangeActionCreator(crac, network);

        for (PropertyBag parentRemedialActionPropertyBag : gridStateAlterationRemedialActionPropertyBags) {
            String remedialActionId = parentRemedialActionPropertyBag.get(CsaProfileConstants.MRID);

            try {
                RemedialActionType remedialActionType = checkRemedialActionCanBeImportedAndIdentifyType(parentRemedialActionPropertyBag, linkedTopologyActions, linkedRotatingMachineActions, linkedTapPositionActions, linkedStaticPropertyRanges);
                RemedialActionAdder<?> remedialActionAdder;
                String nativeRaName = parentRemedialActionPropertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME);
                String tsoName = parentRemedialActionPropertyBag.get(CsaProfileConstants.TSO);
                Optional<String> targetRemedialActionNameOpt = CsaProfileCracUtils.createRemedialActionName(nativeRaName, tsoName);
                Optional<Integer> speedOpt = getSpeedOpt(parentRemedialActionPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT));

                if (remedialActionType.equals(RemedialActionType.NETWORK_ACTION)) {
                    remedialActionAdder = networkActionCreator.getNetworkActionAdder(linkedTopologyActions, linkedRotatingMachineActions, linkedStaticPropertyRanges, remedialActionId);
                } else {
                    remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(linkedTapPositionActions, linkedStaticPropertyRanges, remedialActionId);
                }

                targetRemedialActionNameOpt.ifPresent(remedialActionAdder::withName);
                if (tsoName != null) {
                    remedialActionAdder.withOperator(TsoEICode.fromEICode(tsoName.substring(tsoName.lastIndexOf("/") + 1)).getDisplayName());
                }
                speedOpt.ifPresent(remedialActionAdder::withSpeed);

                if (linkedContingencyWithRAs.containsKey(remedialActionId)) {
                    // on state usage rule
                    String randomCombinationConstraintKind = linkedContingencyWithRAs.get(remedialActionId).iterator().next().get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND);
                    checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(remedialActionId, linkedContingencyWithRAs.get(remedialActionId), randomCombinationConstraintKind);

                    List<String> faraoContingenciesIds = linkedContingencyWithRAs.get(remedialActionId).stream()
                        .map(contingencyWithRemedialActionPropertyBag ->
                            checkContingencyAndGetFaraoId(
                                contingencyWithRemedialActionPropertyBag,
                                parentRemedialActionPropertyBag,
                                remedialActionId,
                                randomCombinationConstraintKind
                            )
                        )
                        .toList();

                    Instant instantCurative = crac.getUniqueInstant(InstantKind.CURATIVE);
                    boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(instantCurative, remedialActionAdder, remedialActionId);
                    if (!hasAtLeastOneOnConstraintUsageRule) {
                        addOnContingencyStateUsageRules(remedialActionAdder, faraoContingenciesIds, instantCurative.getId(), randomCombinationConstraintKind);
                    }
                } else { // no contingency linked to RA --> on instant usage rule
                    String kind = parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);
                    if (kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
                        Instant instantPrev = crac.getUniqueInstant(InstantKind.PREVENTIVE);
                        boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(instantPrev, remedialActionAdder, remedialActionId);
                        if (!hasAtLeastOneOnConstraintUsageRule) {

                            remedialActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId(instantPrev.getId()).add();
                        }
                    } else {
                        Instant instantCurative = crac.getUniqueInstant(InstantKind.CURATIVE);
                        boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(instantCurative, remedialActionAdder, remedialActionId);
                        if (!hasAtLeastOneOnConstraintUsageRule) {

                            remedialActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId(instantCurative.getId()).add();
                        }
                    }
                }
                remedialActionAdder.add();
                csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), "", false));

            } catch (FaraoImportException e) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.notImported(remedialActionId, e.getImportStatus(), e.getMessage()));
            }
        }
        this.cracCreationContext.setRemedialActionCreationContexts(csaProfileRemedialActionCreationContexts);
    }

    private void addOnContingencyStateUsageRules(RemedialActionAdder<?> remedialActionAdder, List<String> faraoContingenciesIds, String instantCurativeId, String randomCombinationConstraintKind) {
        if (randomCombinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
            remedialActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId(instantCurativeId).add();
        }

        UsageMethod usageMethod = CsaProfileCracUtils.getConstraintToUsageMethodMap().get(randomCombinationConstraintKind);
        faraoContingenciesIds.forEach(faraoContingencyId -> remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstantId(instantCurativeId)
            .withContingency(faraoContingencyId)
            .withUsageMethod(usageMethod).add());
    }

    private void checkAllContingenciesLinkedToRaHaveTheSameConstraintKind(String remedialActionId, Set<PropertyBag> linkedContingencyWithRAs, String firstKind) {
        for (PropertyBag propertyBag : linkedContingencyWithRAs) {
            if (!propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND).equals(firstKind)) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because ElementCombinationConstraintKind of a remedial action linked to a contingency must be all of the same kind");
            }
        }
    }

    private RemedialActionType checkRemedialActionCanBeImportedAndIdentifyType(PropertyBag remedialActionPropertyBag, Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> linkedTapPositionActions, Map<String, Set<PropertyBag>> linkedStaticPropertyRanges) {
        String remedialActionId = remedialActionPropertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);

        String kind = remedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);

        boolean normalAvailable = Boolean.parseBoolean(remedialActionPropertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE));

        CsaProfileConstants.HeaderValidity headerValidity = CsaProfileCracUtils.checkProfileHeader(remedialActionPropertyBag, CsaProfileConstants.CsaProfile.REMEDIAL_ACTION, cracCreationContext.getTimeStamp());
        if (headerValidity == CsaProfileConstants.HeaderValidity.INVALID_KEYWORD) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Model.keyword must be " + CsaProfileConstants.CsaProfile.REMEDIAL_ACTION);
        } else if (headerValidity == CsaProfileConstants.HeaderValidity.INVALID_INTERVAL) {
            throw new FaraoImportException(ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "Required timestamp does not fall between Model.startDate and Model.endDate");
        }

        if (!normalAvailable) {
            throw new FaraoImportException(ImportStatus.NOT_FOR_RAO, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");
        }
        if (!kind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString()) && !kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because Unsupported kind for remedial action" + remedialActionId);
        }

        if (linkedTopologyActions.containsKey(remedialActionId)) {
            return RemedialActionType.NETWORK_ACTION;
        } else if (linkedRotatingMachineActions.containsKey(remedialActionId)) {
            checkEachRotatingMachineHasExactlyOneStaticPropertyRangeElseThrowException(remedialActionId, linkedRotatingMachineActions.get(remedialActionId), linkedStaticPropertyRanges);
            return RemedialActionType.NETWORK_ACTION;
        } else if (linkedTapPositionActions.containsKey(remedialActionId)) { // StaticPropertyRanges not mandatory in case of tapPositionsActions
            return RemedialActionType.PST_RANGE_ACTION;
        } else {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because there is no topology actions, no Set point actions, nor tap position action linked to that RA");
        }
    }

    private void checkEachRotatingMachineHasExactlyOneStaticPropertyRangeElseThrowException(String remedialActionId, Set<PropertyBag> rotatingMachineActionsForOneRa, Map<String, Set<PropertyBag>> staticPropertyRangesLinkedToRotatingMachineActions) {
        int numberOfStaticPropertyRanges = getNumberOfStaticPropertyRangeForRotatingMachineAction(rotatingMachineActionsForOneRa, staticPropertyRangesLinkedToRotatingMachineActions);
        if (numberOfStaticPropertyRanges == 0) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because there is no StaticPropertyRange linked to that RA");
        } else if (numberOfStaticPropertyRanges > 1) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because several conflictual StaticPropertyRanges are linked to that RA's RotatingMachineAction");
        }
    }

    private int getNumberOfStaticPropertyRangeForRotatingMachineAction(Set<PropertyBag> rotatingMachineActionsForOneRa, Map<String, Set<PropertyBag>> staticPropertyRangesLinkedToRotatingMachineActions) {
        int numberOfStaticPropertyRanges = 0;
        for (PropertyBag rotatingMachineAction : rotatingMachineActionsForOneRa) {
            Set<PropertyBag> staticPropertyRangePropertyBags = staticPropertyRangesLinkedToRotatingMachineActions.get(rotatingMachineAction.getId("mRID"));
            if (staticPropertyRangePropertyBags != null) {
                numberOfStaticPropertyRanges += staticPropertyRangePropertyBags.size();
            }
        }
        return numberOfStaticPropertyRanges;
    }

    private String checkContingencyAndGetFaraoId(PropertyBag contingencyWithRemedialActionPropertyBag, PropertyBag parentRemedialActionPropertyBag, String
        remedialActionId, String combinationConstraintKind) {
        if (!parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND).equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because it is linked to a contingency but it's kind is not curative");
        }

        if (!combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because combinationConstraintKind of a ContingencyWithRemedialAction must be 'included, 'excluded' or 'considered', but it was: " + combinationConstraintKind);
        }

        String contingencyId = contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).substring(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).lastIndexOf("_") + 1);
        Optional<CsaProfileElementaryCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(contingencyId)).findAny();
        if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because contingency" + contingencyId + " linked to that remedialAction does not exist or was not imported by farao");
        } else {
            String faraoContingencyId = importedCsaProfileContingencyCreationContextOpt.get().getElementId();
            CsaProfileCracUtils.checkNormalEnabled(contingencyWithRemedialActionPropertyBag, remedialActionId, "ContingencyWithRemedialAction");
            return faraoContingencyId;
        }
    }

    private Optional<Integer> getSpeedOpt(String timeToImplement) {
        if (timeToImplement != null) {
            return Optional.of(CsaProfileCracUtils.convertDurationToSeconds(timeToImplement));
        } else {
            return Optional.empty();
        }
    }

    private boolean addOnConstraintUsageRules(Instant remedialActionInstant, RemedialActionAdder remedialActionAdder, String importableRemedialActionId) {
        boolean flag1 = false;
        boolean flag2;
        boolean flag3;
        if (!onConstraintUsageRuleHelper.getExcludedCnecsByRemedialAction().containsKey(importableRemedialActionId)) {
            flag1 = processAvailableAssessedElementsCombinableWithRemedialActions(remedialActionInstant, remedialActionAdder, UsageMethod.AVAILABLE);
        }
        flag2 = processAssessedElementsWithRemedialActions(remedialActionInstant, remedialActionAdder, importableRemedialActionId, UsageMethod.AVAILABLE, onConstraintUsageRuleHelper.getConsideredCnecsElementsByRemedialAction());
        flag3 = processAssessedElementsWithRemedialActions(remedialActionInstant, remedialActionAdder, importableRemedialActionId, UsageMethod.FORCED, onConstraintUsageRuleHelper.getIncludedCnecsByRemedialAction());
        return flag1 || flag2 || flag3;
    }

    private boolean processAvailableAssessedElementsCombinableWithRemedialActions(Instant remedialActionInstant, RemedialActionAdder remedialActionAdder, UsageMethod usageMethod) {
        List<Boolean> flags = onConstraintUsageRuleHelper.getImportedCnecsCombinableWithRas().stream().map(addOnConstraintUsageRuleForCnec(remedialActionInstant, remedialActionAdder, usageMethod)).toList();
        return flags.contains(true);
    }

    private boolean processAssessedElementsWithRemedialActions(Instant remedialActionInstant, RemedialActionAdder remedialActionAdder, String importableRemedialActionId, UsageMethod usageMethod, Map<String, Set<String>> cnecsByRemedialAction) {
        if (cnecsByRemedialAction.containsKey(importableRemedialActionId)) {
            List<Boolean> flags = cnecsByRemedialAction.get(importableRemedialActionId).stream().map(addOnConstraintUsageRuleForCnec(remedialActionInstant, remedialActionAdder, usageMethod)).toList();
            return flags.contains(true);
        }
        return false;
    }

    private Function<String, Boolean> addOnConstraintUsageRuleForCnec(Instant remedialActionInstant, RemedialActionAdder remedialActionAdder, UsageMethod usageMethod) {
        return cnecId -> {
            Cnec cnec = crac.getCnec(cnecId);
            if (isOnConstraintInstantCoherent(cnec.getState().getInstant().getInstantKind(), remedialActionInstant)) {
                if (cnec instanceof FlowCnec) {
                    remedialActionAdder.newOnFlowConstraintUsageRule()
                        .withInstantId(remedialActionInstant.getId())
                        .withFlowCnec(cnecId)
                        .add();
                    // TODO add .withUsageMethod(usageMethod) when API of OnFlowConstraintAdder is ready
                    return true;
                } else if (cnec instanceof VoltageCnec) {
                    remedialActionAdder.newOnVoltageConstraintUsageRule()
                        .withInstantId(remedialActionInstant.getId())
                        .withVoltageCnec(cnecId)
                        .add();
                    // TODO add .withUsageMethod(usageMethod) when API of OnFlowConstraintAdder is ready
                    return true;
                } else if (cnec instanceof AngleCnec) {
                    remedialActionAdder.newOnAngleConstraintUsageRule()
                        .withInstantId(remedialActionInstant.getId())
                        .withAngleCnec(cnecId)
                        .add();
                    // TODO add .withUsageMethod(usageMethod) when API of OnFlowConstraintAdder is ready
                    return true;
                } else {
                    throw new FaraoException(String.format("Unsupported cnec type %s", cnec.getClass()));
                }
            }
            return false;
        };
    }

    enum RemedialActionType {
        PST_RANGE_ACTION,
        NETWORK_ACTION
    }
}
