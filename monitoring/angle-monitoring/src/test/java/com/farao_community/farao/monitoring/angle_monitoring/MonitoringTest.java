package com.farao_community.farao.monitoring.angle_monitoring;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MonitoringTest {
    @Test
    public void test() {
        List<File> fileList = new ArrayList<>();
        listf("D:\\Workspace\\Powsybl\\powsybl-core", fileList);
        fileList.stream().filter(file -> file.toString().endsWith("iidm"))
                .forEach(file -> {
                    Network network = Importers.loadNetwork(file.getPath());

                    // Parcourir tous les voltages : regarder min != max
                    // loadflow, parcourir ceux là en appliquant toutes les parades, loadflow, regarder si ça a changé.
                });

    }

    public void listf(String directoryName, List<File> files) {
        File directory = new File(directoryName);

        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    listf(file.getAbsolutePath(), files);
                }

            }
        }
    }
}
