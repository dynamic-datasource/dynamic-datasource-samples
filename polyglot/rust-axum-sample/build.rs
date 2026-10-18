/*
 * Copyright © ${project.inceptionYear} organization baomidou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//! 链接 libdynamicds。没有用 DYNAMIC_DATASOURCE_LIB_DIR 指定现成的共享库时，先用
//! `../build-libdynamicds.sh` 构建它：共享库已是最新时脚本什么也不做，Java 源码或 pom 变化后
//! cargo 会重新执行本脚本。

use std::env;
use std::io;
use std::path::PathBuf;
use std::process::Command;

fn main() {
    println!("cargo::rerun-if-env-changed=DYNAMIC_DATASOURCE_LIB_DIR");
    let dir = match env::var_os("DYNAMIC_DATASOURCE_LIB_DIR") {
        Some(dir) => PathBuf::from(dir),
        None => build_library(),
    };
    let library = dir.join("libdynamicds.so");
    println!("cargo::rerun-if-changed={}", library.display());
    if !library.exists() {
        // 没有 GraalVM 时脚本只警告：cargo check / clippy 不链接，用不到共享库；cargo build / test 会在链接时失败
        println!(
            "cargo::warning={} 不存在：构建它需要 GraalVM 25（JAVA_HOME 或 GRAALVM_HOME 指向它），\
             也可以用 DYNAMIC_DATASOURCE_LIB_DIR 指向现成的 libdynamicds.so 所在的目录",
            library.display()
        );
    }
    let dir = dir.canonicalize().unwrap_or(dir);
    println!("cargo::rustc-link-search=native={}", dir.display());
    println!("cargo::rustc-link-lib=dylib=dynamicds");
    // 写入 rpath，`cargo run` / `cargo test` 不必再设置 LD_LIBRARY_PATH
    println!("cargo::rustc-link-arg=-Wl,-rpath,{}", dir.display());
}

/// 按需构建 `../dynamic-datasource-ffi` 里的 libdynamicds，返回它所在的目录。
fn build_library() -> PathBuf {
    let polyglot = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap()).join("..");
    let module = polyglot.join("dynamic-datasource-ffi");
    for input in [
        module.join("src/main"),
        module.join("pom.xml"),
        polyglot.join("pom.xml"),
        polyglot.join("../pom.xml"),
    ] {
        println!("cargo::rerun-if-changed={}", input.display());
    }
    let script = polyglot.join("build-libdynamicds.sh");
    println!("cargo::rerun-if-changed={}", script.display());
    // Maven 的输出转到 stderr：cargo 只在构建失败时显示它，stdout 留给 cargo:: 指令
    let status = Command::new("sh")
        .arg(script)
        .stdout(io::stderr())
        .status()
        .expect("无法执行 build-libdynamicds.sh");
    assert!(
        status.success(),
        "构建 libdynamicds 失败（{status}），见上面的 Maven 输出"
    );
    module.join("target")
}
