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

"""本示例自带的数据源配置与 SQL 脚本：项目根目录下的 config/ 与 db/。"""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any

_SOURCE_DIR = Path(__file__).resolve().parents[1]

# 存放 config/ 与 db/ 的目录：默认是本项目的根目录，也可以用 SAMPLE_DIR 指定
SAMPLE_DIR = Path(os.environ.get("SAMPLE_DIR") or _SOURCE_DIR).resolve()
DB_DIR = SAMPLE_DIR / "db"
# 默认用 ../dynamic-datasource-ffi 构建出的共享库
LIBRARY = Path(
    os.environ.get("DYNAMIC_DATASOURCE_LIB")
    or _SOURCE_DIR.parent / "dynamic-datasource-ffi" / "target" / "libdynamicds.so"
)


def load_config(profile: str | None = None, variables: dict[str, str] | None = None) -> dict[str, Any]:
    """读取 config/<profile>.json（默认取 DD_PROFILE，再默认 h2），并通过 variables 告诉 dd_open 建表脚本的位置。"""
    profile = profile or os.environ.get("DD_PROFILE", "h2")
    config = json.loads((SAMPLE_DIR / "config" / f"{profile}.json").read_text(encoding="utf-8"))
    config["variables"] = {"DB_DIR": str(DB_DIR), **(variables or {})}
    return config
