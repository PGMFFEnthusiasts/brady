use axum::{
    Json, Router,
    body::Bytes,
    extract::{Path, State},
    http::StatusCode,
    response::{IntoResponse, Response},
    routing::{get, post},
};
use fjall::{Keyspace, PartitionHandle, PersistMode};
use nanoid::nanoid;
use serde::Serialize;
use tracing::{info, level_filters::LevelFilter};
use tracing_subscriber::{
    EnvFilter,
    fmt::{self},
    layer::SubscriberExt,
    util::SubscriberInitExt as _,
};

#[tokio::main]
async fn main() -> Result<(), anyhow::Error> {
    dotenvy::dotenv()?;
    let env_filter = EnvFilter::builder()
        .with_default_directive(LevelFilter::INFO.into())
        .from_env_lossy();

    tracing_subscriber::registry()
        .with(fmt::layer())
        .with(env_filter)
        .init();

    let path = std::path::Path::new("data");
    let keyspace = fjall::Config::new(path).open()?;
    let db = keyspace.open_partition("data", Default::default())?;
    let app = App { keyspace, db };

    let router = Router::new()
        .route("/post", post(create_handler))
        .route("/{id}/raw", get(get_raw_handler))
        .with_state(app);

    info!("about to begin listening on :3000");
    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await?;
    axum::serve(listener, router).await?;

    Ok(())
}

async fn create_handler(State(app): State<App>, body: Bytes) -> (StatusCode, Json<CreateResponse>) {
    let id = nanoid!(8);
    let copied_id = id.clone();

    let res = tokio::task::spawn_blocking(move || {
        app.db.insert(copied_id, body.to_vec())?;
        app.keyspace.persist(PersistMode::SyncAll)
    })
    .await;

    if res.is_err() {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(CreateResponse {
                key: None,
                message: "server error".to_string(),
            }),
        );
    }

    info!("created paste {}", id);

    (
        StatusCode::OK,
        Json(CreateResponse {
            key: Some(id.to_string()),
            message: "success".to_string(),
        }),
    )
}

async fn get_raw_handler(
    State(app): State<App>,
    Path(id): Path<String>,
) -> Result<(StatusCode, String), AppError> {
    let res = tokio::task::spawn_blocking(move || app.db.get(id)).await??;
    if let Some(content) = res {
        return Ok((StatusCode::OK, String::from_utf8(content.to_vec())?));
    }

    Ok((StatusCode::NOT_FOUND, "".to_string()))
}

#[derive(Clone)]
struct App {
    keyspace: Keyspace,
    db: PartitionHandle,
}

#[derive(Serialize)]
struct CreateResponse {
    key: Option<String>,
    message: String,
}

struct AppError(anyhow::Error);

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        (StatusCode::INTERNAL_SERVER_ERROR, format!("{}", self.0)).into_response()
    }
}

impl<E> From<E> for AppError
where
    E: Into<anyhow::Error>,
{
    fn from(err: E) -> Self {
        Self(err.into())
    }
}
