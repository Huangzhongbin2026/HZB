from __future__ import annotations

import json

from sqlmodel import Session, select

from app.models.note import Note, RagChunk
from app.schemas.note import RagQueryResponse
from app.services.markdown_index import hashed_embedding


def cosine_similarity(left: list[float], right: list[float]) -> float:
    return sum(a * b for a, b in zip(left, right))


class RagIndexService:
    def rebuild_note_index(self, note: Note) -> None:
        _ = note

    def query(self, session: Session, query: str, top_k: int, visible_note_slugs: set[str] | None = None) -> RagQueryResponse:
        query_vector = hashed_embedding(query)
        chunks = session.exec(select(RagChunk)).all()
        scored: list[tuple[float, RagChunk]] = []
        for chunk in chunks:
            if visible_note_slugs is not None and chunk.note_slug not in visible_note_slugs:
                continue
            try:
                embedding = json.loads(chunk.embedding_ref)
            except json.JSONDecodeError:
                embedding = [0.0] * len(query_vector)
            scored.append((cosine_similarity(query_vector, embedding), chunk))

        ranked = sorted(scored, key=lambda item: item[0], reverse=True)[:top_k]
        note_lookup = {
            note.slug: note
            for note in session.exec(select(Note).where(Note.is_deleted == False)).all()
        }
        hits = [
            {
                "title": note_lookup.get(chunk.note_slug).title if chunk.note_slug in note_lookup else chunk.note_slug,
                "note_slug": chunk.note_slug,
                "snippet": chunk.content[:280],
                "score": f"{score:.3f}",
            }
            for score, chunk in ranked
            if score > 0
        ]
        return RagQueryResponse(hits=hits)


rag_index_service = RagIndexService()
