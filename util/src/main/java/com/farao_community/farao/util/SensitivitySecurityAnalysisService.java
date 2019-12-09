package com.farao_community.farao.util;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La sortie doit comporter :
 * * pour chaque CNEC du Crac : les flux de référence
 * * pour chaque couple CNEC x RangeAction du Crac : la sensibilité de la RangeAction sur le flux de la CNEC.
 *
 * Donc en gros la "fonction run" du SensitivitySecurityAnalysisService doit :
 * Regrouper les CNECs par contingency :
 * Pour chaque contingency,
 * Appliquer la contingency (ou ne rien faire si contingency == null)
 * Extraire du Crac (/!\ pas un cracFile), un SensitivityFactorsProvider
 * Lancer un calcul de sensitibilité avec le SensitivityComputationService
 * Aggreger les sorties dans un SensitivitySecurityAnalysisResult
 */
public final class SensitivitySecurityAnalysisService {

    private SensitivitySecurityAnalysisService() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static SensitivitySecurityAnalysisResult runSensitivity(Network network,
                                                               String workingStateId,
                                                               Crac crac) {
        List<Contingency> contingencies = crac.getContingencies();
        Map<Contingency, SensitivityComputationResults> contingencySensitivityComputationResultsMap = new HashMap<>();
        for (Contingency contingency : contingencies) {
            //apply the contingency on network
            applyContingency(network, contingency, workingStateId); //TODO how to apply contingency on network?

            //get Sensitivity factor from this network applied with contingency;
            SensitivityFactorsProvider factorsProvider = net -> generateSensitivityFactorsProvider(net, crac);
            SensitivityComputationResults sensiResults = SensitivityComputationService.runSensitivity(network, workingStateId, factorsProvider);
            contingencySensitivityComputationResultsMap.put(contingency, sensiResults);
        }

        return new SensitivitySecurityAnalysisResult(contingencySensitivityComputationResultsMap);
    }

    private static List<SensitivityFactor> generateSensitivityFactorsProvider(Network net, Crac crac) {
        return null; //TODO
    }

    private static void applyContingency(Network network, Contingency contingency, String workingStateId) {
        return; //TODO
    }
}
