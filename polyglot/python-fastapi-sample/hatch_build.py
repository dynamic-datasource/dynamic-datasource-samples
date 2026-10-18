#
# Copyright © ${project.inceptionYear} organization baomidou
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""安装本项目时（包括 uv sync、uv run 自动完成的可编辑安装）按需构建 libdynamicds。

共享库已是最新时 ``build-libdynamicds.sh`` 什么也不做；用 DYNAMIC_DATASOURCE_LIB 指定了现成的共享库时不构建。
"""

from __future__ import annotations

import os
import subprocess
from pathlib import Path
from typing import Any

from hatchling.builders.hooks.plugin.interface import BuildHookInterface


class LibDynamicDsBuildHook(BuildHookInterface):
    def initialize(self, version: str, build_data: dict[str, Any]) -> None:
        if os.environ.get("DYNAMIC_DATASOURCE_LIB"):
            return
        script = Path(self.root).parent / "build-libdynamicds.sh"
        subprocess.run(["sh", str(script)], check=True)
