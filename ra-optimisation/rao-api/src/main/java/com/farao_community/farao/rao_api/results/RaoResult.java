/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.results;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface RaoResult {

    PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state);

    /* Est-ce qu'on garde les setters par CNEC ? Alternative : passez les map construites dans le constructeur (à priori pas optimal car gros constructeur),
    construire le RaoResult à partir d'un builder et rajouter map par map ce qui nous intéresse, une méthode setStateResult(SearchTreeResult)
    qui appele les différents setter individuels
     */

    default double getFlow(OptimizationState raoState, BranchCnec branchCnec, Unit unit) {
        return getPerimeterResult(raoState, branchCnec.getState()).getFlow(branchCnec, unit);
    }

    default double getMargin(OptimizationState raoState, BranchCnec branchCnec, Unit unit) {
        return getPerimeterResult(raoState, branchCnec.getState()).getMargin(branchCnec, unit);
    }

    /* A voir si on veut récupérer la marge relative quoi qu'il arrive ou la marge au sens de la fct obj donc
    relatif quand c'est positif et absolu quand c'est negatif
    2 vote pour obj func
    1 vote full relatif
     */
    default double getRelativeMargin(OptimizationState raoState, BranchCnec branchCnec, Unit unit) {
        return getPerimeterResult(raoState, branchCnec.getState()).getRelativeMargin(branchCnec, unit);
    }

    default double getLoopFlow(OptimizationState raoState, BranchCnec branchCnec, Unit unit) {
        return getPerimeterResult(raoState, branchCnec.getState()).getLoopFlow(branchCnec, unit);
    }

    default double getPtdfZonalSum(OptimizationState raoState, BranchCnec branchCnec) {
        return getPerimeterResult(raoState, branchCnec.getState()).getPtdfZonalSum(branchCnec);
    }

    double getCost(OptimizationState state);

    double getFunctionalCost(OptimizationState raoState);

    // A voir dans l'implem si on stocke ou non
    List<BranchCnec> getMostLimitingElements(OptimizationState raoState, int number);

    double getVirtualCost(OptimizationState raoState);

    // Peut-être enum plutôt que string
    Set<String> getVirtualCostNames();

    double getVirtualCost(OptimizationState raoState, String virtualCostName);

    /* Ça marche pas pour le sensitivity fallback
    sans le name pour avoir tous les virtual cost, ou même avoir tous les costly elements d'un coup
     */
    List<BranchCnec> getCostlyElements(OptimizationState raoState, String virtualCostName, int number);

    // En relatif par rapport à l'état d'avant
    boolean isActivatedAtOrBeforeState(State state, NetworkAction networkAction);

    boolean isActivatedDuringState(State state, NetworkAction networkAction);

    // Set de isActivatedDuringState
    Set<NetworkAction> getActivatedNetworkActions(State state);

    // On peut simplifier dans un premier temps
    boolean isActivatedAtOrBeforeState(State state, RangeAction rangeAction);

    /*En comparaison avec l'état précédent
    // Si state preventif on compare avant et après optim
    // Si state curatif on compare optim du preventif et optim du curatif
    // Si la range action est pas available sur le state c'est automatiquement false
    // Sinon getOptimizedTap() - getPreStateTap() != 0*/
    boolean isActivatedDuringState(State state, RangeAction rangeAction);

    /*Si preventif on retourne le initial, si curatif on retourne le preventif post pra
    // On retourne celle du PST*/
    int getPreStateTap(State state, RangeAction rangeAction);

    // Si preventif on retourne le preventif post pra, si curatif on retourne le curatif post cra
    int getOptimizedTap(State state, PstRangeAction pstRangeAction);

    double getPreStateSetPoint(RangeAction rangeAction);

    double getOptimizedSetPoint(RangeAction rangeAction);

    // Set de isActivatedDuringState
    Set<RangeAction> getActivatedRangeActions(State state);

    // Map<RangeAction, Integer> getPreStateTaps(State state);

    Map<RangeAction, Integer> getOptimizedTaps(State state);

    Map<RangeAction, Double> getOptimizedSetPoints(State state);

    // Faire un exemple complet des PST

    // Note pour l'export Json : "pst1" : {"initial": 0, "preventif": 5 (si il a changé uniquement), "curatif-1": 12 (si il a changé du préventif), "curatif-2": 12 (si il a changé du préventif)}
}
