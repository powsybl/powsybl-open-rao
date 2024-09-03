/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.NcPropertyBagsConverter;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileKeyword;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.HeaderType;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.OverridingObjectsFields;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.*;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCrac {

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

    private Set<String> getContextNamesToRequest(CsaProfileKeyword keyword) {
        return keywordMap.getOrDefault(keyword.toString(), Collections.emptySet());
    }

    public Map<String, PropertyBags> getHeaders() {
        Map<String, PropertyBags> returnMap = new HashMap<>();
        tripleStoreCsaProfileCrac.contextNames().forEach(context -> returnMap.put(context, queryTripleStore(CsaProfileConstants.REQUEST_HEADER, Set.of(context))));
        return returnMap;
    }

    public PropertyBags getPropertyBags(CsaProfileKeyword keyword, String... queries) {
        Set<String> namesToRequest = getContextNamesToRequest(keyword);
        if (namesToRequest.isEmpty()) {
            return new PropertyBags();
        }
        return this.queryTripleStore(List.of(queries), namesToRequest);
    }

    public PropertyBags getPropertyBags(CsaProfileKeyword keyword, OverridingObjectsFields withOverride, String... queries) {
        return withOverride == null ? getPropertyBags(keyword, queries) : CsaProfileCracUtils.overrideData(getPropertyBags(keyword, queries), overridingData, withOverride);
    }

    public Set<Contingency> getContingencies() {
        return new NcPropertyBagsConverter<>(Contingency::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.CONTINGENCY, OverridingObjectsFields.CONTINGENCY, CsaProfileConstants.REQUEST_ORDINARY_CONTINGENCY, CsaProfileConstants.REQUEST_EXCEPTIONAL_CONTINGENCY, CsaProfileConstants.REQUEST_OUT_OF_RANGE_CONTINGENCY));
    }

    public Set<ContingencyEquipment> getContingencyEquipments() {
        return new NcPropertyBagsConverter<>(ContingencyEquipment::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.CONTINGENCY, CsaProfileConstants.REQUEST_CONTINGENCY_EQUIPMENT));
    }

    public Set<AssessedElement> getAssessedElements() {
        return new NcPropertyBagsConverter<>(AssessedElement::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.ASSESSED_ELEMENT, OverridingObjectsFields.ASSESSED_ELEMENT, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT));
    }

    public Set<AssessedElementWithContingency> getAssessedElementWithContingencies() {
        return new NcPropertyBagsConverter<>(AssessedElementWithContingency::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.ASSESSED_ELEMENT, OverridingObjectsFields.ASSESSED_ELEMENT_WITH_CONTINGENCY, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY));
    }

    public Set<AssessedElementWithRemedialAction> getAssessedElementWithRemedialActions() {
        return new NcPropertyBagsConverter<>(AssessedElementWithRemedialAction::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.ASSESSED_ELEMENT, OverridingObjectsFields.ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION, CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION));
    }

    public Set<CurrentLimit> getCurrentLimits() {
        return new NcPropertyBagsConverter<>(CurrentLimit::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.CGMES, OverridingObjectsFields.CURRENT_LIMIT, CsaProfileConstants.REQUEST_CURRENT_LIMIT));
    }

    public Set<VoltageLimit> getVoltageLimits() {
        return new NcPropertyBagsConverter<>(VoltageLimit::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.CGMES, OverridingObjectsFields.VOLTAGE_LIMIT, CsaProfileConstants.REQUEST_VOLTAGE_LIMIT));
    }

    public Set<VoltageAngleLimit> getVoltageAngleLimits() {
        return new NcPropertyBagsConverter<>(VoltageAngleLimit::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.EQUIPMENT_RELIABILITY, OverridingObjectsFields.VOLTAGE_ANGLE_LIMIT, CsaProfileConstants.REQUEST_VOLTAGE_ANGLE_LIMIT));
    }

    public Set<GridStateAlterationRemedialAction> getGridStateAlterationRemedialActions() {
        return new NcPropertyBagsConverter<>(GridStateAlterationRemedialAction::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.GRID_STATE_ALTERATION_REMEDIAL_ACTION, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION));
    }

    public Set<TopologyAction> getTopologyActions() {
        return new NcPropertyBagsConverter<>(TopologyAction::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.TOPOLOGY_ACTION, CsaProfileConstants.TOPOLOGY_ACTION));
    }

    public Set<RotatingMachineAction> getRotatingMachineActions() {
        return new NcPropertyBagsConverter<>(RotatingMachineAction::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.ROTATING_MACHINE_ACTION, CsaProfileConstants.ROTATING_MACHINE_ACTION));
    }

    public Set<ShuntCompensatorModification> getShuntCompensatorModifications() {
        return new NcPropertyBagsConverter<>(ShuntCompensatorModification::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.SHUNT_COMPENSATOR_MODIFICATION, CsaProfileConstants.SHUNT_COMPENSATOR_MODIFICATION));
    }

    public Set<TapPositionAction> getTapPositionActions() {
        return new NcPropertyBagsConverter<>(TapPositionAction::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.TAP_POSITION_ACTION, CsaProfileConstants.TAP_POSITION_ACTION));
    }

    public Set<StaticPropertyRange> getStaticPropertyRanges() {
        return new NcPropertyBagsConverter<>(StaticPropertyRange::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.STATIC_PROPERTY_RANGE, CsaProfileConstants.STATIC_PROPERTY_RANGE));
    }

    public Set<ContingencyWithRemedialAction> getContingencyWithRemedialActions() {
        return new NcPropertyBagsConverter<>(ContingencyWithRemedialAction::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.CONTINGENCY_WITH_REMEDIAL_ACTION, CsaProfileConstants.REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION));
    }

    public Set<Stage> getStages() {
        return new NcPropertyBagsConverter<>(Stage::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.STAGE));
    }

    public Set<GridStateAlterationCollection> getGridStateAlterationCollections() {
        return new NcPropertyBagsConverter<>(GridStateAlterationCollection::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION));
    }

    public Set<RemedialActionScheme> getRemedialActionSchemes() {
        return new NcPropertyBagsConverter<>(RemedialActionScheme::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.REMEDIAL_ACTION_SCHEME, CsaProfileConstants.REMEDIAL_ACTION_SCHEME));
    }

    public Set<SchemeRemedialAction> getSchemeRemedialActions() {
        return new NcPropertyBagsConverter<>(SchemeRemedialAction::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.SCHEME_REMEDIAL_ACTION, CsaProfileConstants.REQUEST_SCHEME_REMEDIAL_ACTION));
    }

    public Set<RemedialActionGroup> getRemedialActionGroups() {
        return new NcPropertyBagsConverter<>(RemedialActionGroup::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, CsaProfileConstants.REQUEST_REMEDIAL_ACTION_GROUP));

    }

    public Set<RemedialActionDependency> getRemedialActionDependencies() {
        return new NcPropertyBagsConverter<>(RemedialActionDependency::fromPropertyBag).convert(getPropertyBags(CsaProfileKeyword.REMEDIAL_ACTION, OverridingObjectsFields.SCHEME_REMEDIAL_ACTION_DEPENDENCY, CsaProfileConstants.REQUEST_REMEDIAL_ACTION_DEPENDENCY));
    }

    private void setOverridingData(OffsetDateTime importTimestamp) {
        overridingData = new HashMap<>();
        for (OverridingObjectsFields overridingObject : OverridingObjectsFields.values()) {
            addDataFromTripleStoreToMap(overridingData, overridingObject.getRequestName(), overridingObject.getObjectName(), overridingObject.getOverridedFieldName(), overridingObject.getHeaderType(), importTimestamp);
        }
    }

    private void addDataFromTripleStoreToMap(Map<String, String> dataMap, String queryName, String queryObjectName, String queryFieldName, HeaderType headerType, OffsetDateTime importTimestamp) {
        PropertyBags propertyBagsResult = queryTripleStore(queryName, tripleStoreCsaProfileCrac.contextNames());
        for (PropertyBag propertyBag : propertyBagsResult) {
            if (HeaderType.START_END_DATE.equals(headerType)) {
                if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileKeyword.STEADY_STATE_INSTRUCTION) && CsaProfileCracUtils.checkProfileValidityInterval(propertyBag, importTimestamp)) {
                    String id = propertyBag.getId(queryObjectName);
                    String overridedValue = propertyBag.get(queryFieldName);
                    dataMap.put(id, overridedValue);
                }
            } else {
                if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileKeyword.STEADY_STATE_HYPOTHESIS)) {
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
        return CsaProfileCracUtils.isValidInterval(offsetDateTime, startTime, endTime);
    }
}
