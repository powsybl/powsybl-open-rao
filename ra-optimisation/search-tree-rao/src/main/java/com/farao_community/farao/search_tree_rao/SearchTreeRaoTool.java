/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Crac;

import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
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
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@AutoService(Tool.class)
public class SearchTreeRaoTool implements Tool {

    private static final String CASE_FILE_OPTION = "case-file";
    private static final String CRAC_FILE_OPTION = "crac-file";
    private static final String OUTPUT_FILE_OPTION = "output-file";
    private static final String OUTPUT_FORMAT_OPTION = "output-format";

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "search-tree-rao";
            }

            @Override
            public String getTheme() {
                return "Computation";
            }

            @Override
            public String getDescription() {
                return "Run SearchTreeRao Computation";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(CASE_FILE_OPTION)
                        .desc("Network case path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(CRAC_FILE_OPTION)
                        .desc("CRAC file path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FILE_OPTION)
                        .desc("SearchTreeRao results output path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FORMAT_OPTION)
                        .desc("SearchTreeRao results output format")
                        .hasArg()
                        .argName("FORMAT")
                        .required()
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
        //Get Input
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue(CASE_FILE_OPTION));
        Path cracFile = context.getFileSystem().getPath(line.getOptionValue(CRAC_FILE_OPTION));
        Path outputFile = context.getFileSystem().getPath(line.getOptionValue(OUTPUT_FILE_OPTION));
        String format = line.getOptionValue(OUTPUT_FORMAT_OPTION);

        //Network
        context.getOutputStream().println("Loading network '" + caseFile + "'");
        Network network = Importers.loadNetwork(caseFile);
        String currentState = network.getVariantManager().getWorkingVariantId();

        //Crac for SearchTreeRao
        Crac crac = CracImporters.importCrac(cracFile);

        //Rao Parameter
        RaoParameters raoParameters = RaoParameters.load(PlatformConfig.defaultConfig());

        //Run
        ComputationManager computationManager = context.getLongTimeExecutionComputationManager();
        CompletableFuture<RaoComputationResult> raoComputationResult = new SearchTreeRao().run(network, crac, currentState, computationManager, raoParameters);
        SearchTreeRaoResult searchTreeRaoResult = raoComputationResult.get().getExtension(SearchTreeRaoResult.class);

        //Output
        context.getOutputStream().println("Writing results to '" + outputFile + "'");
        OutputStream outputStream = new FileOutputStream(String.valueOf(outputFile));
        SearchTreeRaoResultExporters.exportSearchTreeRaoResult(searchTreeRaoResult, format, outputStream);
    }
}
