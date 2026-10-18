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
use rust_axum_sample::config::{load_config, sample_dir};
use rust_axum_sample::dynamic_datasource::DynamicDataSource;
use rust_axum_sample::routes;
use std::env;
use tokio::net::TcpListener;
use tokio::signal::unix::{SignalKind, signal};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let sample = sample_dir();
    let profile = env::var("DD_PROFILE").unwrap_or_else(|_| "h2".to_owned());
    let dd = DynamicDataSource::open(load_config(&sample, &profile)).await?;
    println!(
        "dynamic-datasource is open: {}",
        dd.list_datasources().await?
    );

    let port = env::var("PORT")
        .ok()
        .and_then(|port| port.parse().ok())
        .unwrap_or(8081);
    let listener = TcpListener::bind(("0.0.0.0", port)).await?;
    println!("rust-axum-sample listening on {}", listener.local_addr()?);
    axum::serve(listener, routes::router(dd, &sample.join("db")))
        .with_graceful_shutdown(shutdown_signal())
        .await?;

    dd.close().await?;
    Ok(())
}

async fn shutdown_signal() {
    let mut terminate = signal(SignalKind::terminate()).expect("install SIGTERM handler");
    tokio::select! {
        _ = tokio::signal::ctrl_c() => {}
        _ = terminate.recv() => {}
    }
}
