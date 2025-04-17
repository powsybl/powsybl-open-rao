/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot.f711;

import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.ComplexVariantsType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.CriticalBranchType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.CriticalBranchesType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.IndependantComplexVariant;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.IdentificationType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.MessageDateTimeType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.MessageType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.MessageTypeList;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.PartyType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.ProcessType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.ProcessTypeList;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.RoleType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.TimeIntervalType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.etso.VersionType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class DailyF711Clusterizer {

    private final List<HourlyF711Info> hourlyF711Infos;
    private final FlowBasedConstraintDocument flowBasedConstraintDocument;
    private static final int DOCUMENT_VERSION = 1;
    private static final MessageTypeList DOCUMENT_TYPE = MessageTypeList.B_06;
    private static final ProcessTypeList PROCESS_TYPE = ProcessTypeList.A_02;

    private static final Comparator<CriticalBranchType> CRITICAL_BRANCH_COMPARATOR =
            Comparator.comparing(DailyF711Clusterizer::getOriginalIdOrId)
                    .thenComparing((CriticalBranchType cb) -> getStartTime(cb.getTimeInterval()))
                    .thenComparing((CriticalBranchType cb) -> getEndTime(cb.getTimeInterval()));

    private static final Comparator<IndependantComplexVariant> COMPLEX_VARIANT_COMPARATOR =
            Comparator.comparing(IndependantComplexVariant::getId)
                    .thenComparing((IndependantComplexVariant icv) -> getStartTime(icv.getTimeInterval()))
                    .thenComparing((IndependantComplexVariant icv) -> getEndTime(icv.getTimeInterval()));

    DailyF711Clusterizer(List<HourlyF711Info> hourlyF711Infos, FlowBasedConstraintDocument flowBasedConstraintDocument) {
        this.hourlyF711Infos = hourlyF711Infos;
        this.flowBasedConstraintDocument = flowBasedConstraintDocument;
    }

    FlowBasedConstraintDocument generateClusterizedDocument() {

        /*
        possible improvement to optimize F303 size: cluster complexVariants with exactly the same RA
         */

        List<IndependantComplexVariant> complexVariantsList = getComplexVariants();
        List<CriticalBranchType> criticalBranchesList = clusterCriticalBranches();
        return generateDailyFbDocument(criticalBranchesList, complexVariantsList);
    }

    private List<IndependantComplexVariant> getComplexVariants() {
        List<IndependantComplexVariant> independentComplexVariants = new ArrayList<>();
        for (HourlyF711Info hourlyInfos : hourlyF711Infos) {
            independentComplexVariants.addAll(hourlyInfos.getComplexVariants());
        }

        independentComplexVariants.sort(COMPLEX_VARIANT_COMPARATOR);

        return independentComplexVariants;
    }

    private List<CriticalBranchType> clusterCriticalBranches() {

        List<CriticalBranchType> criticalBranchTypeList = new ArrayList<>();

        Map<String, List<CriticalBranchType>> criticalBranchesById = arrangeCriticalBranchesById();
        for (Map.Entry<String, List<CriticalBranchType>> entry : criticalBranchesById.entrySet()) {
            entry.getValue().sort(CRITICAL_BRANCH_COMPARATOR);
        }

        criticalBranchesById.forEach((key, cbs) -> {
            List<CriticalBranchType> mergedCbs = mergeCriticalBranches(cbs);
            criticalBranchesById.put(key, mergedCbs);
        });
        for (List<CriticalBranchType> cbt : criticalBranchesById.values()) {
            criticalBranchTypeList.addAll(cbt);
        }

        criticalBranchTypeList.sort(CRITICAL_BRANCH_COMPARATOR);
        return criticalBranchTypeList;
    }

    private FlowBasedConstraintDocument generateDailyFbDocument(List<CriticalBranchType> criticalBranches, List<IndependantComplexVariant> independentComplexVariants) {

        updateHeader(flowBasedConstraintDocument);
        flowBasedConstraintDocument.getCriticalBranches().getCriticalBranch().clear();
        flowBasedConstraintDocument.getComplexVariants().getComplexVariant().clear();
        CriticalBranchesType criticalBranchesType = new CriticalBranchesType();
        criticalBranchesType.getCriticalBranch().addAll(criticalBranches);
        flowBasedConstraintDocument.setCriticalBranches(criticalBranchesType);
        var complexVariantsType = new ComplexVariantsType();
        complexVariantsType.getComplexVariant().addAll(independentComplexVariants);
        flowBasedConstraintDocument.setComplexVariants(complexVariantsType);
        return flowBasedConstraintDocument;
    }

    private static List<CriticalBranchType> mergeCriticalBranches(List<CriticalBranchType> cbs) {
        List<CriticalBranchType> mergedList = new ArrayList<>();
        int i = 0;
        int j;
        while (i < cbs.size()) {
            for (j = i + 1; j < cbs.size(); ++j) {
                if (canBeMerged(cbs.get(i), cbs.get(j))) {
                    updateTimeInterval(cbs.get(i), cbs.get(j));
                } else {
                    break;
                }
            }
            mergedList.add(cbs.get(i));
            i = j;
        }
        return mergedList;
    }

    private static boolean canBeMerged(CriticalBranchType headBranch, CriticalBranchType tailBranch) {
        // If they have the same ID
        if (!headBranch.getId().equals(tailBranch.getId())) {
            return false;
        }

        // Check that timestamps are consecutive
        if (!getEndTime(headBranch.getTimeInterval()).isEqual(getStartTime(tailBranch.getTimeInterval()))) {
            return false;
        }

        // Check that the applied CRAs are the same between the two timestamps
        if (headBranch.getComplexVariantId() == null && tailBranch.getComplexVariantId() != null
                || headBranch.getComplexVariantId() != null && tailBranch.getComplexVariantId() == null
                || headBranch.getComplexVariantId() != null && tailBranch.getComplexVariantId() != null && !headBranch.getComplexVariantId().equals(tailBranch.getComplexVariantId())) {
            return false;
        }

        // Check that the branch has not changed between the two timestamps (can occur if it was initially duplicated in the F301)
        return areCriticalBranchesEquivalent(headBranch, tailBranch);
    }

    private Map<String, List<CriticalBranchType>> arrangeCriticalBranchesById() {
        Map<String, List<CriticalBranchType>> criticalBranchesById = new HashMap<>();
        for (HourlyF711Info hourlyInfos : hourlyF711Infos) {
            for (CriticalBranchType cb : hourlyInfos.getCriticalBranches()) {
                String cbId = cb.getId();
                if (criticalBranchesById.containsKey(cbId)) {
                    criticalBranchesById.get(cbId).add(cb);
                } else {
                    List<CriticalBranchType> cbs = new ArrayList<>();
                    cbs.add(cb);
                    criticalBranchesById.put(cbId, cbs);
                }
            }
        }
        return criticalBranchesById;
    }

    private static void updateHeader(FlowBasedConstraintDocument fbDoc) {
        // Identification
        IdentificationType identificationType = fbDoc.getDocumentIdentification();
        String senderString = fbDoc.getReceiverIdentification().getV();
        String dateString = fbDoc.getConstraintTimeInterval().getV().split("/")[1].substring(0, 10).replace("-", "");
        String documentId = String.format("%s-%s-F711v%s", senderString, dateString, DOCUMENT_VERSION);
        identificationType.setV(documentId);
        fbDoc.setDocumentIdentification(identificationType);

        // Version & co
        VersionType versionType = fbDoc.getDocumentVersion();
        versionType.setV(DOCUMENT_VERSION);
        fbDoc.setDocumentVersion(versionType);
        MessageType messageType = fbDoc.getDocumentType();
        messageType.setV(DOCUMENT_TYPE);
        fbDoc.setDocumentType(messageType);
        ProcessType processType = fbDoc.getProcessType();
        processType.setV(PROCESS_TYPE);
        fbDoc.setProcessType(processType);

        // Invert sender & receiver
        PartyType rev = (PartyType) fbDoc.getSenderIdentification().clone();
        PartyType sender = (PartyType) fbDoc.getReceiverIdentification().clone();
        fbDoc.setReceiverIdentification(rev);
        fbDoc.setSenderIdentification(sender);
        RoleType revRoleType = (RoleType) fbDoc.getSenderRole().clone();
        RoleType senderRoleType = (RoleType) fbDoc.getReceiverRole().clone();
        fbDoc.setReceiverRole(revRoleType);
        fbDoc.setSenderRole(senderRoleType);

        // DateTime
        MessageDateTimeType messageDateTimeType = new MessageDateTimeType();
        XMLGregorianCalendar xmlGregorianCalendar = XmlOutputsUtil.getXMLGregorianCurrentTime();
        messageDateTimeType.setV(xmlGregorianCalendar);
        fbDoc.setCreationDateTime(messageDateTimeType);
        fbDoc.setConstraintTimeInterval(fbDoc.getConstraintTimeInterval());
        fbDoc.setDomain(fbDoc.getDomain());
    }

    private static void updateTimeInterval(CriticalBranchType headBranch, CriticalBranchType tailBranch) {
        OffsetDateTime start = getStartTime(headBranch.getTimeInterval());
        OffsetDateTime end = getEndTime(tailBranch.getTimeInterval());
        TimeIntervalType timeIntervalType = new TimeIntervalType();
        String timeIntervalString = start.toString() + "/" + end.toString();
        timeIntervalType.setV(timeIntervalString);
        headBranch.setTimeInterval(timeIntervalType);
    }

    private static OffsetDateTime getStartTime(TimeIntervalType timeIntervalType) {
        String[] timeInterval = timeIntervalType.getV().split("/");
        return OffsetDateTime.parse(timeInterval[0]);
    }

    private static OffsetDateTime getEndTime(TimeIntervalType timeIntervalType) {
        String[] timeInterval = timeIntervalType.getV().split("/");
        return OffsetDateTime.parse(timeInterval[1]);
    }

    private static String getOriginalIdOrId(CriticalBranchType cb) {
        if (!Objects.isNull(cb.getOriginalId())) {
            return cb.getOriginalId();
        } else {
            return cb.getId();
        }
    }

    private static boolean areCriticalBranchesEquivalent(CriticalBranchType cb1, CriticalBranchType cb2) {
        return cb1.getBranch().equals(cb2.getBranch())
                && areImaxFactorsEqual(cb1, cb2)
                && areImaxEqual(cb1, cb2)
                && areMinRamFactorEqual(cb1, cb2)
                && areOutageEqual(cb1, cb2)
                && areDirectionEqual(cb1, cb2)
                && areOriginEqual(cb1, cb2)
                && Math.abs(cb1.getFrmMw() - cb2.getFrmMw()) < 1e-6
                && areMnecEqual(cb1, cb2)
                && areCnecEqual(cb1, cb2);
    }

    private static boolean areCnecEqual(CriticalBranchType cb1, CriticalBranchType cb2) {
        return Objects.equals(cb1.isCNEC(), cb2.isCNEC());
    }

    private static boolean areMnecEqual(CriticalBranchType cb1, CriticalBranchType cb2) {
        return Objects.equals(cb1.isMNEC(), cb2.isMNEC());
    }

    private static boolean areOriginEqual(CriticalBranchType cb1, CriticalBranchType cb2) {
        return Objects.equals(cb1.getTsoOrigin(), cb2.getTsoOrigin());
    }

    private static boolean areDirectionEqual(CriticalBranchType cb1, CriticalBranchType cb2) {
        return Objects.equals(cb1.getDirection(), cb2.getDirection());
    }

    private static boolean areOutageEqual(CriticalBranchType cb1, CriticalBranchType cb2) {
        return Objects.equals(cb1.getOutage(), cb2.getOutage());
    }

    private static boolean areMinRamFactorEqual(CriticalBranchType cb1, CriticalBranchType cb2) {
        return cb1.getMinRAMfactor() == null && cb2.getMinRAMfactor() == null || cb1.getMinRAMfactor() != null && cb2.getMinRAMfactor() != null && Math.abs(cb1.getMinRAMfactor().doubleValue() - cb2.getMinRAMfactor().doubleValue()) < 1e-6;
    }

    private static boolean areImaxEqual(CriticalBranchType cb1, CriticalBranchType cb2) {
        return cb1.getImaxA() == null && cb2.getImaxA() == null || cb1.getImaxA() != null && cb2.getImaxA() != null && Math.abs(cb1.getImaxA().doubleValue() - cb2.getImaxA().doubleValue()) < 1e-6;
    }

    private static boolean areImaxFactorsEqual(CriticalBranchType cb1, CriticalBranchType cb2) {
        return cb1.getImaxFactor() == null && cb2.getImaxFactor() == null || cb1.getImaxFactor() != null && cb2.getImaxFactor() != null && Math.abs(cb1.getImaxFactor().doubleValue() - cb2.getImaxFactor().doubleValue()) < 1e-6;
    }
}
