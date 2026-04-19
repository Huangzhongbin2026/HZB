from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy import text
from sqlmodel import Session, select

from app.db.session import engine
from app.models.note import Note
from app.models.note import User
from app.schemas.note import SearchResponse, SearchResult
from app.services.access_control import access_control_service


class SearchService:
    def search(self, session: Session, user: User, query: str, limit: int = 20) -> SearchResponse:
        try:
            with engine.begin() as connection:
                rows = connection.execute(
                    text(
                        "SELECT note_id, snippet(note_search, 2, '[', ']', '…', 16) AS snippet FROM note_search WHERE note_search MATCH :query LIMIT :limit"
                    ),
                    {"query": query, "limit": limit},
                ).fetchall()
        except SQLAlchemyError:
            rows = []

        visible_slugs = access_control_service.visible_note_slugs(session, user)

        if not rows:
            notes = session.exec(
                select(Note).where(
                    Note.is_deleted == False,
                    (Note.title.contains(query) | Note.content.contains(query) | Note.tags.contains(query)),
                )
            ).all()
            return SearchResponse(
                results=[
                    SearchResult(
                        note_slug=note.slug,
                        title=note.title,
                        path=note.path,
                        snippet=(note.summary or note.content[:120]),
                    )
                    for note in notes
                    if note.slug in visible_slugs
                ][:limit]
            )

        note_ids = [int(row.note_id) for row in rows if str(row.note_id).isdigit()]
        results: list[SearchResult] = []
        if not note_ids:
            return SearchResponse(results=[])

        notes = session.exec(select(Note).where(Note.id.in_(note_ids))).all()
        note_map = {note.id: note for note in notes}
        for row in rows:
            note = note_map.get(int(row.note_id))
            if note is None or note.slug not in visible_slugs:
                continue
            results.append(
                SearchResult(
                    note_slug=note.slug,
                    title=note.title,
                    path=note.path,
                    snippet=row.snippet,
                )
            )
        return SearchResponse(results=results)


search_service = SearchService()