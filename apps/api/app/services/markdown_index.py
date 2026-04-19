from __future__ import annotations

import json
import re
from hashlib import sha1

from sqlalchemy import text
from sqlmodel import Session, delete, select

from app.core.config import settings
from app.models.note import Note, NoteLink, NoteTag, RagChunk

LINK_PATTERN = re.compile(r"\[\[([^\]|#]+)(?:#[^\]]+)?(?:\|[^\]]+)?\]\]")
TAG_PATTERN = re.compile(r"(?<!\w)#([\u4e00-\u9fffA-Za-z0-9_\-/]+)")


def slugify(value: str) -> str:
    normalized = re.sub(r"[^\w\u4e00-\u9fff-]+", "-", value.strip().lower())
    normalized = re.sub(r"-+", "-", normalized).strip("-")
    return normalized or sha1(value.encode("utf-8")).hexdigest()[:10]


def compute_version(content: str) -> str:
    return sha1(content.encode("utf-8")).hexdigest()


def extract_links(content: str) -> list[str]:
    return sorted({slugify(match.strip()) for match in LINK_PATTERN.findall(content) if match.strip()})


def extract_tags(content: str) -> list[str]:
    return sorted({match.lower() for match in TAG_PATTERN.findall(content)})


def build_summary(content: str) -> str:
    lines = [line.strip() for line in content.splitlines() if line.strip()]
    return " ".join(lines[:3])[:220]


def chunk_markdown(content: str) -> list[str]:
    blocks = [block.strip() for block in re.split(r"\n\s*\n", content) if block.strip()]
    chunks: list[str] = []
    buffer = ""
    for block in blocks:
        candidate = f"{buffer}\n\n{block}".strip() if buffer else block
        if len(candidate) <= settings.rag_chunk_size:
            buffer = candidate
            continue
        if buffer:
            chunks.append(buffer)
        overlap = buffer[-settings.rag_overlap :] if buffer else ""
        buffer = f"{overlap}\n{block}".strip()

    if buffer:
        chunks.append(buffer)
    return chunks or [content]


def hashed_embedding(content: str, size: int = 64) -> list[float]:
    vector = [0.0] * size
    tokens = re.findall(r"[\w\u4e00-\u9fff]+", content.lower())
    if not tokens:
        return vector

    for token in tokens:
        index = int(sha1(token.encode("utf-8")).hexdigest(), 16) % size
        vector[index] += 1.0

    length = sum(value * value for value in vector) ** 0.5 or 1.0
    return [round(value / length, 6) for value in vector]


class MarkdownIndexService:
    def reindex_note(self, session: Session, note: Note) -> None:
        links = extract_links(note.content)
        tags = extract_tags(note.content)
        note.links = ",".join(links)
        note.tags = ",".join(tags)
        note.version = compute_version(note.content)
        note.summary = build_summary(note.content)
        note.parent_path = note.path.rsplit("/", 1)[0] if "/" in note.path else ""

        session.exec(delete(NoteLink).where(NoteLink.source_slug == note.slug))
        session.exec(delete(NoteTag).where(NoteTag.note_slug == note.slug))
        session.exec(delete(RagChunk).where(RagChunk.note_slug == note.slug))

        for tag in tags:
            session.add(NoteTag(note_slug=note.slug, tag=tag))

        for target_slug in links:
            resolved = session.exec(select(Note).where(Note.slug == target_slug, Note.is_deleted == False)).first() is not None
            session.add(
                NoteLink(
                    source_slug=note.slug,
                    target_slug=target_slug,
                    target_title=target_slug,
                    is_resolved=resolved,
                )
            )

        for index, chunk in enumerate(chunk_markdown(note.content)):
            session.add(
                RagChunk(
                    note_slug=note.slug,
                    chunk_index=index,
                    content=chunk,
                    token_count=len(chunk.split()),
                    embedding_ref=json.dumps(hashed_embedding(chunk)),
                )
            )

        session.flush()
        self.update_search_index(session, note)

    def delete_note_index(self, note: Note, session: Session) -> None:
        session.exec(delete(NoteLink).where(NoteLink.source_slug == note.slug))
        session.exec(delete(NoteLink).where(NoteLink.target_slug == note.slug))
        session.exec(delete(NoteTag).where(NoteTag.note_slug == note.slug))
        session.exec(delete(RagChunk).where(RagChunk.note_slug == note.slug))
        self.delete_search_index(session, note.id or 0)

    def update_search_index(self, session: Session, note: Note) -> None:
        session.execute(text("DELETE FROM note_search WHERE note_id = :note_id"), {"note_id": str(note.id or 0)})
        session.execute(
            text(
                "INSERT INTO note_search(note_id, title, content, tags, path) VALUES (:note_id, :title, :content, :tags, :path)"
            ),
            {
                "note_id": str(note.id or 0),
                "title": note.title,
                "content": note.content,
                "tags": note.tags,
                "path": note.path,
            },
        )

    def delete_search_index(self, session: Session, note_id: int) -> None:
        session.execute(text("DELETE FROM note_search WHERE note_id = :note_id"), {"note_id": str(note_id)})


markdown_index_service = MarkdownIndexService()