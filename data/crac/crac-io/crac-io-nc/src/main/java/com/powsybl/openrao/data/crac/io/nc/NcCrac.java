/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcPropertyBagsConverter;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.HeaderType;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcKeyword;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.OverridingObjectsFields;
import com.powsybl.openrao.data.crac.io.nc.objects.AssessedElement;
import com.powsybl.openrao.data.crac.io.nc.objects.AssessedElementWithContingency;
import com.powsybl.openrao.data.crac.io.nc.objects.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.crac.io.nc.objects.Contingency;
import com.powsybl.openrao.data.crac.io.nc.objects.ContingencyEquipment;
import com.powsybl.openrao.data.crac.io.nc.objects.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.crac.io.nc.objects.CurrentLimit;
import com.powsybl.openrao.data.crac.io.nc.objects.GridStateAlterationRemedialAction;
import com.powsybl.openrao.data.crac.io.nc.objects.RemedialActionDependency;
import com.powsybl.openrao.data.crac.io.nc.objects.RemedialActionGroup;
import com.powsybl.openrao.data.crac.io.nc.objects.RotatingMachineAction;
import com.powsybl.openrao.data.crac.io.nc.objects.ShuntCompensatorModification;
import com.powsybl.openrao.data.crac.io.nc.objects.StaticPropertyRange;
import com.powsybl.openrao.data.crac.io.nc.objects.TapChanger;
import com.powsybl.openrao.data.crac.io.nc.objects.TapPositionAction;
import com.powsybl.openrao.data.crac.io.nc.objects.TopologyAction;
import com.powsybl.openrao.data.crac.io.nc.objects.VoltageAngleLimit;
import com.powsybl.openrao.data.crac.io.nc.objects.VoltageLimit;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class NcCrac {

    private final TripleStore tripleStoreNcCrac;

    private final QueryCatalog queryCatalogNcCrac;

    private final Map<String, Set<String>> keywordMap;
    private Map<String, String> overridingData;

    public NcCrac(TripleStore tripleStoreNcCrac, Map<String, Set<String>> keywordMap) {
        this.tripleStoreNcCrac = tripleStoreNcCrac;
        this.queryCatalogNcCrac = new QueryCatalog(NcConstants.SPARQL_FILE_NC_PROFILE);
        this.keywordMap = keywordMap;
        this.overridingData = new HashMap<>();
    }

    public void clearContext(String context) {
        tripleStoreNcCrac.clear(context);
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

    private Set<String> getContextNamesToRequest(NcKeyword keyword) {
        return keywordMap.getOrDefault(keyword.toString(), Collections.emptySet());
    }

    public Map<String, PropertyBags> getHeaders() {
        Map<String, PropertyBags> returnMap = new HashMap<>();
        tripleStoreNcCrac.contextNames().forEach(context -> returnMap.put(context, queryTripleStore(NcConstants.REQUEST_HEADER, Set.of(context))));
        return returnMap;
    }

    public PropertyBags getPropertyBags(NcKeyword keyword, String... queries) {
        Set<String> namesToRequest = getContextNamesToRequest(keyword);
        if (namesToRequest.isEmpty()) {
            return new PropertyBags();
        }
        return this.queryTripleStore(List.of(queries), namesToRequest);
    }

    public PropertyBags getPropertyBags(NcKeyword keyword, OverridingObjectsFields withOverride, String... queries) {
        return withOverride == null ? getPropertyBags(keyword, queries) : NcCracUtils.overrideData(getPropertyBags(keyword, queries), overridingData, withOverride);
    }

    public Set<Contingency> getContingencies() {
        return new NcPropertyBagsConverter<>(Contingency::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.CONTINGENCY, OverridingObjectsFields.CONTINGENCY, NcConstants.REQUEST_ORDINARY_CONTINGENCY,
                                     NcConstants.REQUEST_EXCEPTIONAL_CONTINGENCY, NcConstants.REQUEST_OUT_OF_RANGE_CONTINGENCY));
    }

    public Set<ContingencyEquipment> getContingencyEquipments() {
        return new NcPropertyBagsConverter<>(ContingencyEquipment::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.CONTINGENCY, NcConstants.REQUEST_CONTINGENCY_EQUIPMENT));
    }

    public Set<AssessedElement> getAssessedElements() {
        return new NcPropertyBagsConverter<>(AssessedElement::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.ASSESSED_ELEMENT, OverridingObjectsFields.ASSESSED_ELEMENT, NcConstants.REQUEST_ASSESSED_ELEMENT));
    }

    public Set<AssessedElementWithContingency> getAssessedElementWithContingencies() {
        return new NcPropertyBagsConverter<>(AssessedElementWithContingency::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.ASSESSED_ELEMENT, OverridingObjectsFields.ASSESSED_ELEMENT_WITH_CONTINGENCY, NcConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY));
    }

    public Set<AssessedElementWithRemedialAction> getAssessedElementWithRemedialActions() {
        return new NcPropertyBagsConverter<>(AssessedElementWithRemedialAction::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.ASSESSED_ELEMENT, OverridingObjectsFields.ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION, NcConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION));
    }

    public Set<CurrentLimit> getCurrentLimits() {
        return new NcPropertyBagsConverter<>(CurrentLimit::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.CGMES, OverridingObjectsFields.CURRENT_LIMIT, NcConstants.REQUEST_CURRENT_LIMIT));
    }

    public Set<VoltageLimit> getVoltageLimits() {
        return new NcPropertyBagsConverter<>(VoltageLimit::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.CGMES, OverridingObjectsFields.VOLTAGE_LIMIT, NcConstants.REQUEST_VOLTAGE_LIMIT));
    }

    public Set<VoltageAngleLimit> getVoltageAngleLimits() {
        return new NcPropertyBagsConverter<>(VoltageAngleLimit::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.EQUIPMENT_RELIABILITY, OverridingObjectsFields.VOLTAGE_ANGLE_LIMIT, NcConstants.REQUEST_VOLTAGE_ANGLE_LIMIT));
    }

    public Set<GridStateAlterationRemedialAction> getGridStateAlterationRemedialActions() {
        return new NcPropertyBagsConverter<>(GridStateAlterationRemedialAction::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.REMEDIAL_ACTION, OverridingObjectsFields.GRID_STATE_ALTERATION_REMEDIAL_ACTION, NcConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION));
    }

    public Set<TopologyAction> getTopologyActions() {
        return new NcPropertyBagsConverter<>(TopologyAction::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.REMEDIAL_ACTION, OverridingObjectsFields.TOPOLOGY_ACTION, NcConstants.TOPOLOGY_ACTION));
    }

    public Set<RotatingMachineAction> getRotatingMachineActions() {
        return new NcPropertyBagsConverter<>(RotatingMachineAction::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.REMEDIAL_ACTION, OverridingObjectsFields.ROTATING_MACHINE_ACTION, NcConstants.ROTATING_MACHINE_ACTION));
    }

    public Set<ShuntCompensatorModification> getShuntCompensatorModifications() {
        return new NcPropertyBagsConverter<>(ShuntCompensatorModification::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.REMEDIAL_ACTION, OverridingObjectsFields.SHUNT_COMPENSATOR_MODIFICATION, NcConstants.SHUNT_COMPENSATOR_MODIFICATION));
    }

    public Set<TapPositionAction> getTapPositionActions() {
        return new NcPropertyBagsConverter<>(TapPositionAction::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.REMEDIAL_ACTION, OverridingObjectsFields.TAP_POSITION_ACTION, NcConstants.TAP_POSITION_ACTION));
    }

    public Set<StaticPropertyRange> getStaticPropertyRanges() {
        return new NcPropertyBagsConverter<>(StaticPropertyRange::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.REMEDIAL_ACTION, OverridingObjectsFields.STATIC_PROPERTY_RANGE, NcConstants.STATIC_PROPERTY_RANGE));
    }

    public Set<ContingencyWithRemedialAction> getContingencyWithRemedialActions() {
        return new NcPropertyBagsConverter<>(ContingencyWithRemedialAction::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.REMEDIAL_ACTION, OverridingObjectsFields.CONTINGENCY_WITH_REMEDIAL_ACTION, NcConstants.REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION));
    }

    public Set<RemedialActionGroup> getRemedialActionGroups() {
        return new NcPropertyBagsConverter<>(RemedialActionGroup::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.REMEDIAL_ACTION, NcConstants.REQUEST_REMEDIAL_ACTION_GROUP));

    }

    public Set<RemedialActionDependency> getRemedialActionDependencies() {
        return new NcPropertyBagsConverter<>(RemedialActionDependency::fromPropertyBag)
            .convert(getPropertyBags(NcKeyword.REMEDIAL_ACTION, OverridingObjectsFields.SCHEME_REMEDIAL_ACTION_DEPENDENCY, NcConstants.REQUEST_REMEDIAL_ACTION_DEPENDENCY));
    }

    public Set<TapChanger> getTapChangers() {
        return new NcPropertyBagsConverter<>(TapChanger::fromPropertyBag).convert(getPropertyBags(NcKeyword.CGMES, NcConstants.REQUEST_TAP_CHANGER));
    }

    private void setOverridingData(OffsetDateTime importTimestamp) {
        overridingData = new HashMap<>();
        for (OverridingObjectsFields overridingObject : OverridingObjectsFields.values()) {
            addDataFromTripleStoreToMap(
                overridingData,
                overridingObject.getRequestName(),
                overridingObject.getObjectName(),
                overridingObject.getOverridedFieldName(),
                overridingObject.getHeaderType(),
                importTimestamp
            );
        }
    }

    private void addDataFromTripleStoreToMap(Map<String, String> dataMap,
                                             String queryName,
                                             String queryObjectName,
                                             String queryFieldName,
                                             HeaderType headerType,
                                             OffsetDateTime importTimestamp) {
        PropertyBags propertyBagsResult = queryTripleStore(queryName, tripleStoreNcCrac.contextNames());
        for (PropertyBag propertyBag : propertyBagsResult) {
            if (HeaderType.START_END_DATE.equals(headerType)) {
                if (NcCracUtils.checkProfileKeyword(propertyBag, NcKeyword.STEADY_STATE_INSTRUCTION) && NcCracUtils.checkProfileValidityInterval(propertyBag, importTimestamp)) {
                    String id = propertyBag.getId(queryObjectName);
                    String overridedValue = propertyBag.get(queryFieldName);
                    dataMap.put(id, overridedValue);
                }
            } else {
                if (NcCracUtils.checkProfileKeyword(propertyBag, NcKeyword.STEADY_STATE_HYPOTHESIS)) {
                    OffsetDateTime scenarioTime = OffsetDateTime.parse(propertyBag.get(NcConstants.SCENARIO_TIME));
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
        String query = queryCatalogNcCrac.get(queryKey);
        if (query == null) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("Query [{}] not found in catalog", queryKey);
            return new PropertyBags();
        }

        if (contexts.isEmpty()) {
            return tripleStoreNcCrac.query(query);
        }

        PropertyBags multiContextsPropertyBags = new PropertyBags();
        for (String context : contexts) {
            String contextQuery = String.format(query, context);
            multiContextsPropertyBags.addAll(tripleStoreNcCrac.query(contextQuery));
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
                    OpenRaoLoggerProvider.BUSINESS_WARNS.warn(String.format(
                        "[REMOVED] The file : %s will be ignored. Its dates are not consistent with the import date : %s",
                        contextName, offsetDateTime
                    ));
                    clearContext(contextName);
                    clearKeywordMap(contextName);
                }
            }
        });
    }

    private static boolean checkTimeCoherence(PropertyBag header, OffsetDateTime offsetDateTime) {
        String startTime = header.getId(NcConstants.REQUEST_HEADER_START_DATE);
        String endTime = header.getId(NcConstants.REQUEST_HEADER_END_DATE);
        return NcCracUtils.isValidInterval(offsetDateTime, startTime, endTime);
    }
}
