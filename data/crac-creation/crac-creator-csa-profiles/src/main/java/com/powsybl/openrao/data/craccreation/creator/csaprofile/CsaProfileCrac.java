/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.cim.CurrentLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.cim.VoltageLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithContingency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.Contingency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyEquipment;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.GridStateAlterationCollection;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionDependency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionGroup;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RemedialActionScheme;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.RotatingMachineAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ShuntCompensatorModification;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.Stage;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.StaticPropertyRange;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TapPositionAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TopologyAction;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.VoltageAngleLimit;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils.isValidInterval;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCrac implements NativeCrac {

    private final TripleStore tripleStoreCsaProfileCrac;

    private final QueryCatalog queryCatalogCsaProfileCrac;

    private final Map<String, Set<String>> keywordMap;
    private Map<String, String> overridingData;

    public CsaProfileCrac(TripleStore tripleStoreCsaProfileCrac, Map<String, Set<String>> keywordMap) {
        this.tripleStoreCsaProfileCrac = tripleStoreCsaProfileCrac;
        this.queryCatalogCsaProfileCrac = new QueryCatalog(CsaProfileConstants.SPARQL_FILE_CSA_PROFILE);
        this.keywordMap = keywordMap;
        this.overridingData = new HashMap<>();
    }

    @Override
    public String getFormat() {
        return "CsaProfileCrac";
    }

    public void clearContext(String context) {
        tripleStoreCsaProfileCrac.clear(context);
    }

    public void clearKeywordMap(String context) {
        for (Map.Entry<String, Set<String>> entry : keywordMap.entrySet()) {
            String keyword = entry.getKey();
            Set<String> contextNames = entry.getValue();
            if (contextNames.contains(context)) {
                contextNames.remove(context);
                keywordMap.put(keyword, contextNames);
                break;
            }
        }
    }

    private Set<String> getContextNamesToRequest(String keyword) {
        if (keywordMap.containsKey(keyword)) {
            return keywordMap.get(keyword);
        }
        return Collections.emptySet();
    }

    public Map<String, PropertyBags> getHeaders() {
        Map<String, PropertyBags> returnMap = new HashMap<>();
        tripleStoreCsaProfileCrac.contextNames().forEach(context -> returnMap.put(context, queryTripleStore(CsaProfileConstants.REQUEST_HEADER, Set.of(context))));
        return returnMap;
    }

    public PropertyBags getPropertyBags(String csaProfileConstant, String keyword) {
        Set<String> namesToRequest = getContextNamesToRequest(keyword);
        if (namesToRequest.isEmpty()) {
            return new PropertyBags();
        }
        return this.queryTripleStore(csaProfileConstant, namesToRequest);
    }

    public PropertyBags getPropertyBags(List<String> csaProfileConstants, String keyword) {
        Set<String> namesToRequest = getContextNamesToRequest(keyword);
        if (namesToRequest.isEmpty()) {
            return new PropertyBags();
        }
        return this.queryTripleStore(csaProfileConstants, namesToRequest);
    }

    public Set<Contingency> getContingencies() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(Arrays.asList(CsaProfileConstants.REQUEST_ORDINARY_CONTINGENCY, CsaProfileConstants.REQUEST_EXCEPTIONAL_CONTINGENCY, CsaProfileConstants.REQUEST_OUT_OF_RANGE_CONTINGENCY), CsaProfileConstants.CsaProfileKeywords.CONTINGENCY.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.CONTINGENCY).stream().map(Contingency::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<ContingencyEquipment> getContingencyEquipments() {
        return getPropertyBags(CsaProfileConstants.REQUEST_CONTINGENCY_EQUIPMENT, CsaProfileConstants.CsaProfileKeywords.CONTINGENCY.toString()).stream().map(ContingencyEquipment::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<AssessedElement> getAssessedElements() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT, CsaProfileConstants.CsaProfileKeywords.ASSESSED_ELEMENT.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT).stream().map(AssessedElement::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<AssessedElementWithContingency> getAssessedElementWithContingencies() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY, CsaProfileConstants.CsaProfileKeywords.ASSESSED_ELEMENT.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT_WITH_CONTINGENCY).stream().map(AssessedElementWithContingency::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<AssessedElementWithRemedialAction> getAssessedElementWithRemedialActions() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION, CsaProfileConstants.CsaProfileKeywords.ASSESSED_ELEMENT.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION).stream().map(AssessedElementWithRemedialAction::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<CurrentLimit> getCurrentLimits() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REQUEST_CURRENT_LIMIT, CsaProfileConstants.CGMES), overridingData, CsaProfileConstants.OverridingObjectsFields.CURRENT_LIMIT).stream().map(CurrentLimit::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<VoltageLimit> getVoltageLimits() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REQUEST_VOLTAGE_LIMIT, CsaProfileConstants.CGMES), overridingData, CsaProfileConstants.OverridingObjectsFields.VOLTAGE_LIMIT).stream().map(VoltageLimit::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<VoltageAngleLimit> getVoltageAngleLimits() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REQUEST_VOLTAGE_ANGLE_LIMIT, CsaProfileConstants.CsaProfileKeywords.EQUIPMENT_RELIABILITY.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.VOLTAGE_ANGLE_LIMIT).stream().map(VoltageAngleLimit::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<RemedialAction> getGridStateAlterationRemedialActions() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.GRID_STATE_ALTERATION_REMEDIAL_ACTION).stream().map(propertyBag -> RemedialAction.fromPropertyBag(propertyBag, false)).collect(Collectors.toSet());
    }

    public Set<TopologyAction> getTopologyActions() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.TOPOLOGY_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.TOPOLOGY_ACTION).stream().map(TopologyAction::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<RotatingMachineAction> getRotatingMachineActions() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.ROTATING_MACHINE_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.ROTATING_MACHINE_ACTION).stream().map(RotatingMachineAction::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<ShuntCompensatorModification> getShuntCompensatorModifications() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.SHUNT_COMPENSATOR_MODIFICATION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.SHUNT_COMPENSATOR_MODIFICATION).stream().map(ShuntCompensatorModification::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<TapPositionAction> getTapPositionActions() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.TAP_POSITION_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.TAP_POSITION_ACTION).stream().map(TapPositionAction::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<StaticPropertyRange> getStaticPropertyRanges() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.STATIC_PROPERTY_RANGE, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.STATIC_PROPERTY_RANGE).stream().map(StaticPropertyRange::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<ContingencyWithRemedialAction> getContingencyWithRemedialActions() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.CONTINGENCY_WITH_REMEDIAL_ACTION).stream().map(ContingencyWithRemedialAction::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<Stage> getStages() {
        return getPropertyBags(CsaProfileConstants.STAGE, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()).stream().map(Stage::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<GridStateAlterationCollection> getGridStateAlterationCollections() {
        return getPropertyBags(CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()).stream().map(GridStateAlterationCollection::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<RemedialActionScheme> getRemedialActionSchemes() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REMEDIAL_ACTION_SCHEME, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.REMEDIAL_ACTION_SCHEME).stream().map(RemedialActionScheme::fromPropertyBag).collect(Collectors.toSet());
    }

    public Set<RemedialAction> getSchemeRemedialActions() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REQUEST_SCHEME_REMEDIAL_ACTION, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.SCHEME_REMEDIAL_ACTION).stream().map(propertyBag -> RemedialAction.fromPropertyBag(propertyBag, true)).collect(Collectors.toSet());
    }

    public Set<RemedialActionGroup> getRemedialActionGroups() {
        return getPropertyBags(CsaProfileConstants.REQUEST_REMEDIAL_ACTION_GROUP, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()).stream().map(RemedialActionGroup::fromPropertyBag).collect(Collectors.toSet());

    }

    public Set<RemedialActionDependency> getRemedialActionDependencies() {
        return CsaProfileCracUtils.overrideData(getPropertyBags(CsaProfileConstants.REQUEST_REMEDIAL_ACTION_DEPENDENCY, CsaProfileConstants.CsaProfileKeywords.REMEDIAL_ACTION.toString()), overridingData, CsaProfileConstants.OverridingObjectsFields.SCHEME_REMEDIAL_ACTION_DEPENDENCY).stream().map(RemedialActionDependency::fromPropertyBag).collect(Collectors.toSet());

    }

    private void setOverridingData(OffsetDateTime importTimestamp) {
        overridingData = new HashMap<>();
        for (CsaProfileConstants.OverridingObjectsFields overridingObject : CsaProfileConstants.OverridingObjectsFields.values()) {
            addDataFromTripleStoreToMap(overridingData, overridingObject.getRequestName(), overridingObject.getObjectName(), overridingObject.getOverridedFieldName(), overridingObject.getHeaderType(), importTimestamp);
        }
    }

    private void addDataFromTripleStoreToMap(Map<String, String> dataMap, String queryName, String queryObjectName, String queryFieldName, CsaProfileConstants.HeaderType headerType, OffsetDateTime importTimestamp) {
        PropertyBags propertyBagsResult = queryTripleStore(queryName, tripleStoreCsaProfileCrac.contextNames());
        for (PropertyBag propertyBag : propertyBagsResult) {
            if (CsaProfileConstants.HeaderType.START_END_DATE.equals(headerType)) {
                if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileConstants.CsaProfileKeywords.STEADY_STATE_INSTRUCTION) && CsaProfileCracUtils.checkProfileValidityInterval(propertyBag, importTimestamp)) {
                    String id = propertyBag.getId(queryObjectName);
                    String overridedValue = propertyBag.get(queryFieldName);
                    dataMap.put(id, overridedValue);
                }
            } else {
                if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileConstants.CsaProfileKeywords.STEADY_STATE_HYPOTHESIS)) {
                    OffsetDateTime scenarioTime = OffsetDateTime.parse(propertyBag.get(CsaProfileConstants.SCENARIO_TIME));
                    if (importTimestamp.isEqual(scenarioTime)) {
                        String id = propertyBag.getId(queryObjectName);
                        String overridedValue = propertyBag.get(queryFieldName);
                        dataMap.put(id, overridedValue);
                    }
                }
            }

        }
    }

    private PropertyBags queryTripleStore(List<String> queryKeys, Set<String> contexts) {
        PropertyBags mergedPropertyBags = new PropertyBags();
        for (String queryKey : queryKeys) {
            mergedPropertyBags.addAll(queryTripleStore(queryKey, contexts));
        }
        return mergedPropertyBags;
    }

    /**
     * execute query on the whole tripleStore or on each context included in the set
     *
     * @param queryKey : query name in the sparql file
     * @param contexts : list of contexts where the query will be executed (if empty, the query is executed on the whole tripleStore
     */
    private PropertyBags queryTripleStore(String queryKey, Set<String> contexts) {
        String query = queryCatalogCsaProfileCrac.get(queryKey);
        if (query == null) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("Query [{}] not found in catalog", queryKey);
            return new PropertyBags();
        }

        if (contexts.isEmpty()) {
            return tripleStoreCsaProfileCrac.query(query);
        }

        PropertyBags multiContextsPropertyBags = new PropertyBags();
        for (String context : contexts) {
            String contextQuery = String.format(query, context);
            multiContextsPropertyBags.addAll(tripleStoreCsaProfileCrac.query(contextQuery));
        }
        return multiContextsPropertyBags;
    }

    public void setForTimestamp(OffsetDateTime offsetDateTime) {
        clearTimewiseIrrelevantContexts(offsetDateTime);
        setOverridingData(offsetDateTime);
    }

    private void clearTimewiseIrrelevantContexts(OffsetDateTime offsetDateTime) {
        getHeaders().forEach((contextName, properties) -> {
            if (!properties.isEmpty()) {
                PropertyBag property = properties.get(0);
                if (!checkTimeCoherence(property, offsetDateTime)) {
                    OpenRaoLoggerProvider.BUSINESS_WARNS.warn(String.format("[REMOVED] The file : %s will be ignored. Its dates are not consistent with the import date : %s", contextName, offsetDateTime));
                    clearContext(contextName);
                    clearKeywordMap(contextName);
                }
            }
        });
    }

    private static boolean checkTimeCoherence(PropertyBag header, OffsetDateTime offsetDateTime) {
        String startTime = header.getId(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = header.getId(CsaProfileConstants.REQUEST_HEADER_END_DATE);
        return isValidInterval(offsetDateTime, startTime, endTime);
    }
}
