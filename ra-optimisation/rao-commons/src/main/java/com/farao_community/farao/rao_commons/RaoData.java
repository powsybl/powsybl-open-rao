/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A RaoData is an object that gathers Network, Crac and SystematicSensitivityResult data. It manages
 * variants of these objects to ensure data consistency at any moment. Network will remain the same at any moment
 * with no variant management. It is a single point of entry to manipulate all data related to linear rao with
 * variant management.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoData {
    private final Network network;
    private final Crac crac;
    private final State optimizedState;
    private final Set<State> perimeter;
    private final ReferenceProgram referenceProgram;
    private final ZonalData<LinearGlsk> glsk;
    private final CracResultManager cracResultManager;
    private final Set<Country> loopflowCountries;

    private Set<BranchCnec> perimeterCnecs;
    private Set<BranchCnec> loopflowCnecs;

    private CracVariantManager cracVariantManager;

    /**
     * This constructor creates a new data variant with a pre-optimisation prefix and set it as the working variant.
     * So accessing data after this constructor will lead directly to the newly created variant data. CRAC and
     * sensitivity data will be empty. It will create a CRAC ResultVariantManager if it does not exist yet.
     *
     * @param network:           Network object.
     * @param crac:              CRAC object.
     * @param optimizedState:    State in which the remedial actions are optimized
     * @param perimeter:         set of State for which the Cnecs are monitored
     * @param referenceProgram:  ReferenceProgram object (needed only for loopflows and relative margin)
     * @param glsk:              GLSK provider (needed only for loopflows)
     * @param cracVariantId:     Existing variant of the CRAC on which RaoData will be based
     * @param loopflowCountries: countries for which we wish to check loopflows
     */
    public RaoData(Network network, Crac crac, State optimizedState, Set<State> perimeter, ReferenceProgram referenceProgram, ZonalData<LinearGlsk> glsk, String cracVariantId, Set<Country> loopflowCountries) {
        Objects.requireNonNull(network, "Unable to build RAO data without network.");
        Objects.requireNonNull(crac, "Unable to build RAO data without CRAC.");
        Objects.requireNonNull(optimizedState, "Unable to build RAO data without optimized state.");
        Objects.requireNonNull(crac, "Unable to build RAO data without perimeter.");
        this.network = network;
        this.crac = crac;
        this.optimizedState = optimizedState;
        this.perimeter = perimeter;
        this.referenceProgram = referenceProgram;
        this.glsk = glsk;
        this.loopflowCountries = loopflowCountries;
        cracResultManager = new CracResultManager(this);
        addRaoDataVariantManager(cracVariantId);

        computePerimeterCnecs();
        computeLoopflowCnecs();
    }

    private void addRaoDataVariantManager(String cracVariantId) {
        cracVariantManager = new CracVariantManager(crac, cracVariantId);
        cracResultManager.fillRangeActionResultsWithNetworkValues();
    }

    public static RaoData createOnPreventiveState(Network network, Crac crac) {
        return createOnPreventiveStateBasedOnExistingVariant(network, crac, null);
    }

    public static RaoData createOnPreventiveStateBasedOnExistingVariant(Network network, Crac crac, String cracVariantId) {
        return new RaoData(
                network,
                crac,
                crac.getPreventiveState(),
                Collections.singleton(crac.getPreventiveState()),
                null,
                null,
                cracVariantId,
                new HashSet<>());
    }

    public static RaoData create(Network network, RaoData raoData) {
        return new RaoData(
                network,
                raoData.getCrac(),
                raoData.getOptimizedState(),
                raoData.getPerimeter(),
                raoData.getReferenceProgram(),
                raoData.getGlskProvider(),
                null,
                raoData.getLoopflowCountries());
    }

    public Network getNetwork() {
        return network;
    }

    public Crac getCrac() {
        return crac;
    }

    public ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public ZonalData<LinearGlsk> getGlskProvider() {
        return glsk;
    }

    public Set<Country> getLoopflowCountries() {
        return loopflowCountries;
    }

    public Set<BranchCnec> getCnecs() {
        return perimeterCnecs;
    }

    private void computePerimeterCnecs() {
        if (perimeter != null) {
            Set<BranchCnec> cnecs = new HashSet<>();
            perimeter.forEach(state -> cnecs.addAll(crac.getBranchCnecs(state)));
            perimeterCnecs = cnecs;
        } else {
            perimeterCnecs = crac.getBranchCnecs();
        }
    }

    public Set<BranchCnec> getLoopflowCnecs() {
        return loopflowCnecs;
    }

    private void computeLoopflowCnecs() {
        if (!loopflowCountries.isEmpty()) {
            loopflowCnecs = perimeterCnecs.stream()
                    .filter(cnec -> !Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class)) && cnecIsInCountryList(cnec, network, loopflowCountries))
                    .collect(Collectors.toSet());
        } else {
            loopflowCnecs = perimeterCnecs.stream()
                    .filter(cnec -> !Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class)))
                    .collect(Collectors.toSet());
        }
    }

    private static boolean cnecIsInCountryList(Cnec<?> cnec, Network network, Set<Country> loopflowCountries) {
        Line line = (Line) network.getIdentifiable(cnec.getNetworkElement().getId());
        Optional<Country> country1 = line.getTerminal1().getVoltageLevel().getSubstation().getCountry();
        Optional<Country> country2 = line.getTerminal2().getVoltageLevel().getSubstation().getCountry();
        return (country1.isPresent() && loopflowCountries.contains(country1.get())) || (country2.isPresent() && loopflowCountries.contains(country2.get()));
    }

    public Set<RangeAction> getAvailableRangeActions() {
        return crac.getRangeActions(network, optimizedState, UsageMethod.AVAILABLE);
    }

    public Set<NetworkAction> getAvailableNetworkActions() {
        return crac.getNetworkActions(network, optimizedState, UsageMethod.AVAILABLE);
    }

    public State getOptimizedState() {
        return optimizedState;
    }

    public Set<State> getPerimeter() {
        return perimeter;
    }

    public CracResultManager getCracResultManager() {
        return cracResultManager;
    }

    public CracVariantManager getCracVariantManager() {
        return cracVariantManager;
    }

    // Delegate methods of RaoDataVariantManager
    public String getWorkingVariantId() {
        return getCracVariantManager().getWorkingVariantId();
    }

    public String getInitialVariantId() {
        return getCracVariantManager().getInitialVariantId();
    }

    public CracResult getCracResult(String variantId) {
        return getCracVariantManager().getCracResult(variantId);
    }

    public CracResult getCracResult() {
        return getCracVariantManager().getCracResult();
    }

    public SystematicSensitivityResult getSystematicSensitivityResult() {
        return getCracVariantManager().getSystematicSensitivityResult();
    }

    public void setSystematicSensitivityResult(SystematicSensitivityResult systematicSensitivityResult) {
        getCracVariantManager().setSystematicSensitivityResult(systematicSensitivityResult);
    }

    public boolean hasSensitivityValues() {
        return getSystematicSensitivityResult() != null;
    }

    public double getReferenceFlow(Cnec<?> cnec) {
        return getSystematicSensitivityResult().getReferenceFlow(cnec);
    }

    public double getSensitivity(Cnec<?> cnec, RangeAction rangeAction) {
        return getSystematicSensitivityResult().getSensitivityOnFlow(rangeAction, cnec);
    }
}
