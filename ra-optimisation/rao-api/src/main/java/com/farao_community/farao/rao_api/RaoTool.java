/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.rao_api.json.JsonRaoResult;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;


/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@AutoService(Tool.class)
public class RaoTool implements Tool {
    private static final String OUTPUT_FILE_OPTION = "output-file";
    private static final String CRAC_FILE_OPTION = "crac-file";
    private static final String CASE_FILE_OPTION = "case-file";
    private static final String OUTPUT_FORMAT_OPTION = "output-format";

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "rao";
            }

            @Override
            public String getTheme() {
                return "Computation";
            }

            @Override
            public String getDescription() {
                return "Run a RAO computation";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder()
                        .longOpt(CASE_FILE_OPTION)
                        .desc("Network file")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder()
                        .longOpt(CRAC_FILE_OPTION)
                        .desc("Crac file")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder()
                        .longOpt(OUTPUT_FILE_OPTION)
                        .desc("Rao results output file")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder()
                        .longOpt(OUTPUT_FORMAT_OPTION)
                        .desc("Rao results output format")
                        .hasArg()
                        .argName("FORMAT")
                        .required()
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return "Rao computation returns RaoComputationResult using the Rao implementation given in the config";
            }
        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        //Get Input
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue(CASE_FILE_OPTION));
        Path cracFile = context.getFileSystem().getPath(line.getOptionValue(CRAC_FILE_OPTION));
        Path outputFile = context.getFileSystem().getPath(line.getOptionValue(OUTPUT_FILE_OPTION));

        //Network
        context.getOutputStream().println("Loading network '" + caseFile + "'");
        Network network = Importers.loadNetwork(caseFile);
        String currentState = network.getVariantManager().getWorkingVariantId();

        //Crac
        context.getOutputStream().println("Importing crac '" + cracFile + "'");
        Crac crac = CracImporters.importCrac(cracFile);

        //Rao Parameter
        context.getOutputStream().println("Loading RAO parameters");
        RaoParameters raoParameters = RaoParameters.load(PlatformConfig.defaultConfig());

        //Run
        ComputationManager computationManager = context.getLongTimeExecutionComputationManager();
        context.getOutputStream().println("Running Rao computation");
        RaoResult raoResult = Rao.run(network, crac, currentState, computationManager, raoParameters);

        //Output
        context.getOutputStream().println("Writing results to '" + outputFile + "'");
        OutputStream outputStream = new FileOutputStream(String.valueOf(outputFile));
        JsonRaoResult.write(raoResult, outputStream);
    }
}
