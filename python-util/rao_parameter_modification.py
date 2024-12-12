#  Copyright (c) 2024, RTE (http://www.rte-france.com)
#  This Source Code Form is subject to the terms of the Mozilla Public
#  License, v. 2.0. If a copy of the MPL was not distributed with this
#  file, You can obtain one at http://mozilla.org/MPL/2.0/.
#  SPDX-License-Identifier: MPL-2.0

import os
import json

root_directory = os.path.join(os.getcwd(), "..")


def rao_parameters_file(file_path):
    correct_version = False
    obj_fun = False
    if file_path.endswith(".json") or file_path.endswith(".yml"):
        with open(os.path.join(dirpath, filename), 'r') as file:
            for line in file:
                if "objective-function" in line:
                    obj_fun = True
                if '"version" : "2.4"' in line or '"version" : "2.5"' in line:
                    correct_version = True
                if correct_version and obj_fun:
                    return True
    return False


for dirpath, dirnames, filenames in os.walk(root_directory):
    for filename in filenames:
        file_path = os.path.join(dirpath, filename)
        if rao_parameters_file(file_path):
            print("file to change : " + file_path)
            lines = None
            if file_path.endswith(".json"):
                with open(file_path, 'r') as file:
                    lines = file.readlines()
                    lines = [line.replace('"version" : "2.4"', '"version" : "2.5"') for line in lines if "forbid-cost-increase" not in line]
            if lines is not None:
                with open(file_path, 'w') as file:
                    file.writelines(lines)
