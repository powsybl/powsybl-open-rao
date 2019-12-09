/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation;

import com.farao_community.farao.ra_optimisation.converter.RaoComputationResultExporters;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationParameters;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(Tool.class)
public class RaoComputationTool implements Tool {

    private static final String CASE_FILE_OPTION = "case-file";
    private static final String CRAC_FILE_OPTION = "crac-file";
    private static final String OUTPUT_FILE_OPTION = "output-file";
    private static final String OUTPUT_FORMAT_OPTION = "output-format";
    private static final String PARAMETERS_FILE = "parameters-file";
    private static final String SKIP_POSTPROC_OPTION = "skip-postproc";

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "ra-optimisation";
            }

            @Override
            public String getTheme() {
                return "Computation";
            }

            @Override
            public String getDescription() {
                return "Run modular RAO computation";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(CASE_FILE_OPTION)
                        .desc("the case path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(CRAC_FILE_OPTION)
                        .desc("the CRAC file path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(PARAMETERS_FILE)
                        .desc("the RAO parameters as JSON file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FILE_OPTION)
                        .desc("the RAO results output path")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FORMAT_OPTION)
                        .desc("the RAO results output format " + RaoComputationResultExporters.getFormats())
                        .hasArg()
                        .argName("FORMAT")
                        .build());
                options.addOption(Option.builder().longOpt(SKIP_POSTPROC_OPTION)
                        .desc("skip network importer post processors (when configured)")
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue(CASE_FILE_OPTION));
        Path cracFile = context.getFileSystem().getPath(line.getOptionValue(CRAC_FILE_OPTION));

        // Output file and output format
        Path outputFile = null;
        String format = null;
        if (line.hasOption(OUTPUT_FILE_OPTION)) {
            outputFile = context.getFileSystem().getPath(line.getOptionValue(OUTPUT_FILE_OPTION));
            if (!line.hasOption(OUTPUT_FORMAT_OPTION)) {
                throw new ParseException("Missing required option: " + OUTPUT_FORMAT_OPTION);
            }
            format = line.getOptionValue(OUTPUT_FORMAT_OPTION);
        }

        context.getOutputStream().println("Loading network '" + caseFile + "'");
        Network network = Importers.loadNetwork(caseFile);

        CracFile cracProvider = JsonCracFile.read(Files.newInputStream(cracFile));
        ComputationManager computationManager = context.getLongTimeExecutionComputationManager();

        RaoComputationParameters parameters = RaoComputationParameters.load();
        if (line.hasOption(PARAMETERS_FILE)) {
            Path parametersFile = context.getFileSystem().getPath(line.getOptionValue(PARAMETERS_FILE));
            JsonRaoComputationParameters.update(parameters, parametersFile);
        }

        RaoComputation raoComputation = ComponentDefaultConfig.load().newFactoryImpl(RaoComputationFactory.class)
                    .create(network, cracProvider, computationManager, 0);

        String currentState = network.getVariantManager().getWorkingVariantId();

        RaoComputationResult result = raoComputation.run(currentState, parameters).join();

        if (Objects.isNull(result)) {
            context.getErrorStream().println("Error occurred during computation, result not provided");
        } else {
            if (outputFile != null) {
                context.getOutputStream().println("Writing results to '" + outputFile + "'");
                RaoComputationResultExporters.export(result, outputFile, format);
            }
        }
    }
}
