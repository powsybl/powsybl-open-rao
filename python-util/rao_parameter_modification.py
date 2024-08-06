#  Copyright (c) 2024, RTE (http://www.rte-france.com)
#  This Source Code Form is subject to the terms of the Mozilla Public
#  License, v. 2.0. If a copy of the MPL was not distributed with this
#  file, You can obtain one at http://mozilla.org/MPL/2.0/.
#  SPDX-License-Identifier: MPL-2.0

import os
import json
import re
from json import JSONDecodeError

# import yaml

current_directory = os.getcwd()


def rao_parameters_file(file_path):
    if "target" not in file_path and file_path.endswith(".json"): #or file_path.endswith(".yml"):
        with open(os.path.join(dirpath, filename), 'r') as file:
            for line in file:
                if "objective-function" in line:
                    return True
    return False

def read_data(file_path) -> dict:
    if file_path.endswith(".json"):
        with open(file_path, 'r') as file:
            try:
                return json.load(file)
            except JSONDecodeError as je:
                print("in file " + file_path)
                raise je
    #if file_path.endswith(".yml"):
    #    with open(file_path, 'r') as file:
    #        return yaml.safe_load(file)["rao-parameters"]

def extract_leading_whitespace(line):
    leading_whitespace = ""
    for char in line:
        if char.isspace():
            leading_whitespace += char
        else:
            break
    return leading_whitespace

def write_data(new_data, file_path):
    if file_path.endswith(".json"):
        with open(file_path, 'r') as file:
            lines = file.readlines()
            lines_to_write = []
            inside_obj_fun_to_replace = False
            for line in lines:
                if "objective-function" in line and "objective-function" in new_data:
                    leading_whitespace = extract_leading_whitespace(line)
                    inside_obj_fun_to_replace = True
                if inside_obj_fun_to_replace and "}" in line:
                    obj_fun_str = '"objective-function" : ' + json.dumps(new_data["objective-function"], indent=2, separators=(',', ' : ')) + ',\n'
                    for new_line in obj_fun_str.splitlines(True):
                        lines_to_write.append(leading_whitespace + new_line)
                    inside_obj_fun_to_replace = False
                elif not inside_obj_fun_to_replace:
                    lines_to_write.append(line)
        with open(file_path, 'w') as file:
            file.writelines(lines_to_write)
    #if file_path.endswith(".yml"):
    #    with open(file_path, 'r') as file:
    #        all_data = yaml.safe_load(file)
    #        all_data["rao-parameters"] = datagit diff -
    #        with open(file_path, 'w') as file:
    #            yaml.dump(all_data, file, default_flow_style=False)

def new_rao_param(data: dict, file_path: str) -> dict:
    try:
        old_obj_fun = data["objective-function"]
        new_obj_fun = {}
        if "preventive-stop-criterion" not in old_obj_fun or old_obj_fun["preventive-stop-criterion"] == "SECURE":
            new_obj_fun["type"] = "SECURE_FLOW"
            new_obj_fun["enforce-curative-security"] = old_obj_fun["optimize-curative-if-preventive-unsecure"]
        elif old_obj_fun["type"] in ("MAX_MIN_MARGIN_IN_MEGAWATT", "MAX_MIN_MARGIN_IN_AMPERE"):
            new_obj_fun["type"] = "MAX_MIN_FLOW_MARGIN"
        elif old_obj_fun["type"] in ("MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT", "MAX_MIN_RELATIVE_MARGIN_IN_AMPERE"):
            new_obj_fun["type"] = "MAX_MIN_RELATIVE_FLOW_MARGIN"
        if "curative-stop-criterion" in old_obj_fun:
            if "SECURE" in old_obj_fun["curative-stop-criterion"]:
                new_obj_fun["enforce-curative-security"] = True
            else:
                new_obj_fun["enforce-curative-security"] = False
        if "curative-min-obj-improvement" in old_obj_fun:
            new_obj_fun["curative-min-obj-improvement"] = old_obj_fun["curative-min-obj-improvement"]
    except KeyError as ke:
        raise KeyError("in file " + file_path) from ke
    data["objective-function"] = new_obj_fun
    return data



if __name__ == "__main__":
    base_dir = os.path.join(current_directory, "..")
    print(base_dir)
    for dirpath, dirnames, filenames in os.walk(base_dir):
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)
            if rao_parameters_file(file_path):
                data = read_data(file_path)
                new_rao_params = new_rao_param(data, file_path)
                write_data(new_rao_params, file_path)
