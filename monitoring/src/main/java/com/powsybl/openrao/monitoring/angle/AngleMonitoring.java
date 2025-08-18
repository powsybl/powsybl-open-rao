/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.action.Action;
import com.powsybl.action.DanglingLineAction;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.AbstractMonitoring;
import com.powsybl.openrao.monitoring.AppliedNetworkActionsResult;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.SecurityStatus;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleMonitoring extends AbstractMonitoring<AngleCnec> {
    public AngleMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        super(loadFlowProvider, loadFlowParameters);
    }

    @Override
    protected PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.ANGLE;
    }

    @Override
    protected Unit getUnit() {
        return Unit.DEGREE;
    }

    @Override
    protected MonitoringResult<AngleCnec> makeEmptySecureResult() {
        return new AngleMonitoringResult(Set.of(), Map.of(), SecurityStatus.SECURE);
    }

    @Override
    protected Set<AngleCnec> getCnecs(Crac crac) {
        return crac.getAngleCnecs();
    }

    @Override
    protected AppliedNetworkActionsResult applyNetworkActions(Network network, Set<NetworkAction> availableNetworkActions, String cnecId, MonitoringInput<AngleCnec> monitoringInput) {
        Set<RemedialAction<?>> appliedNetworkActions = new TreeSet<>(Comparator.comparing(RemedialAction::getId));
        boolean networkActionOk = false;
        EnumMap<Country, Double> powerToBeRedispatched = new EnumMap<>(Country.class);
        Set<String> networkElementsToBeExcluded = new HashSet<>();
        for (NetworkAction na : availableNetworkActions) {
            EnumMap<Country, Double> tempPowerToBeRedispatched = new EnumMap<>(powerToBeRedispatched);
            for (Action ea : na.getElementaryActions()) {
                networkActionOk = checkElementaryActionAndStoreInjection(ea, network, cnecId, na.getId(), networkElementsToBeExcluded, tempPowerToBeRedispatched, monitoringInput.getScalableZonalData());
                if (!networkActionOk) {
                    break;
                }
            }
            if (networkActionOk) {
                na.apply(network);
                appliedNetworkActions.add(na);
                powerToBeRedispatched.putAll(tempPowerToBeRedispatched);
            }
        }
        AppliedNetworkActionsResult appliedNetworkActionsResult = new AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder().withAppliedNetworkActions(appliedNetworkActions)
            .withNetworkElementsToBeExcluded(networkElementsToBeExcluded).withPowerToBeRedispatched(powerToBeRedispatched).build();
        BUSINESS_LOGS.info("Applied the following remedial action(s) in order to reduce constraints on CNEC \"{}\": {}", cnecId, appliedNetworkActions.stream().map(com.powsybl.openrao.data.crac.api.Identifiable::getId).collect(Collectors.joining(", ")));
        return appliedNetworkActionsResult;
    }

    @Override
    protected MonitoringResult<AngleCnec> makeMonitoringResult(Set<CnecResult<AngleCnec>> cnecResults, Map<State, Set<RemedialAction<?>>> appliedRemedialActions, SecurityStatus monitoringResultStatus) {
        return new AngleMonitoringResult(cnecResults, appliedRemedialActions, monitoringResultStatus);
    }

    @Override
    protected MonitoringResult<AngleCnec> makeFailedMonitoringResultForState(State state, String failureReason, Set<CnecResult<AngleCnec>> cnecResults) {
        BUSINESS_WARNS.warn(failureReason);
        return new AngleMonitoringResult(cnecResults, Map.of(state, Collections.emptySet()), SecurityStatus.FAILURE);
    }

    @Override
    protected CnecResult<AngleCnec> computeCnecResult(AngleCnec angleCnec, Network network, Unit unit) {
        AngleCnecHelper angleCnecHelper = new AngleCnecHelper();
        return new AngleCnecResult(angleCnec, unit, angleCnecHelper.computeValue(angleCnec, network, unit), angleCnecHelper.computeMargin(angleCnec, network, unit), angleCnecHelper.computeSecurityStatus(angleCnec, network, unit));
    }

    @Override
    protected CnecResult<AngleCnec> makeFailedCnecResult(AngleCnec angleCnec, Unit unit) {
        return new AngleCnecResult(angleCnec, unit, new AngleCnecValue(Double.NaN), Double.NaN, SecurityStatus.FAILURE);
    }

    /**
     * 1) Checks a network action's elementary action : it must be a Generator or a Load injection setpoint,
     * with a defined country.
     * 2) Stores applied injections on network
     * Returns false if network action must be filtered.
     */
    private boolean checkElementaryActionAndStoreInjection(Action ea, Network network, String angleCnecId, String naId, Set<String> networkElementsToBeExcluded, Map<Country, Double> powerToBeRedispatched, ZonalData<Scalable> scalableZonalData) {
        if (!(ea instanceof LoadAction) && !(ea instanceof GeneratorAction)) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that's not an injection setpoint.", naId, angleCnecId);
            return false;
        }
        Identifiable<?> ne = getInjectionSetpointIdentifiable(ea, network);

        if (ne == null) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has no elementary actions.", naId, angleCnecId);
            return false;
        }

        Optional<Substation> substation = ((Injection<?>) ne).getTerminal().getVoltageLevel().getSubstation();
        if (substation.isEmpty()) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that doesn't have a substation.", naId, angleCnecId);
            return false;
        } else {
            Optional<Country> country = substation.get().getCountry();
            if (country.isEmpty()) {
                BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that doesn't have a country.", naId, angleCnecId);
                return false;
            } else {
                checkGlsks(country.get(), naId, angleCnecId, scalableZonalData);
                if (ne.getType().equals(IdentifiableType.GENERATOR)) {
                    powerToBeRedispatched.merge(country.get(), ((Generator) ne).getTargetP() - ((GeneratorAction) ea).getActivePowerValue().getAsDouble(), Double::sum);
                } else if (ne.getType().equals(IdentifiableType.LOAD)) {
                    powerToBeRedispatched.merge(country.get(), -((Load) ne).getP0() + ((LoadAction) ea).getActivePowerValue().getAsDouble(), Double::sum);
                } else {
                    BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an injection setpoint that's neither a generator nor a load.", naId, angleCnecId);
                    return false;
                }
                networkElementsToBeExcluded.add(ne.getId());
            }
        }
        return true;
    }

    private Identifiable<?> getInjectionSetpointIdentifiable(Action ea, Network network) {
        if (ea instanceof GeneratorAction generatorAction) {
            return network.getIdentifiable(generatorAction.getGeneratorId());
        }
        if (ea instanceof LoadAction loadAction) {
            return network.getIdentifiable(loadAction.getLoadId());
        }
        if (ea instanceof DanglingLineAction danglingLineAction) {
            return network.getIdentifiable(danglingLineAction.getDanglingLineId());
        }
        if (ea instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction) {
            return network.getIdentifiable(shuntCompensatorPositionAction.getShuntCompensatorId());
        }
        return null;
    }

    /**
     * Checks glsks are correctly defined on country
     */
    private void checkGlsks(Country country, String naId, String angleCnecId, ZonalData<Scalable> scalableZonalData) {
        Set<Country> glskCountries = new TreeSet<>(Comparator.comparing(Country::getName));
        if (Objects.isNull(scalableZonalData)) {
            String error = "ScalableZonalData undefined (no GLSK given)";
            BUSINESS_LOGS.error(error);
            throw new OpenRaoException(error);
        }
        for (String zone : scalableZonalData.getDataPerZone().keySet()) {
            glskCountries.add(new CountryEICode(zone).getCountry());
        }
        if (!glskCountries.contains(country)) {
            throw new OpenRaoException(String.format("INFEASIBLE Angle Monitoring : Glsks were not defined for country %s. Remedial action %s of AngleCnec %s is ignored.", country.getName(), naId, angleCnecId));
        }
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with AngleMonitoringResult
     */
    public static RaoResult runAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, MonitoringInput<AngleCnec> monitoringInput) throws OpenRaoException {
        return new RaoResultWithAngleMonitoring(monitoringInput.getRaoResult(), new AngleMonitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel));
    }
}
