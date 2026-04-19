from __future__ import annotations

import json
import shutil
from pathlib import Path

from fastapi import HTTPException
from sqlmodel import Session, select

from app.core.config import settings
from app.models.note import Device, Note, SyncEvent
from app.schemas.note import NoteCreate
from app.services.markdown_index import compute_version, markdown_index_service, slugify
from app.services.sync_bus import sync_connection_manager


class VaultSyncService:
    default_folders = [
        "00_Inbox",
        "01_Notes",
        "02_Articles",
        "03_Resources",
        "04_Templates",
    ]

    def ensure_default_structure(self) -> None:
        for folder in self.default_folders:
            (settings.cloud_vault_dir / folder).mkdir(parents=True, exist_ok=True)
            (settings.desktop_vault_dir / folder).mkdir(parents=True, exist_ok=True)

    def resolve_note_path(self, path: str) -> Path:
        clean_path = path.strip().replace("\\", "/").lstrip("/")
        if not clean_path.endswith(".md"):
            clean_path = f"{clean_path}.md"
        note_path = settings.cloud_vault_dir / clean_path
        note_path.parent.mkdir(parents=True, exist_ok=True)
        return note_path

    def safe_filename(self, title: str) -> str:
        cleaned = "".join(char for char in title if char not in '<>:"/\\|?*').strip()
        return cleaned[:80] or "untitled"

    def save_note_file(self, note: NoteCreate) -> Path:
        note_path = self.resolve_note_path(note.path)
        note_path.parent.mkdir(parents=True, exist_ok=True)
        note_path.write_text(note.content, encoding="utf-8")
        desktop_path = settings.desktop_vault_dir / note_path.relative_to(settings.cloud_vault_dir)
        desktop_path.parent.mkdir(parents=True, exist_ok=True)
        desktop_path.write_text(note.content, encoding="utf-8")
        return note_path

    def load_tree(self) -> list[dict]:
        self.ensure_default_structure()

        def build_node(folder: Path) -> list[dict]:
            nodes: list[dict] = []
            for child in sorted(folder.iterdir(), key=lambda item: (item.is_file(), item.name.lower())):
                rel_path = child.relative_to(settings.cloud_vault_dir).as_posix()
                if child.is_dir():
                    nodes.append(
                        {
                            "name": child.name,
                            "path": rel_path,
                            "node_type": "folder",
                            "children": build_node(child),
                            "note_slug": None,
                        }
                    )
                elif child.suffix.lower() == ".md":
                    nodes.append(
                        {
                            "name": child.name,
                            "path": rel_path,
                            "node_type": "file",
                            "children": [],
                            "note_slug": slugify(child.stem),
                        }
                    )
            return nodes

        return build_node(settings.cloud_vault_dir)

    def create_folder(self, path: str) -> None:
        folder = settings.cloud_vault_dir / path.strip().replace("\\", "/").strip("/")
        folder.mkdir(parents=True, exist_ok=True)
        desktop_folder = settings.desktop_vault_dir / path.strip().replace("\\", "/").strip("/")
        desktop_folder.mkdir(parents=True, exist_ok=True)

    def move_folder(self, source_path: str, target_path: str, session: Session) -> None:
        source = settings.cloud_vault_dir / source_path.strip().replace("\\", "/").strip("/")
        target = settings.cloud_vault_dir / target_path.strip().replace("\\", "/").strip("/")
        if not source.exists():
            raise HTTPException(status_code=404, detail="folder not found")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(source), str(target))
        desktop_source = settings.desktop_vault_dir / source_path.strip().replace("\\", "/").strip("/")
        desktop_target = settings.desktop_vault_dir / target_path.strip().replace("\\", "/").strip("/")
        if desktop_source.exists():
            desktop_target.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(desktop_source), str(desktop_target))

        notes = session.exec(select(Note).where(Note.path.startswith(source_path))).all()
        for note in notes:
            note.path = note.path.replace(source_path, target_path, 1)
            note.parent_path = note.path.rsplit("/", 1)[0] if "/" in note.path else ""
            markdown_index_service.update_search_index(session, note)

    def delete_folder(self, path: str, session: Session) -> list[Note]:
        folder = settings.cloud_vault_dir / path.strip().replace("\\", "/").strip("/")
        if not folder.exists():
            return []

        notes = session.exec(select(Note).where(Note.path.startswith(path), Note.is_deleted == False)).all()
        shutil.rmtree(folder)
        desktop_folder = settings.desktop_vault_dir / path.strip().replace("\\", "/").strip("/")
        if desktop_folder.exists():
            shutil.rmtree(desktop_folder)
        return notes

    def delete_note_file(self, path: str) -> None:
        note_path = settings.cloud_vault_dir / path
        if note_path.exists():
            note_path.unlink()
        desktop_path = settings.desktop_vault_dir / path
        if desktop_path.exists():
            desktop_path.unlink()

    def move_note_file(self, source_path: str, target_path: str) -> None:
        source = settings.cloud_vault_dir / source_path
        target = settings.cloud_vault_dir / target_path
        if not source.exists():
            raise HTTPException(status_code=404, detail="note file not found")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(source), str(target))

        desktop_source = settings.desktop_vault_dir / source_path
        desktop_target = settings.desktop_vault_dir / target_path
        if desktop_source.exists():
            desktop_target.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(desktop_source), str(desktop_target))

    def create_conflict_copy(self, note: NoteCreate, device_id: str) -> NoteCreate:
        stem = Path(note.path).stem
        suffix = Path(note.path).suffix or ".md"
        parent = Path(note.path).parent.as_posix()
        parent = "00_Inbox" if parent == "." else parent
        conflict_name = f"{stem}.conflict-{device_id}{suffix}"
        return NoteCreate(
            title=f"{note.title} Conflict {device_id}",
            slug=slugify(f"{note.slug}-conflict-{device_id}"),
            content=note.content,
            path=f"{parent}/{conflict_name}",
            tags=note.tags,
            links=note.links,
            source_url=note.source_url,
        )

    def register_device(self, session: Session, device_id: str, device_type: str) -> Device:
        device = session.exec(select(Device).where(Device.device_id == device_id)).first()
        if device is None:
            device = Device(device_id=device_id, device_type=device_type, display_name=device_id, sync_status="online")
            session.add(device)
        else:
            device.device_type = device_type
            device.sync_status = "online"
        session.commit()
        session.refresh(device)
        return device

    async def queue_sync_event(
        self,
        session: Session,
        note_slug: str,
        device_id: str,
        event_type: str,
        version: str,
        path: str,
        target_path: str = "",
        payload: dict | None = None,
    ) -> SyncEvent:
        event = SyncEvent(
            note_slug=note_slug,
            device_id=device_id,
            event_type=event_type,
            version=version,
            path=path,
            target_path=target_path,
            payload_json=json.dumps(payload or {}, ensure_ascii=False),
        )
        session.add(event)
        session.commit()
        session.refresh(event)
        await sync_connection_manager.push_event(
            {
                "event_id": event.id,
                "note_slug": note_slug,
                "event_type": event_type,
                "path": path,
                "target_path": target_path,
                "version": version,
                "payload": payload or {},
            }
        )
        return event

    def note_version_matches(self, note: Note | None, previous_version: str | None) -> bool:
        if note is None or not previous_version:
            return True
        return note.version == previous_version

    def materialize_note(self, title: str, path: str, content: str, source_url: str = "") -> NoteCreate:
        safe_name = self.safe_filename(Path(path).stem)
        resolved_path = str(Path(path).with_name(f"{safe_name}.md")).replace("\\", "/")
        slug = slugify(safe_name)
        return NoteCreate(
            title=title,
            slug=slug,
            content=content,
            path=resolved_path,
            tags=[],
            links=[],
            source_url=source_url,
        )

    def sync_version_for(self, content: str) -> str:
        return compute_version(content)


vault_sync_service = VaultSyncService()
