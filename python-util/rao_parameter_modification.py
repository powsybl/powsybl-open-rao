#  Copyright (c) 2024, RTE (http://www.rte-france.com)
#  This Source Code Form is subject to the terms of the Mozilla Public
#  License, v. 2.0. If a copy of the MPL was not distributed with this
#  file, You can obtain one at http://mozilla.org/MPL/2.0/.
#  SPDX-License-Identifier: MPL-2.0

import os
import json
from json import JSONDecodeError
import yaml

current_directory = os.getcwd()


tag_by_file_type = {"json": "objective-function", "yaml": "rao-objective-function"}

def rao_parameters_file(file_path):
    correct_version = False
    score = 0
    if "target" not in file_path and (file_path.endswith(".json") or file_path.endswith(".yml")):
        ftype = "json" if file_path.endswith(".json") else "yaml"
        with open(os.path.join(dirpath, filename), 'r') as file:
            for line in file:
                if '"version" : "2.4"' in line or '"version" : "2.5"' in line:
                    correct_version = True
                if tag_by_file_type[ftype] in line:
                    score += 1
                if "MAX_MIN_MARGIN_IN" in line or "MAX_MIN_RELATIVE_MARGIN_IN" in line:
                    score += 1
                if correct_version and score >= 2:
                    return True
    return False

def read_data(file_path) -> tuple[dict, str]:
    if file_path.endswith(".json"):
        with open(file_path, 'r') as file:
            try:
                return json.load(file), "json"
            except JSONDecodeError as je:
                print("in file " + file_path)
                raise je
    if file_path.endswith(".yml"):
        with open(file_path, 'r') as file:
            return yaml.safe_load(file), "yaml"  # ["rao-parameters"]

def extract_leading_whitespace(line):
    leading_whitespace = ""
    for char in line:
        if char.isspace():
            leading_whitespace += char
        else:
            break
    return leading_whitespace

def write_data(new_data, file_path, file_type):
    with open(file_path, 'r') as file:
        lines = file.readlines()
        lines_to_write = []
        inside_obj_fun_to_replace = False
        for line in lines:
            if tag_by_file_type[file_type] in line and tag_by_file_type[file_type] in new_data:
                leading_whitespace = extract_leading_whitespace(line)
                inside_obj_fun_to_replace = True
            if inside_obj_fun_to_replace and ((file_type == "json" and "}" in line) or (file_type == "yaml" and line == "\n")):
                obj_fun_str = f'"{tag_by_file_type[file_type]}" : ' + obj_function_as_str_lines(new_data, file_type)
                for new_line in obj_fun_str.splitlines(True):
                    lines_to_write.append(leading_whitespace + new_line)
                inside_obj_fun_to_replace = False
            elif not inside_obj_fun_to_replace:
                lines_to_write.append(line)
    with open(file_path, 'w') as file:
        file.writelines(lines_to_write)


def obj_function_as_str_lines(new_data, file_type):
    if file_type == "json":
        return json.dumps(new_data[tag_by_file_type[file_type]], indent=2, separators=(',', ' : ')) + ',\n'
    else:
        return yaml.dump(new_data[tag_by_file_type[file_type]], default_flow_style=False) + '\n'



def new_rao_param(data: dict, file_path: str, file_type: str) -> dict:
    try:
        old_obj_fun = data[tag_by_file_type[file_type]]
        new_obj_fun = {}
        for key in old_obj_fun:
            if key not in ("curative-stop-criterion", "optimize-curative-if-preventive-unsecure"):
                new_obj_fun[key] = old_obj_fun[key]
        prev_secure = "preventive-stop-criterion" not in old_obj_fun or old_obj_fun["preventive-stop-criterion"] == "SECURE"
        if prev_secure:
            if "optimize-curative-if-preventive-unsecure" in old_obj_fun:
                new_obj_fun["enforce-curative-security"] = old_obj_fun["optimize-curative-if-preventive-unsecure"]
        else:
            cur_secure = "curative-stop-criterion" in old_obj_fun and old_obj_fun["curative-stop-criterion"] in ("SECURE", "PREVENTIVE_OBJECTIVE_AND_SECURE")
            if cur_secure:
                new_obj_fun["enforce-curative-security"] = True
            else:
                new_obj_fun["enforce-curative-security"] = False
                cur_min = "curative-stop-criterion" in old_obj_fun and old_obj_fun["curative-stop-criterion"] == "MIN_OBJECTIVE"
                if cur_min:
                    new_obj_fun["curative-min-obj-improvement"] = 10000.0 + (old_obj_fun["curative-min-obj-improvement"] if "curative-min-obj-improvement" in old_obj_fun else 0.0)
    except KeyError as ke:
        raise KeyError("in file " + file_path) from ke
    data[tag_by_file_type[file_type]] = new_obj_fun
    return data



if __name__ == "__main__":
    base_dir = os.path.join(current_directory, "..")
    print(base_dir)
    for dirpath, dirnames, filenames in os.walk(base_dir):
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)
            if rao_parameters_file(file_path):
                data, file_type = read_data(file_path)
                new_rao_params = new_rao_param(data, file_path, file_type)
                write_data(new_rao_params, file_path, file_type)
