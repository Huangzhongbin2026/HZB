from sqlalchemy import text
from sqlmodel import Session, SQLModel, create_engine, select

from app.core.config import settings
from app.models.note import User
from app.services.access_control import access_control_service
from app.services.security import hash_password, verify_password

engine = create_engine(settings.database_url, connect_args={"check_same_thread": False})


def init_db() -> None:
    settings.storage_root.mkdir(parents=True, exist_ok=True)
    settings.cloud_vault_dir.mkdir(parents=True, exist_ok=True)
    settings.desktop_vault_dir.mkdir(parents=True, exist_ok=True)
    settings.rag_workspace_dir.mkdir(parents=True, exist_ok=True)
    SQLModel.metadata.create_all(engine)
    with engine.begin() as connection:
        connection.execute(
            text(
                "CREATE VIRTUAL TABLE IF NOT EXISTS note_search USING fts5(note_id UNINDEXED, title, content, tags, path)"
            )
        )

    with Session(engine) as session:
        admin = session.exec(select(User).where(User.username == settings.admin_username)).first()
        if admin is None:
            admin = User(
                username=settings.admin_username,
                password_hash=hash_password(settings.admin_password),
                display_name="System Admin",
            )
            session.add(admin)
            session.commit()
            session.refresh(admin)
        elif not verify_password(settings.admin_password, admin.password_hash):
            admin.password_hash = hash_password(settings.admin_password)
            session.add(admin)
            session.commit()

        instance = access_control_service.ensure_instance(session)
        default_space = access_control_service.ensure_user_default_space(session, instance.instance_id, admin)
        access_control_service.ensure_membership(session, instance.instance_id, admin, default_space.slug, "admin")
        access_control_service.backfill_note_policies(session, admin.id or 0, default_space.slug)


def get_session():
    with Session(engine) as session:
        yield session
