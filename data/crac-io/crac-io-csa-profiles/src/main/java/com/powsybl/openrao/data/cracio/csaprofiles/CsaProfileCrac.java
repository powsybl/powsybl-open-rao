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
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.Query;
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
        tripleStoreCsaProfileCrac.contextNames().forEach(context -> returnMap.put(context, queryTripleStore(Query.HEADER.getTitle(), Set.of(context))));
        return returnMap;
    }

    public PropertyBags getPropertyBags(Query query) {
        Set<String> namesToRequest = getContextNamesToRequest(query.getTargetProfilesKeyword());
        if (namesToRequest.isEmpty()) {
            return new PropertyBags();
        }
        return CsaProfileCracUtils.overrideQuery(this.queryTripleStore(List.of(query.getTitle()), namesToRequest), query, overridingData);
    }

    public Set<Contingency> getContingencies() {
        return new NcPropertyBagsConverter<>(Contingency::fromPropertyBag).convert(getPropertyBags(Query.CONTINGENCY));
    }

    public Set<ContingencyEquipment> getContingencyEquipments() {
        return new NcPropertyBagsConverter<>(ContingencyEquipment::fromPropertyBag).convert(getPropertyBags(Query.CONTINGENCY_EQUIPMENT));
    }

    public Set<AssessedElement> getAssessedElements() {
        return new NcPropertyBagsConverter<>(AssessedElement::fromPropertyBag).convert(getPropertyBags(Query.ASSESSED_ELEMENT));
    }

    public Set<AssessedElementWithContingency> getAssessedElementWithContingencies() {
        return new NcPropertyBagsConverter<>(AssessedElementWithContingency::fromPropertyBag).convert(getPropertyBags(Query.ASSESSED_ELEMENT_WITH_CONTINGENCY));
    }

    public Set<AssessedElementWithRemedialAction> getAssessedElementWithRemedialActions() {
        return new NcPropertyBagsConverter<>(AssessedElementWithRemedialAction::fromPropertyBag).convert(getPropertyBags(Query.ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION));
    }

    public Set<CurrentLimit> getCurrentLimits() {
        return new NcPropertyBagsConverter<>(CurrentLimit::fromPropertyBag).convert(getPropertyBags(Query.CURRENT_LIMIT));
    }

    public Set<VoltageLimit> getVoltageLimits() {
        return new NcPropertyBagsConverter<>(VoltageLimit::fromPropertyBag).convert(getPropertyBags(Query.VOLTAGE_LIMIT));
    }

    public Set<VoltageAngleLimit> getVoltageAngleLimits() {
        return new NcPropertyBagsConverter<>(VoltageAngleLimit::fromPropertyBag).convert(getPropertyBags(Query.VOLTAGE_ANGLE_LIMIT));
    }

    public Set<GridStateAlterationRemedialAction> getGridStateAlterationRemedialActions() {
        return new NcPropertyBagsConverter<>(GridStateAlterationRemedialAction::fromPropertyBag).convert(getPropertyBags(Query.GRID_STATE_ALTERATION_REMEDIAL_ACTION));
    }

    public Set<TopologyAction> getTopologyActions() {
        return new NcPropertyBagsConverter<>(TopologyAction::fromPropertyBag).convert(getPropertyBags(Query.TOPOLOGY_ACTION));
    }

    public Set<RotatingMachineAction> getRotatingMachineActions() {
        return new NcPropertyBagsConverter<>(RotatingMachineAction::fromPropertyBag).convert(getPropertyBags(Query.ROTATING_MACHINE_ACTION));
    }

    public Set<ShuntCompensatorModification> getShuntCompensatorModifications() {
        return new NcPropertyBagsConverter<>(ShuntCompensatorModification::fromPropertyBag).convert(getPropertyBags(Query.SHUNT_COMPENSATOR_MODIFICATION));
    }

    public Set<TapPositionAction> getTapPositionActions() {
        return new NcPropertyBagsConverter<>(TapPositionAction::fromPropertyBag).convert(getPropertyBags(Query.TAP_POSITION_ACTION));
    }

    public Set<StaticPropertyRange> getStaticPropertyRanges() {
        return new NcPropertyBagsConverter<>(StaticPropertyRange::fromPropertyBag).convert(getPropertyBags(Query.STATIC_PROPERTY_RANGE));
    }

    public Set<ContingencyWithRemedialAction> getContingencyWithRemedialActions() {
        return new NcPropertyBagsConverter<>(ContingencyWithRemedialAction::fromPropertyBag).convert(getPropertyBags(Query.CONTINGENCY_WITH_REMEDIAL_ACTION));
    }

    public Set<Stage> getStages() {
        return new NcPropertyBagsConverter<>(Stage::fromPropertyBag).convert(getPropertyBags(Query.STAGE));
    }

    public Set<GridStateAlterationCollection> getGridStateAlterationCollections() {
        return new NcPropertyBagsConverter<>(GridStateAlterationCollection::fromPropertyBag).convert(getPropertyBags(Query.GRID_STATE_ALTERATION_COLLECTION));
    }

    public Set<RemedialActionScheme> getRemedialActionSchemes() {
        return new NcPropertyBagsConverter<>(RemedialActionScheme::fromPropertyBag).convert(getPropertyBags(Query.REMEDIAL_ACTION_SCHEME));
    }

    public Set<SchemeRemedialAction> getSchemeRemedialActions() {
        return new NcPropertyBagsConverter<>(SchemeRemedialAction::fromPropertyBag).convert(getPropertyBags(Query.SCHEME_REMEDIAL_ACTION));
    }

    public Set<RemedialActionGroup> getRemedialActionGroups() {
        return new NcPropertyBagsConverter<>(RemedialActionGroup::fromPropertyBag).convert(getPropertyBags(Query.REMEDIAL_ACTION_GROUP));

    }

    public Set<RemedialActionDependency> getRemedialActionDependencies() {
        return new NcPropertyBagsConverter<>(RemedialActionDependency::fromPropertyBag).convert(getPropertyBags(Query.REMEDIAL_ACTION_DEPENDENCY));
    }

    public Set<TapChanger> getTapChangers() {
        return new NcPropertyBagsConverter<>(TapChanger::fromPropertyBag).convert(getPropertyBags(Query.TAP_CHANGER));
    }

    private void setOverridingData(OffsetDateTime importTimestamp) {
        overridingData = new HashMap<>();
        Arrays.stream(Query.values()).forEach(query -> addDataFromTripleStoreToMap(overridingData, query, importTimestamp));
    }

    private void addDataFromTripleStoreToMap(Map<String, String> dataMap, Query query, OffsetDateTime importTimestamp) {
        if (query.getOverridableFields().isEmpty()) {
            return;
        }
        PropertyBags propertyBagsResult = queryTripleStore(query.getTitle() + "Overriding", tripleStoreCsaProfileCrac.contextNames());
        for (PropertyBag propertyBag : propertyBagsResult) {
            if (!CsaProfileKeyword.CGMES.equals(query.getTargetProfilesKeyword())) {
                for (String overridingField : query.getOverridableFields().values()) {
                    if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileKeyword.STEADY_STATE_INSTRUCTION) && CsaProfileCracUtils.checkProfileValidityInterval(propertyBag, importTimestamp)) {
                        String id = propertyBag.getId(query.getTitle());
                        String overridingValue = propertyBag.get(overridingField);
                        dataMap.put(id, overridingValue);
                    }
                }
            } else {
                if (CsaProfileCracUtils.checkProfileKeyword(propertyBag, CsaProfileKeyword.STEADY_STATE_HYPOTHESIS)) {
                    OffsetDateTime scenarioTime = OffsetDateTime.parse(propertyBag.get(CsaProfileConstants.SCENARIO_TIME));
                    for (String overridingField : query.getOverridableFields().values()) {
                        if (importTimestamp.isEqual(scenarioTime)) {
                            String id = propertyBag.getId(query.getTitle());
                            String overridingValue = propertyBag.get(overridingField);
                            dataMap.put(id, overridingValue);
                        }
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
        String startTime = header.getId(CsaProfileConstants.START_DATE);
        String endTime = header.getId(CsaProfileConstants.END_DATE);
        return CsaProfileCracUtils.isValidInterval(offsetDateTime, startTime, endTime);
    }
}
