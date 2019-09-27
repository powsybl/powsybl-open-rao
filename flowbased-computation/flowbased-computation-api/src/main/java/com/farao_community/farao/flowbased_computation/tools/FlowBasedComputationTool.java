/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.tools;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.farao_community.farao.data.flowbased_domain.json.JsonFlowbasedDomain;
import com.farao_community.farao.flowbased_computation.FlowBasedComputation;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.glsk_provider.CimGlskProvider;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.flowbased_computation.json.JsonFlowBasedComputationParameters;

import com.google.auto.service.AutoService;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * FlowBased Computation Tool
 * providing an example to use FlowBased computation API
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
@AutoService(Tool.class)
public class FlowBasedComputationTool implements Tool {

    private static final String CASE_FILE_OPTION = "case-file";
    private static final String CRAC_FILE_OPTION = "crac-file";
    private static final String GLSK_FILE_OPTION = "glsk-file";
    private static final String OUTPUT_FILE_OPTION = "output-file";
    private static final String PARAMETERS_FILE = "parameters-file";
    private static final String INSTANT = "instant";

    /**
     * @return Command to run a flow based computation
     */
    @Override
    public Command getCommand() {
        return new Command() {

            @Override
            public String getName() {
                return "flowbased-computation";
            }

            @Override
            public String getTheme() {
                return "Computation";
            }

            @Override
            public String getDescription() {
                return "Run modular FlowBased computation";
            }

            @Override
            public Options getOptions() {

                Options options = new Options();

                options.addOption(Option
                        .builder()
                        .longOpt(CASE_FILE_OPTION)
                        .desc("the case path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option
                        .builder()
                        .longOpt(CRAC_FILE_OPTION)
                        .desc("the CRAC file path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option
                        .builder()
                        .longOpt(GLSK_FILE_OPTION)
                        .desc("the CIM GlSK file path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option
                        .builder()
                        .longOpt(PARAMETERS_FILE)
                        .desc("the FlowBased computation parameters as JSON file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option
                        .builder()
                        .longOpt(INSTANT)
                        .desc("the instant of FlowBased computation")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option
                        .builder()
                        .longOpt(OUTPUT_FILE_OPTION)
                        .desc("the FlowBased computation results output path")
                        .hasArg()
                        .argName("FILE")
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    /**
     * @param line comman line
     * @param context running environment
     */
    @Override
    public void run(CommandLine line, ToolRunningContext context) throws IOException {
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue(CASE_FILE_OPTION));
        Path cracFile = context.getFileSystem().getPath(line.getOptionValue(CRAC_FILE_OPTION));
        Path glskFile = context.getFileSystem().getPath(line.getOptionValue(GLSK_FILE_OPTION));
        Path outputFile = null;
        if (line.hasOption(OUTPUT_FILE_OPTION)) {
            outputFile = context.getFileSystem().getPath(line.getOptionValue(OUTPUT_FILE_OPTION));
        }
        Instant instant = Instant.parse(line.getOptionValue(INSTANT));
        context.getOutputStream().println("Loading network '" + caseFile + "'");
        Network network = Importers.loadNetwork(caseFile);
        CracFile cracProvider = JsonCracFile.read(Files.newInputStream(cracFile));
        //TODO : handling also Ucte format
        GlskProvider cimGlskProvider = new CimGlskProvider(new FileInputStream(glskFile.toFile()), network, instant);
        ComputationManager computationManager = context.getLongTimeExecutionComputationManager();
        FlowBasedComputationParameters parameters = FlowBasedComputationParameters.load();
        if (line.hasOption(PARAMETERS_FILE)) {
            JsonFlowBasedComputationParameters.update(parameters, context.getFileSystem().getPath(line.getOptionValue(PARAMETERS_FILE)));
        }

        FlowBasedComputationResult result = FlowBasedComputation.run(network, cracProvider, cimGlskProvider, computationManager, parameters);
        if (outputFile != null) {
            JsonFlowbasedDomain.write(result.getFlowBasedDomain(), Files.newOutputStream(outputFile));
        }
    }
}
