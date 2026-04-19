from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    app_name: str = "Private Knowledge Cloud API"
    api_v1_prefix: str = "/api/v1"
    instance_name: str = "个人双向知识库"
    deployment_mode: str = "desktop"
    edition: str = "personal"
    instance_seed_id: str | None = None
    team_authorization_code: str = ""
    team_invite_base_url: str = ""
    database_url: str = "sqlite:///./knowledge_cloud.db"
    storage_root: Path = Path("storage")
    cloud_vault_dir: Path = Path("storage/cloud_vault")
    desktop_vault_dir: Path = Path("storage/desktop_vault")
    rag_workspace_dir: Path = Path("storage/rag_workspace")
    jwt_secret: str = "change-me-in-production-with-at-least-32-bytes"
    jwt_algorithm: str = "HS256"
    jwt_exp_minutes: int = 60 * 24 * 7
    admin_username: str = "admin"
    admin_password: str = "admin123456"
    clip_timeout_seconds: int = 12
    rag_chunk_size: int = 700
    rag_overlap: int = 120
    ai_api_base: str | None = None
    ai_api_key: str | None = None
    ai_model: str = "gpt-4.1-mini"
    ai_provider_label: str = "环境变量默认配置"
    cors_origins: list[str] = ["*"]


settings = Settings()
