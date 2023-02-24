package com.farao_community.farao.search_tree_rao.castor.parameters;

import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import old.OldJsonRaoParameters;
import old.OldRaoParameters;

import java.io.*;
import java.nio.file.Path;

public class ConverterTest {

    public void scriptRaoParametersConversion() throws IOException {
        String inputPath = "/home/demontmorillongod/Workspace/FARAO/farao-core/ra-optimisation/rao-api/src/test/resources/";
        String outputPath = inputPath;
        String file = "RaoParametersWithLoopFlowError";

        String extension = ".json";
        String inputFile = inputPath + file + extension;
        String outputFile = outputPath + file + "_v2" + extension;
        new File(outputFile);
        InputStream is = new FileInputStream(inputFile);
        OldRaoParameters oldParameters = OldJsonRaoParameters.read(is);
        RaoParameters newRaoParameters = Converter.buildWithOldRaoParameters(oldParameters);
        JsonRaoParameters.write(newRaoParameters, Path.of(outputFile));

    }

    public void roundTripDefault() throws IOException {
        String inputPath = "/home/demontmorillongod/Workspace/FARAO/farao-core/ra-optimisation/rao-api/src/test/resources/";
        String outputPath = inputPath;
        String file = "RaoParameters";

        String extension = ".json";
        String inputFile = inputPath + file + extension;
        String outputFile = outputPath + file + "_v2" + extension;
        new File(outputFile);

        RaoParameters raoParameters = JsonRaoParameters.read(new FileInputStream(inputFile));
        JsonRaoParameters.write(raoParameters, Path.of(outputFile));

    }
}
