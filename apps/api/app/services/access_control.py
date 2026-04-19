from __future__ import annotations

import hashlib
import hmac
from collections import defaultdict
from datetime import UTC, datetime
from uuid import uuid4

from sqlmodel import Session, select

from app.core.config import settings
from app.models.note import InstanceConfig, KnowledgeSpace, Note, NoteAccessPolicy, TeamMembership, User
from app.services.markdown_index import slugify


class AccessControlService:
    def generate_instance_id(self) -> str:
        return settings.instance_seed_id or uuid4().hex

    def get_instance(self, session: Session) -> InstanceConfig | None:
        return session.exec(select(InstanceConfig).order_by(InstanceConfig.id.asc())).first()

    def ensure_instance(self, session: Session) -> InstanceConfig:
        instance = self.get_instance(session)
        if instance is not None:
            return instance

        deployment_mode = settings.deployment_mode if settings.deployment_mode in {"desktop", "server"} else "desktop"
        edition = settings.edition if settings.edition in {"personal", "team"} else "personal"
        instance = InstanceConfig(
            instance_id=self.generate_instance_id(),
            instance_name=settings.instance_name,
            deployment_mode=deployment_mode,
            edition=edition,
            desktop_host_enabled=deployment_mode == "desktop",
            server_host_enabled=deployment_mode == "server",
            auth_required=edition == "team",
            team_license_verified=edition == "personal",
            is_initialized=False,
        )
        session.add(instance)
        session.commit()
        session.refresh(instance)
        return instance

    def verify_team_authorization_code(self, authorization_code: str) -> bool:
        expected = settings.team_authorization_code.strip()
        if not expected:
            return False
        return hmac.compare_digest(expected, authorization_code.strip())

    def hash_authorization_code(self, authorization_code: str) -> str:
        return hashlib.sha256(authorization_code.encode("utf-8")).hexdigest()

    def ensure_user_default_space(self, session: Session, instance_id: str, user: User) -> KnowledgeSpace:
        space = session.exec(
            select(KnowledgeSpace).where(KnowledgeSpace.owner_user_id == (user.id or 0), KnowledgeSpace.is_default == True)
        ).first()
        if space is not None:
            return space

        base_slug = slugify(f"{user.username}-space")
        slug = base_slug
        suffix = 1
        while session.exec(select(KnowledgeSpace).where(KnowledgeSpace.slug == slug)).first() is not None:
            suffix += 1
            slug = f"{base_slug}-{suffix}"

        space = KnowledgeSpace(
            instance_id=instance_id,
            owner_user_id=user.id or 0,
            slug=slug,
            name=f"{user.display_name} 的知识库",
            visibility="private",
            is_default=True,
        )
        session.add(space)
        session.commit()
        session.refresh(space)
        return space

    def resolve_user_space(self, session: Session, instance_id: str, user: User, space_slug: str | None = None) -> KnowledgeSpace:
        if space_slug:
            space = session.exec(
                select(KnowledgeSpace).where(
                    KnowledgeSpace.instance_id == instance_id,
                    KnowledgeSpace.owner_user_id == (user.id or 0),
                    KnowledgeSpace.slug == space_slug,
                )
            ).first()
            if space is not None:
                return space
        return self.ensure_user_default_space(session, instance_id, user)

    def ensure_membership(self, session: Session, instance_id: str, user: User, space_slug: str, role: str) -> TeamMembership:
        membership = session.exec(
            select(TeamMembership).where(TeamMembership.instance_id == instance_id, TeamMembership.user_id == (user.id or 0))
        ).first()
        if membership is not None:
            if membership.space_slug != space_slug or membership.role != role or membership.is_active is False:
                membership.space_slug = space_slug
                membership.role = role
                membership.is_active = True
                session.add(membership)
                session.commit()
                session.refresh(membership)
            return membership

        membership = TeamMembership(
            instance_id=instance_id,
            user_id=user.id or 0,
            space_slug=space_slug,
            role=role,
            content_visibility_scope="private",
            is_active=True,
        )
        session.add(membership)
        session.commit()
        session.refresh(membership)
        return membership

    def get_membership(self, session: Session, instance_id: str, user_id: int) -> TeamMembership | None:
        return session.exec(
            select(TeamMembership).where(
                TeamMembership.instance_id == instance_id,
                TeamMembership.user_id == user_id,
                TeamMembership.is_active == True,
            )
        ).first()

    def is_instance_admin(self, session: Session, user: User) -> bool:
        instance = self.ensure_instance(session)
        if user.username == settings.admin_username:
            return True
        membership = self.get_membership(session, instance.instance_id, user.id or 0)
        return membership is not None and membership.role == "admin"

    def ensure_note_policy(self, session: Session, note_slug: str, owner_user_id: int, space_slug: str) -> NoteAccessPolicy:
        policy = session.exec(select(NoteAccessPolicy).where(NoteAccessPolicy.note_slug == note_slug)).first()
        if policy is not None:
            return policy

        policy = NoteAccessPolicy(
            note_slug=note_slug,
            owner_user_id=owner_user_id,
            space_slug=space_slug,
            visibility_scope="private",
            shared_member_ids="",
        )
        session.add(policy)
        session.flush()
        return policy

    def parse_shared_member_ids(self, value: str) -> set[int]:
        result: set[int] = set()
        for item in value.split(","):
            item = item.strip()
            if item.isdigit():
                result.add(int(item))
        return result

    def serialize_shared_member_ids(self, member_ids: list[int]) -> str:
        return ",".join(str(member_id) for member_id in sorted({member_id for member_id in member_ids if member_id > 0}))

    def can_read_note(self, session: Session, user: User, note_slug: str) -> bool:
        instance = self.ensure_instance(session)
        if instance.edition == "personal":
            return True

        if self.is_instance_admin(session, user):
            return True

        policy = session.exec(select(NoteAccessPolicy).where(NoteAccessPolicy.note_slug == note_slug)).first()
        if policy is None:
            return False
        if policy.owner_user_id == (user.id or 0):
            return True
        if policy.visibility_scope == "team":
            membership = self.get_membership(session, instance.instance_id, user.id or 0)
            return membership is not None
        if policy.visibility_scope == "selected":
            return (user.id or 0) in self.parse_shared_member_ids(policy.shared_member_ids)
        return False

    def can_edit_note(self, session: Session, user: User, note_slug: str) -> bool:
        if self.is_instance_admin(session, user):
            return True
        policy = session.exec(select(NoteAccessPolicy).where(NoteAccessPolicy.note_slug == note_slug)).first()
        return policy is not None and policy.owner_user_id == (user.id or 0)

    def visible_note_slugs(self, session: Session, user: User, include_deleted: bool = False) -> set[str]:
        if self.ensure_instance(session).edition == "personal":
            statement = select(Note.slug)
            if not include_deleted:
                statement = statement.where(Note.is_deleted == False)
            return set(session.exec(statement).all())

        note_slugs = set()
        statement = select(Note)
        if not include_deleted:
            statement = statement.where(Note.is_deleted == False)
        for note in session.exec(statement).all():
            if self.can_read_note(session, user, note.slug):
                note_slugs.add(note.slug)
        return note_slugs

    def visible_notes(self, session: Session, user: User, include_deleted: bool = False) -> list[Note]:
        visible_slugs = self.visible_note_slugs(session, user, include_deleted=include_deleted)
        if not visible_slugs:
            return []
        statement = select(Note).where(Note.slug.in_(visible_slugs))
        if not include_deleted:
            statement = statement.where(Note.is_deleted == False)
        return session.exec(statement.order_by(Note.updated_at.desc())).all()

    def build_tree(self, notes: list[Note]) -> list[dict]:
        root: dict[str, dict] = {}

        def ensure_folder(path_parts: list[str]) -> dict[str, dict]:
            node = root
            current_path = []
            for part in path_parts:
                current_path.append(part)
                node = node.setdefault(
                    part,
                    {"name": part, "path": "/".join(current_path), "node_type": "folder", "children": {}},
                )["children"]
            return node

        for note in sorted(notes, key=lambda item: item.path):
            parts = [part for part in note.path.split("/") if part]
            if not parts:
                continue
            folder_parts = parts[:-1]
            filename = parts[-1]
            target = ensure_folder(folder_parts)
            target[filename] = {
                "name": filename,
                "path": note.path,
                "node_type": "file",
                "note_slug": note.slug,
            }

        def convert(children: dict[str, dict]) -> list[dict]:
            result = []
            for key in sorted(children):
                child = children[key]
                if child["node_type"] == "folder":
                    result.append(
                        {
                            "name": child["name"],
                            "path": child["path"],
                            "node_type": "folder",
                            "children": convert(child["children"]),
                        }
                    )
                else:
                    result.append(child)
            return result

        return convert(root)

    def backfill_note_policies(self, session: Session, owner_user_id: int, space_slug: str) -> None:
        existing = set(session.exec(select(NoteAccessPolicy.note_slug)).all())
        notes = session.exec(select(Note)).all()
        dirty = False
        for note in notes:
            if note.slug in existing:
                continue
            session.add(
                NoteAccessPolicy(
                    note_slug=note.slug,
                    owner_user_id=owner_user_id,
                    space_slug=space_slug,
                    visibility_scope="private",
                    shared_member_ids="",
                )
            )
            dirty = True
        if dirty:
            session.commit()

    def count_tags_for_visible_notes(self, session: Session, user: User) -> dict[str, int]:
        visible_slugs = self.visible_note_slugs(session, user)
        counter: dict[str, int] = defaultdict(int)
        if not visible_slugs:
            return counter
        for note in session.exec(select(Note).where(Note.slug.in_(visible_slugs), Note.is_deleted == False)).all():
            for tag in [item for item in note.tags.split(",") if item]:
                counter[tag] += 1
        return counter


access_control_service = AccessControlService()