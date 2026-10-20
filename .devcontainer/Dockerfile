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

FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

# native-image 链接 libdynamicds 时需要 gcc 与 zlib 的头文件
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

# 与 polyglot/Dockerfile 相同的 uv；Python 由 uv 按各项目的 .python-version 下载
COPY --from=ghcr.io/astral-sh/uv:0.12 /uv /uvx /usr/local/bin/

# uv 的虚拟环境放在工作区之外（见 devcontainer.json 的 UV_PROJECT_ENVIRONMENT）：虚拟环境里的 python
# 是指向解释器安装路径的符号链接，容器与宿主机共用工作区里的 .venv 时，一方创建的另一方用不了。
# 目录对所有用户可写，因为 remoteUser 可能是 root，也可能是 uid 被改成与宿主机一致的 vscode。
RUN install -d -m 1777 /opt/venvs
