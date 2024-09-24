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
    # do not work with yaml yet
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
                if "MAX_MIN_MARGIN" in line or "MAX_MIN_RELATIVE_MARGIN" in line:
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
            return f"{o}".replace("e-0", "E-").replace("1E", "1.0E").replace("0.0001", "1.0E-4")  # dangerous hack

        else:
            return json.dumps(o)


    @property
    def indent_str(self) -> str:
        return " " * self.indentation_level * self.indent

    def iterencode(self, o, **kwargs):
        """Required to also work with `json.dump`."""
        return self.encode(o)



def new_rao_param(data: dict, file_path: str, file_type: str) -> dict:
    try:
        move_to_extension(data, tag_by_file_type[file_type], ["curative-min-obj-improvement"])
        move_to_extension(data, "range-actions-optimization", ["linear-optimization-solver", "max-mip-iterations", "pst-sensitivity-threshold", "hvdc-penalty-cost", "injection-ra-sensitivity-threshold", "pst-model", "ra-range-shrinking"])
        move_to_extension(data, "topological-actions-optimization", ["max-preventive-search-tree-depth", "max-auto-search-tree-depth", "max-curative-search-tree-depth", "predefined-combinations", "skip-actions-far-from-most-limiting-element", "max-number-of-boundaries-for-skipping-actions"])
        move_to_extension(data, "second-preventive-rao")
        move_to_extension(data, "load-flow-and-sensitivity-computation", ["load-flow-provider", "sensitivity-provider", "sensitivity-failure-overcost", "	sensitivity-parameters"])
        if "range-actions-optimization" in data:
            new_names = {"pst-penalty-cost": "pst-ra-min-impact-threshold", "hvdc-penalty-cost": "hvdc-ra-min-impact-threshold", "injection-ra-penalty-cost": "injection-ra-min-impact-threshold"}
            data["range-actions-optimization"] = {new_names[k] if k in new_names else k: v for k, v in data["range-actions-optimization"].items()}
        if "multi-threading" in data and any(data["multi-threading"]):
            data["multi-threading"] = {"available-cpus": max(v for k, v in data["multi-threading"].items())}
        # TODO : move from extensions to extensions
    except KeyError as ke:
        raise KeyError("in file " + file_path) from ke
    return data


def move_to_extension(data: dict, name_level1: str, names_level2 : list | None = None):
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
    if "extensions" in data:
        if "open-rao-search-tree-parameters" not in data["extensions"]:
            data["extensions"]["open-rao-search-tree-parameters"] = {}
    else:
        data["extensions"] = {"open-rao-search-tree-parameters": {}}
    return data["extensions"]["open-rao-search-tree-parameters"]


if __name__ == "__main__":
    base_dir = os.path.join(current_directory, "..")
    print(base_dir)
    for dirpath, dirnames, filenames in os.walk(base_dir):
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)
            if rao_parameters_file(file_path):
                old_rao_param, file_type = read_data(file_path)
                new_rao_params = new_rao_param(old_rao_param, file_path, file_type)
                with open(file_path, 'w') as f:
                    json.dump(new_rao_params, f, indent=2, separators=(',', ' : '), cls=SpecialJSONEncoder)
