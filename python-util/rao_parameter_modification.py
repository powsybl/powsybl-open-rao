#  Copyright (c) 2024, RTE (http://www.rte-france.com)
#  This Source Code Form is subject to the terms of the Mozilla Public
#  License, v. 2.0. If a copy of the MPL was not distributed with this
#  file, You can obtain one at http://mozilla.org/MPL/2.0/.
#  SPDX-License-Identifier: MPL-2.0

import os
import json
import re
from json import JSONDecodeError

current_directory = os.getcwd()

relevant_rao_param_names = ["objective-function", "range-actions-optimization", "topological-actions-optimization",
                            "second-preventive-rao", "load-flow-and-sensitivity-computation", "multi-threading"]


def rao_parameters_file(file_path):
    # do not work with yaml yet
    correct_version = False
    has_rao_param_name = False
    if "target" not in file_path and (file_path.endswith(".json")):
        with open(os.path.join(dirpath, filename), 'r') as file:
            for line in file:
                if '"version" : "2.4"' in line or '"version" : "2.5"' in line:
                    correct_version = True
                if any(name in line for name in relevant_rao_param_names):
                    has_rao_param_name = True
                if correct_version and has_rao_param_name:
                    return True
    return False


# do not work with yaml yet
def read_data(file_path) -> dict:
    with open(file_path, 'r') as file:
        try:
            return json.load(file)
        except JSONDecodeError as je:
            print("in file " + file_path)
            raise je


class SpecialJSONEncoder(json.JSONEncoder):
    """A JSON Encoder closer to actual rao parameter json format"""

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.indentation_level = 0

    def encode(self, o):
        """Encode JSON object *o* with respect to single line lists."""

        if isinstance(o, (list, tuple)):
            if len(o) == 0:
                return "[ ]"
            else:
                return "[ " + ", ".join(self.encode(el) for el in o) + " ]"

        elif isinstance(o, dict):
            self.indentation_level += 1
            output = [self.indent_str + f"{json.dumps(k)} : {self.encode(v)}" for k, v in o.items()]
            self.indentation_level -= 1
            return "{\n" + ",\n".join(output) + "\n" + self.indent_str + "}"

        elif isinstance(o, float):
            pattern = r'(\d+).?(\d*)e-0(\d+)'

            def python_to_java_sci(match):
                return f"{match.group(1)}{'.0' if not any(match.group(2)) else ('.' + match.group(2))}E-{match.group(3)}"

            float_as_str = re.sub(pattern, python_to_java_sci, str(o))
            if float_as_str == "0.0001":
                return "1.0E-4"
            return float_as_str

        else:
            return json.dumps(o)

    @property
    def indent_str(self) -> str:
        return " " * self.indentation_level * self.indent

    def iterencode(self, o, **kwargs):
        """Required to also work with `json.dump`."""
        return self.encode(o)


def new_rao_param(data: dict, file_path: str) -> dict:
    try:
        move_to_extension(data, "objective-function", ["curative-min-obj-improvement"])
        move_to_extension(data, "range-actions-optimization",
                          ["max-mip-iterations", "pst-sensitivity-threshold", "pst-model",
                           "hvdc-sensitivity-threshold", "injection-ra-sensitivity-threshold",
                           "ra-range-shrinking", "linear-optimization-solver"])
        move_to_extension(data, "topological-actions-optimization",
                          ["max-preventive-search-tree-depth", "max-auto-search-tree-depth",
                           "max-curative-search-tree-depth", "predefined-combinations",
                           "skip-actions-far-from-most-limiting-element",
                           "max-number-of-boundaries-for-skipping-actions"])
        move_to_extension(data, "second-preventive-rao")
        move_to_extension(data, "load-flow-and-sensitivity-computation")
        if "range-actions-optimization" in data:
            new_names = {"pst-penalty-cost": "pst-ra-min-impact-threshold",
                         "hvdc-penalty-cost": "hvdc-ra-min-impact-threshold",
                         "injection-ra-penalty-cost": "injection-ra-min-impact-threshold"}
            data["range-actions-optimization"] = {new_names[k] if k in new_names else k: v for k, v in
                                                  data["range-actions-optimization"].items()}
        if "multi-threading" in data and any(data["multi-threading"]):
            data["multi-threading"] = {"available-cpus": max(v for k, v in data["multi-threading"].items() if k in ("contingency-scenarios-in-parallel", "preventive-leaves-in-parallel"))}
        move_to_extension(data, "multi-threading")
        if "extensions" in data:
            extensions = data["extensions"]
            # put extensions at the end:
            del data["extensions"]
            data["extensions"] = extensions
    except KeyError as ke:
        raise KeyError("in file " + file_path) from ke
    return data


def move_to_extension(data: dict, name_level1: str, names_level2: list | None = None):
    if name_level1 in data:
        param_level_1: dict = data[name_level1]
        if names_level2 is None:
            st_params = get_or_create_st_params(data)
            st_params[name_level1] = param_level_1
            del data[name_level1]
        else:
            if any(set(names_level2).intersection(param_level_1.keys())):
                st_params = get_or_create_st_params(data)
                if name_level1 not in st_params:
                    st_params[name_level1] = {}
                for name_level_2 in names_level2:
                    if name_level_2 in param_level_1:
                        st_params[name_level1][name_level_2] = param_level_1[name_level_2]
                        del param_level_1[name_level_2]
                if not any(param_level_1):
                    del data[name_level1]


def get_or_create_st_params(data: dict) -> dict:
    if "extensions" not in data:
        data["extensions"] = {}
    if "open-rao-search-tree-parameters" not in data["extensions"]:
        data["extensions"]["open-rao-search-tree-parameters"] = {}
    return data["extensions"]["open-rao-search-tree-parameters"]


def write_data():
    with open(file_path, 'w') as f:
        json.dump(new_rao_params, f, indent=2, separators=(',', ' : '), cls=SpecialJSONEncoder)


# do not work with yaml yet
if __name__ == "__main__":
    base_dir = os.path.join(current_directory, "..")
    print(base_dir)
    for dirpath, dirnames, filenames in os.walk(base_dir):
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)
            if rao_parameters_file(file_path):
                old_rao_param = read_data(file_path)
                new_rao_params = new_rao_param(old_rao_param, file_path)
                write_data()
