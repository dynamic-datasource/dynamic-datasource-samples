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

"""各路由共用的 DynamicDataSource：进程内只有一个，dd_open / dd_close 由 main.py 的 lifespan 负责。"""

from __future__ import annotations

from app.config import LIBRARY
from app.dynamic_datasource import DynamicDataSource

if not LIBRARY.exists():
    raise RuntimeError(
        f"{LIBRARY} 不存在：安装本项目（uv sync / uv run）时会用 GraalVM 构建它，请安装 GraalVM 25 并让 JAVA_HOME "
        "或 GRAALVM_HOME 指向它；或用 DYNAMIC_DATASOURCE_LIB 指向 libdynamicds.so"
    )

dd = DynamicDataSource(LIBRARY)
