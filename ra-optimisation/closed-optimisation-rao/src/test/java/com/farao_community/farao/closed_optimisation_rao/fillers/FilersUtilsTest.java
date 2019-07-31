package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.JsonClosedOptimisationRaoResultTest;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStream;
import java.util.HashMap;

class FilersUtilsTest {

    public HashMap<String, Object> getData() {
        HashMap<String, Object> data = new HashMap<>();
        HashMap<String, Double> pstSensitivities = new HashMap<>();
        HashMap<Pair<String, String>, Double> generatorSensitivities = new HashMap<>();
        HashMap<String, Double> referenceFlows = new HashMap<>();

        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_BE_1.1"), 0.3786);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_BE_1.2"), 0.3786);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_FR_1"), -0.4348);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_FR_2"), -0.4348);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE_1.1"), 0.2899);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE_1.2"), 0.2899);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR_1"), 0.5652);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR_2"), 0.5652);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE_1.1"), -0.1449);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE_1.2"), -0.1449);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR_1"), 0.1884);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR_2"), 0.1884);

        referenceFlows.put("MONITORED_FRANCE_BELGIUM_1", -266.667);
        referenceFlows.put("MONITORED_FRANCE_BELGIUM_2", -533.333);
        referenceFlows.put("C1_MONITORED_FRANCE_BELGIUM_1", -800.000);

        data.put("pst_branch_sensitivities", pstSensitivities);
        data.put("generator_branch_sensitivities", generatorSensitivities);
        data.put("reference_flows", referenceFlows);

        return data;
    }
}
