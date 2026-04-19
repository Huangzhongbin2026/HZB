from __future__ import annotations

import json
import secrets
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Iterable

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, WebSocket, WebSocketDisconnect
from sqlmodel import Session, delete, select

from app.api.deps import get_current_user
from app.core.config import settings
from app.db.session import get_session
from app.models.note import (
    AiProviderConfig,
    IdeaActivityLog,
    IdeaRecord,
    InstanceConfig,
    KnowledgeSpace,
    Note,
    NoteAccessPolicy,
    NoteLink,
    GoalJournalEntry,
    GoalPlanRecord,
    GoalRecord,
    GoalTaskRecord,
    PasswordAccessAudit,
    SystemPluginConfig,
    PasswordVaultConfig,
    PasswordVaultEntry,
    SavedClip,
    SyncEvent,
    TopicActivityLog,
    TopicRecord,
    TeamInvite,
    TeamMembership,
    User,
)
from app.schemas.auth import LoginRequest, TokenResponse, UserRead
from app.schemas.instance import (
    InstanceBootstrapRequest,
    InstanceConfigRead,
    KnowledgeSpaceCreateRequest,
    KnowledgeSpaceRead,
    NoteSharingRead,
    NoteSharingUpdateRequest,
    TeamInviteAcceptRequest,
    TeamInviteCreateRequest,
    TeamInviteRead,
    TeamMemberRead,
)
from app.schemas.note import (
    AiGenerateRequest,
    AiGenerateResponse,
    AiProviderCatalogResponse,
    AiProviderConfigRead,
    AiProviderConfigUpsert,
    BacklinkBundle,
    DeleteRequest,
    DocumentUploadResponse,
    FileTreeNode,
    FolderCreateRequest,
    FolderDeleteRequest,
    FolderMoveRequest,
    MoveNoteRequest,
    NoteCreate,
    NoteRead,
    NoteUpdate,
    RagQueryRequest,
    RagQueryResponse,
    SearchResponse,
    SyncAckRequest,
    SyncEventRead,
    SyncStatusResponse,
    SyncUploadRequest,
    TagSummary,
    WebClipRequest,
    WebClipResponse,
    WebClipSaveRequest,
)
from app.schemas.plugin import (
    GoalAiPlanRequest,
    GoalAiPlanResponse,
    GoalCreateRequest,
    IdeaActivityLogRead,
    IdeaCreateRequest,
    IdeaOverviewResponse,
    IdeaRead,
    IdeaStatsRead,
    IdeaStatusUpdateRequest,
    IdeaUpdateRequest,
    GoalJournalRead,
    GoalJournalUpdateRequest,
    GoalOverviewResponse,
    GoalPlanCreateRequest,
    GoalPlanRead,
    GoalPlanSuggestion,
    GoalRead,
    GoalTaskCreateRequest,
    GoalTaskRead,
    GoalTaskToggleRequest,
    GoalTaskUpdateRequest,
    GoalUpdateRequest,
    PasswordAuditRead,
    PasswordEntryCreateRequest,
    PasswordEntryRead,
    PasswordEntrySecretRead,
    PasswordEntryUpdateRequest,
    PasswordGenerateRequest,
    PasswordGenerateResponse,
    PasswordRevealRequest,
    PasswordVaultConfigRead,
    PasswordVaultSetupRequest,
    PasswordVaultVerifyRequest,
    SystemPluginConfigRead,
    SystemPluginConfigUpdateRequest,
    TopicActivityLogRead,
    TopicAiDiscoverRequest,
    TopicAiDiscoverResponse,
    TopicAiOutlineRequest,
    TopicAiOutlineResponse,
    TopicCreateRequest,
    TopicOverviewResponse,
    TopicRead,
    TopicStatsRead,
    TopicStatusUpdateRequest,
    TopicUpdateRequest,
    UnifiedSearchItem,
    UnifiedSearchResponse,
)
from app.services.access_control import access_control_service
from app.services.ai import ai_service
from app.services.clipper import clip_service
from app.services.document_ingest import document_ingest_service
from app.services.markdown_index import compute_version, markdown_index_service, slugify
from app.services.password_manager import password_manager_service
from app.services.rag_index import rag_index_service
from app.services.search import search_service
from app.services.security import create_access_token, decode_token, hash_password, verify_password
from app.services.sync_bus import sync_connection_manager
from app.services.vault_sync import vault_sync_service

router = APIRouter()

IDEA_STATUS_TRANSITIONS: dict[str, set[str]] = {
    "pending_review": {"accepted", "shelved"},
    "accepted": {"planning", "shelved"},
    "planning": {"accepted", "building", "shelved"},
    "building": {"planning", "launched", "shelved"},
    "launched": {"planning", "shelved"},
    "shelved": {"pending_review", "accepted"},
}


def to_utc_iso(value: datetime) -> str:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC).isoformat()
    return value.astimezone(UTC).isoformat()


def normalize_utc(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value.astimezone(UTC)


def parse_iso_datetime(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        normalized = value.replace("Z", "+00:00")
        parsed = datetime.fromisoformat(normalized)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail="invalid datetime") from exc
    return normalize_utc(parsed)


def parse_id_csv(value: str) -> list[int]:
    result: list[int] = []
    for item in value.split(","):
        item = item.strip()
        if item.isdigit():
            result.append(int(item))
    return result


def serialize_id_csv(values: Iterable[int]) -> str:
    unique = sorted({value for value in values if value > 0})
    return ",".join(str(value) for value in unique)


def serialize_note(note: Note) -> NoteRead:
    return NoteRead(
        id=note.id or 0,
        title=note.title,
        slug=note.slug,
        content=note.content,
        path=note.path,
        parent_path=note.parent_path,
        tags=[item for item in note.tags.split(",") if item],
        links=[item for item in note.links.split(",") if item],
        summary=note.summary,
        version=note.version,
        updated_at=to_utc_iso(note.updated_at),
    )


def serialize_event(event: SyncEvent) -> SyncEventRead:
    return SyncEventRead(
        id=event.id or 0,
        note_slug=event.note_slug,
        event_type=event.event_type,
        path=event.path,
        target_path=event.target_path,
        payload_json=event.payload_json,
        version=event.version,
        status=event.status,
        created_at=to_utc_iso(event.created_at),
    )


def serialize_instance(instance: InstanceConfig) -> InstanceConfigRead:
    return InstanceConfigRead(
        instance_id=instance.instance_id,
        instance_name=instance.instance_name,
        deployment_mode=instance.deployment_mode,
        edition=instance.edition,
        desktop_host_enabled=instance.desktop_host_enabled,
        server_host_enabled=instance.server_host_enabled,
        auth_required=instance.auth_required,
        team_license_verified=instance.team_license_verified,
        is_initialized=instance.is_initialized,
    )


def serialize_space(space: KnowledgeSpace) -> KnowledgeSpaceRead:
    return KnowledgeSpaceRead(
        id=space.id or 0,
        instance_id=space.instance_id,
        owner_user_id=space.owner_user_id,
        slug=space.slug,
        name=space.name,
        visibility=space.visibility,
        is_default=space.is_default,
    )


def serialize_password_entry(entry: PasswordVaultEntry) -> PasswordEntryRead:
    return PasswordEntryRead(
        id=entry.id or 0,
        title=entry.title,
        username=entry.username,
        category=entry.category,
        url=entry.url,
        website=entry.website,
        notes=entry.notes,
        linked_note_slug=entry.linked_note_slug,
        vault_scope=entry.vault_scope,
        shared_member_ids=parse_id_csv(entry.shared_member_ids),
        editor_member_ids=parse_id_csv(entry.editor_member_ids),
        created_at=to_utc_iso(entry.created_at),
        updated_at=to_utc_iso(entry.updated_at),
        last_used_at=to_utc_iso(entry.last_used_at) if entry.last_used_at else None,
    )


def serialize_goal(goal: GoalRecord, progress_percent: int) -> GoalRead:
    return GoalRead(
        id=goal.id or 0,
        title=goal.title,
        vision=goal.vision,
        key_results=[item for item in goal.key_results.split("\n") if item],
        priority=goal.priority,
        status=goal.status,
        visibility_scope=goal.visibility_scope,
        cycle_start=to_utc_iso(goal.cycle_start) if goal.cycle_start else None,
        cycle_end=to_utc_iso(goal.cycle_end) if goal.cycle_end else None,
        progress_percent=progress_percent,
        created_at=to_utc_iso(goal.created_at),
        updated_at=to_utc_iso(goal.updated_at),
    )


def serialize_goal_plan(plan: GoalPlanRecord, progress_percent: int) -> GoalPlanRead:
    return GoalPlanRead(
        id=plan.id or 0,
        goal_id=plan.goal_id,
        title=plan.title,
        summary=plan.summary,
        priority=plan.priority,
        status=plan.status,
        sequence=plan.sequence,
        start_date=to_utc_iso(plan.start_date) if plan.start_date else None,
        end_date=to_utc_iso(plan.end_date) if plan.end_date else None,
        progress_percent=progress_percent,
        created_at=to_utc_iso(plan.created_at),
        updated_at=to_utc_iso(plan.updated_at),
    )


def serialize_goal_task(task: GoalTaskRecord) -> GoalTaskRead:
    return GoalTaskRead(
        id=task.id or 0,
        goal_id=task.goal_id,
        plan_id=task.plan_id,
        title=task.title,
        details=task.details,
        priority=task.priority,
        status=task.status,
        due_date=to_utc_iso(task.due_date) if task.due_date else None,
        assignee_user_id=task.assignee_user_id,
        completed_at=to_utc_iso(task.completed_at) if task.completed_at else None,
        created_at=to_utc_iso(task.created_at),
        updated_at=to_utc_iso(task.updated_at),
    )


def serialize_goal_journal(entry: GoalJournalEntry) -> GoalJournalRead:
    return GoalJournalRead(
        id=entry.id or 0,
        goal_id=entry.goal_id,
        plan_id=entry.plan_id,
        journal_date=entry.journal_date,
        note_slug=entry.note_slug,
        task_ids=parse_id_csv(entry.task_ids),
        reflection=entry.reflection,
        ai_summary=entry.ai_summary,
        updated_at=to_utc_iso(entry.updated_at),
    )


def serialize_plugin_config(plugin_id: str, is_enabled: bool) -> SystemPluginConfigRead:
    return SystemPluginConfigRead(plugin_id=plugin_id, is_enabled=is_enabled)


def serialize_topic(topic: TopicRecord) -> TopicRead:
    return TopicRead(
        id=topic.id or 0,
        title=topic.title,
        domain=topic.domain,
        keywords=[item for item in topic.keywords.split("\n") if item],
        source_type=topic.source_type,
        status=topic.status,
        priority=topic.priority,
        heat_score=topic.heat_score,
        trend_summary=topic.trend_summary,
        notes=topic.notes,
        ai_outline=topic.ai_outline,
        linked_note_slug=topic.linked_note_slug,
        linked_password_entry_ids=parse_id_csv(topic.linked_password_entry_ids),
        completed_note_slug=topic.completed_note_slug,
        assignee_user_id=topic.assignee_user_id,
        review_status=topic.review_status,
        due_date=to_utc_iso(topic.due_date) if topic.due_date else None,
        last_collected_at=to_utc_iso(topic.last_collected_at) if topic.last_collected_at else None,
        created_at=to_utc_iso(topic.created_at),
        updated_at=to_utc_iso(topic.updated_at),
    )


def serialize_topic_log(log: TopicActivityLog) -> TopicActivityLogRead:
    return TopicActivityLogRead(
        id=log.id or 0,
        topic_id=log.topic_id,
        actor_user_id=log.actor_user_id,
        action=log.action,
        detail=log.detail,
        created_at=to_utc_iso(log.created_at),
    )


def compute_topic_stats(topics: list[TopicRecord]) -> TopicStatsRead:
    total = len(topics)
    completed = sum(1 for topic in topics if topic.status == "completed")
    writable = sum(1 for topic in topics if topic.status == "writable")
    pending = sum(1 for topic in topics if topic.status == "pending")
    in_progress = sum(1 for topic in topics if topic.status == "in_progress")
    shelved = sum(1 for topic in topics if topic.status == "shelved")
    high_priority = sum(1 for topic in topics if topic.priority == "high")
    completion_rate = int((completed / total) * 100) if total else 0
    return TopicStatsRead(
        total=total,
        completed=completed,
        writable=writable,
        pending=pending,
        in_progress=in_progress,
        shelved=shelved,
        high_priority=high_priority,
        completion_rate=completion_rate,
    )


def compute_idea_opportunity(value_score: int, effort_score: int, business_score: int) -> int:
    return int(round((value_score + business_score + (100 - effort_score)) / 3))


def serialize_idea(idea: IdeaRecord) -> IdeaRead:
    return IdeaRead(
        id=idea.id or 0,
        title=idea.title,
        summary=idea.summary,
        details=idea.details,
        idea_type=idea.idea_type,
        tags=[item for item in idea.tags.split("\n") if item],
        status=idea.status,
        priority=idea.priority,
        value_score=idea.value_score,
        effort_score=idea.effort_score,
        business_score=idea.business_score,
        opportunity_score=compute_idea_opportunity(idea.value_score, idea.effort_score, idea.business_score),
        linked_note_slug=idea.linked_note_slug,
        linked_goal_id=idea.linked_goal_id,
        linked_topic_id=idea.linked_topic_id,
        source_context=idea.source_context,
        visibility_scope=idea.visibility_scope,
        assignee_user_id=idea.assignee_user_id,
        next_step=idea.next_step,
        created_at=to_utc_iso(idea.created_at),
        updated_at=to_utc_iso(idea.updated_at),
    )


def serialize_idea_log(log: IdeaActivityLog) -> IdeaActivityLogRead:
    return IdeaActivityLogRead(
        id=log.id or 0,
        idea_id=log.idea_id,
        actor_user_id=log.actor_user_id,
        action=log.action,
        detail=log.detail,
        created_at=to_utc_iso(log.created_at),
    )


def compute_idea_stats(ideas: list[IdeaRecord]) -> IdeaStatsRead:
    total = len(ideas)
    scores = [compute_idea_opportunity(idea.value_score, idea.effort_score, idea.business_score) for idea in ideas]
    average_opportunity_score = int(round(sum(scores) / len(scores))) if scores else 0
    return IdeaStatsRead(
        total=total,
        pending_review=sum(1 for idea in ideas if idea.status == "pending_review"),
        accepted=sum(1 for idea in ideas if idea.status == "accepted"),
        planning=sum(1 for idea in ideas if idea.status == "planning"),
        building=sum(1 for idea in ideas if idea.status == "building"),
        launched=sum(1 for idea in ideas if idea.status == "launched"),
        shelved=sum(1 for idea in ideas if idea.status == "shelved"),
        high_priority=sum(1 for idea in ideas if idea.priority == "high"),
        average_opportunity_score=average_opportunity_score,
    )


def serialize_user(session: Session, user: User) -> UserRead:
    instance = access_control_service.ensure_instance(session)
    membership = access_control_service.get_membership(session, instance.instance_id, user.id or 0)
    default_space = access_control_service.ensure_user_default_space(session, instance.instance_id, user)
    return UserRead(
        id=user.id or 0,
        username=user.username,
        display_name=user.display_name,
        role=(membership.role if membership else ("admin" if user.username == settings.admin_username else "member")),
        instance_id=instance.instance_id,
        edition=instance.edition,
        default_space_slug=default_space.slug,
    )


def require_instance_admin(session: Session, user: User) -> InstanceConfig:
    instance = access_control_service.ensure_instance(session)
    if not access_control_service.is_instance_admin(session, user):
        raise HTTPException(status_code=403, detail="admin permission required")
    return instance


def require_note_access(session: Session, slug: str, user: User) -> Note:
    note = require_note(session, slug)
    if not access_control_service.can_read_note(session, user, slug):
        raise HTTPException(status_code=404, detail="note not found")
    return note


def require_note_edit(session: Session, slug: str, user: User) -> Note:
    note = require_note_access(session, slug, user)
    if not access_control_service.can_edit_note(session, user, slug):
        raise HTTPException(status_code=403, detail="note edit not allowed")
    return note


def require_note(session: Session, slug: str) -> Note:
    note = session.exec(select(Note).where(Note.slug == slug, Note.is_deleted == False)).first()
    if note is None:
        raise HTTPException(status_code=404, detail="note not found")
    return note


def require_password_vault_config(session: Session, instance_id: str) -> PasswordVaultConfig:
    config = session.exec(select(PasswordVaultConfig).where(PasswordVaultConfig.instance_id == instance_id)).first()
    if config is None:
        raise HTTPException(status_code=404, detail="password vault not initialized")
    return config


def ensure_password_read_access(session: Session, current_user: User, entry: PasswordVaultEntry) -> None:
    instance = access_control_service.ensure_instance(session)
    if instance.edition == "personal" or access_control_service.is_instance_admin(session, current_user):
        return
    if entry.owner_user_id == (current_user.id or 0):
        return
    if entry.vault_scope == "team" and access_control_service.get_membership(session, instance.instance_id, current_user.id or 0):
        return
    if entry.vault_scope == "selected" and (current_user.id or 0) in parse_id_csv(entry.shared_member_ids):
        return
    raise HTTPException(status_code=404, detail="password entry not found")


def ensure_password_edit_access(session: Session, current_user: User, entry: PasswordVaultEntry) -> None:
    if access_control_service.is_instance_admin(session, current_user) or entry.owner_user_id == (current_user.id or 0):
        return
    if (current_user.id or 0) in parse_id_csv(entry.editor_member_ids):
        return
    raise HTTPException(status_code=403, detail="password edit not allowed")


def visible_password_entries(session: Session, current_user: User) -> list[PasswordVaultEntry]:
    entries = session.exec(select(PasswordVaultEntry).where(PasswordVaultEntry.is_deleted == False).order_by(PasswordVaultEntry.updated_at.desc())).all()
    visible: list[PasswordVaultEntry] = []
    for entry in entries:
        try:
            ensure_password_read_access(session, current_user, entry)
            visible.append(entry)
        except HTTPException:
            continue
    return visible


def record_password_audit(session: Session, entry: PasswordVaultEntry, actor_user_id: int, action: str, detail: str = "") -> None:
    session.add(
        PasswordAccessAudit(
            entry_id=entry.id or 0,
            instance_id=entry.instance_id,
            actor_user_id=actor_user_id,
            action=action,
            detail=detail,
        )
    )
    session.commit()


def visible_goals(session: Session, current_user: User) -> list[GoalRecord]:
    instance = access_control_service.ensure_instance(session)
    goals = session.exec(select(GoalRecord).order_by(GoalRecord.updated_at.desc())).all()
    if instance.edition == "personal" or access_control_service.is_instance_admin(session, current_user):
        return goals
    membership = access_control_service.get_membership(session, instance.instance_id, current_user.id or 0)
    return [
        goal for goal in goals
        if goal.owner_user_id == (current_user.id or 0) or (goal.visibility_scope == "team" and membership is not None)
    ]


def require_goal_access(session: Session, goal_id: int, current_user: User) -> GoalRecord:
    goal = session.get(GoalRecord, goal_id)
    if goal is None:
        raise HTTPException(status_code=404, detail="goal not found")
    if goal not in visible_goals(session, current_user):
        raise HTTPException(status_code=404, detail="goal not found")
    return goal


def get_plugin_config(session: Session, instance_id: str, plugin_id: str) -> SystemPluginConfig | None:
    return session.exec(
        select(SystemPluginConfig).where(
            SystemPluginConfig.instance_id == instance_id,
            SystemPluginConfig.plugin_id == plugin_id,
        )
    ).first()


def ensure_plugin_enabled(session: Session, instance_id: str, plugin_id: str) -> None:
    plugin = get_plugin_config(session, instance_id, plugin_id)
    if plugin is None or not plugin.is_enabled:
        raise HTTPException(status_code=403, detail=f"plugin {plugin_id} disabled")


def validate_idea_status_transition(current_status: str, next_status: str) -> None:
    if current_status == next_status:
        return
    if next_status not in IDEA_STATUS_TRANSITIONS.get(current_status, set()):
        raise HTTPException(status_code=422, detail=f"invalid idea status transition: {current_status} -> {next_status}")


def visible_topics(session: Session, current_user: User) -> list[TopicRecord]:
    instance = access_control_service.ensure_instance(session)
    topics = session.exec(select(TopicRecord).order_by(TopicRecord.updated_at.desc())).all()
    if instance.edition == "personal" or access_control_service.is_instance_admin(session, current_user):
        return topics
    membership = access_control_service.get_membership(session, instance.instance_id, current_user.id or 0)
    visible: list[TopicRecord] = []
    for topic in topics:
        if topic.owner_user_id == (current_user.id or 0):
            visible.append(topic)
            continue
        if topic.assignee_user_id == (current_user.id or 0):
            visible.append(topic)
            continue
        if topic.review_status != "none" and membership is not None:
            visible.append(topic)
    return visible


def require_topic_access(session: Session, topic_id: int, current_user: User) -> TopicRecord:
    topic = session.get(TopicRecord, topic_id)
    if topic is None or topic not in visible_topics(session, current_user):
        raise HTTPException(status_code=404, detail="topic not found")
    return topic


def require_topic_edit(session: Session, topic_id: int, current_user: User) -> TopicRecord:
    topic = require_topic_access(session, topic_id, current_user)
    if topic.owner_user_id != (current_user.id or 0) and not access_control_service.is_instance_admin(session, current_user):
        raise HTTPException(status_code=403, detail="topic edit not allowed")
    return topic


def record_topic_activity(session: Session, topic: TopicRecord, actor_user_id: int, action: str, detail: str = "") -> None:
    session.add(
        TopicActivityLog(
            topic_id=topic.id or 0,
            instance_id=topic.instance_id,
            actor_user_id=actor_user_id,
            action=action,
            detail=detail,
        )
    )
    session.commit()


def visible_ideas(session: Session, current_user: User) -> list[IdeaRecord]:
    instance = access_control_service.ensure_instance(session)
    ideas = session.exec(select(IdeaRecord).order_by(IdeaRecord.updated_at.desc())).all()
    if instance.edition == "personal" or access_control_service.is_instance_admin(session, current_user):
        return ideas
    membership = access_control_service.get_membership(session, instance.instance_id, current_user.id or 0)
    visible: list[IdeaRecord] = []
    for idea in ideas:
        if idea.owner_user_id == (current_user.id or 0):
            visible.append(idea)
            continue
        if idea.assignee_user_id == (current_user.id or 0):
            visible.append(idea)
            continue
        if idea.visibility_scope == "team" and membership is not None:
            visible.append(idea)
    return visible


def require_idea_access(session: Session, idea_id: int, current_user: User) -> IdeaRecord:
    idea = session.get(IdeaRecord, idea_id)
    if idea is None or idea not in visible_ideas(session, current_user):
        raise HTTPException(status_code=404, detail="idea not found")
    return idea


def require_idea_edit(session: Session, idea_id: int, current_user: User) -> IdeaRecord:
    idea = require_idea_access(session, idea_id, current_user)
    if idea.owner_user_id != (current_user.id or 0) and not access_control_service.is_instance_admin(session, current_user):
        raise HTTPException(status_code=403, detail="idea edit not allowed")
    return idea


def record_idea_activity(session: Session, idea: IdeaRecord, actor_user_id: int, action: str, detail: str = "") -> None:
    session.add(
        IdeaActivityLog(
            idea_id=idea.id or 0,
            instance_id=idea.instance_id,
            actor_user_id=actor_user_id,
            action=action,
            detail=detail,
        )
    )
    session.commit()


def build_topic_discovery_fallback(payload: TopicAiDiscoverRequest) -> list[dict[str, object]]:
    keywords = payload.keywords or ["行业趋势", "用户痛点", "方法论"]
    domain = payload.domain or "内容创作"
    topics: list[dict[str, object]] = []
    for index in range(payload.count):
        seed = keywords[index % len(keywords)]
        topics.append(
            {
                "title": f"{domain} · {seed} 选题 {index + 1}",
                "trend_summary": f"围绕 {seed} 提炼热点切口、实战案例与适合沉淀进知识库的结构。",
                "priority": "high" if index == 0 else "medium",
                "heat_score": max(55, 82 - index * 6),
                "keywords": [seed, domain, "内容创作"],
            }
        )
    return topics


async def generate_topic_candidates(session: Session, payload: TopicAiDiscoverRequest) -> list[dict[str, object]]:
    fallback = build_topic_discovery_fallback(payload)
    if not payload.use_ai:
        return fallback
    prompt = (
        f"请围绕领域 {payload.domain or '内容创作'} 和关键词 {'、'.join(payload.keywords or ['热点趋势'])}，"
        f"生成 {payload.count} 个可执行的内容选题。每个选题输出一行 JSON，包含 title、trend_summary、priority、heat_score、keywords。"
    )
    try:
        result = await ai_service.generate(session, "create", prompt, use_ai=True, use_rag=False)
        parsed: list[dict[str, object]] = []
        for line in result.content.splitlines():
            line = line.strip()
            if not line or not line.startswith("{"):
                continue
            parsed.append(json.loads(line))
        return parsed or fallback
    except Exception:
        return fallback


async def generate_topic_outline(session: Session, payload: TopicAiOutlineRequest) -> str:
    fallback = (
        f"## 选题定位\n- 标题：{payload.title}\n- 领域：{payload.domain or '内容创作'}\n- 关键词：{' / '.join(payload.keywords or ['趋势'])}\n\n"
        f"## 写作结构\n1. 现象与背景\n2. 核心观点\n3. 方法或案例\n4. 行动建议\n\n"
        f"## 开篇思路\n- 从 {payload.trend_summary or '当前趋势变化'} 切入，先给读者一个明确判断，再展开结构。"
    )
    if not payload.use_ai:
        return fallback
    prompt = (
        f"请为选题《{payload.title}》生成一份中文写作大纲，包含选题定位、4 段结构和开篇思路。"
        f"\n领域：{payload.domain}\n关键词：{'、'.join(payload.keywords)}\n补充：{payload.trend_summary}"
    )
    try:
        result = await ai_service.generate(session, "create", prompt, use_ai=True, use_rag=False)
        return result.content or fallback
    except Exception:
        return fallback


def require_goal_edit(session: Session, goal_id: int, current_user: User) -> GoalRecord:
    goal = require_goal_access(session, goal_id, current_user)
    if goal.owner_user_id != (current_user.id or 0) and not access_control_service.is_instance_admin(session, current_user):
        raise HTTPException(status_code=403, detail="goal edit not allowed")
    return goal


def compute_progress_ratio(statuses: list[str]) -> int:
    if not statuses:
        return 0
    done = sum(1 for status in statuses if status == "done")
    return int((done / len(statuses)) * 100)


async def ensure_daily_note_for_date(session: Session, current_user: User, journal_date: str, space_slug: str) -> Note:
    existing = session.exec(select(Note).where(Note.title == journal_date, Note.path == f"05_Daily/{journal_date}.md", Note.is_deleted == False)).first()
    if existing is not None:
        return existing
    created = await persist_note(
        session,
        NoteCreate(
            title=journal_date,
            slug=slugify(journal_date),
            content=f"# {journal_date}\n\n## 今日计划\n\n## 目标联动\n\n## 记录\n\n## 目标复盘\n",
            path=f"05_Daily/{journal_date}.md",
            space_slug=space_slug,
            tags=["daily"],
            links=[],
            source_url="",
        ),
        device_id="goal-system",
        source="goal-daily-bootstrap",
        owner_user_id=current_user.id or 0,
        space_slug=space_slug,
    )
    return created


async def sync_task_line_to_daily_note(
    session: Session,
    current_user: User,
    space_slug: str,
    journal_date: str,
    task: GoalTaskRecord,
    plan: GoalPlanRecord | None,
    goal: GoalRecord | None,
) -> Note:
    note = await ensure_daily_note_for_date(session, current_user, journal_date, space_slug)
    marker = f"<!-- goal-task:{task.id} -->"
    lines = note.content.splitlines()
    filtered = [line for line in lines if marker not in line]
    link_text = f"- [{'x' if task.status == 'done' else ' '}] {task.title}"
    context_suffix = ""
    if goal or plan:
        context_suffix = f" （目标：{goal.title if goal else ''} / 计划：{plan.title if plan else ''}）"
    task_line = f"{link_text}{context_suffix} {marker}".rstrip()
    inserted = False
    result_lines: list[str] = []
    for index, line in enumerate(filtered):
        result_lines.append(line)
        if line.strip() == "## 目标联动":
            result_lines.append("")
            result_lines.append(task_line)
            inserted = True
            remaining = [candidate for candidate in filtered[index + 1 :] if candidate.strip()]
            result_lines.extend(remaining)
            break
    if not inserted:
        result_lines.extend(["", "## 目标联动", "", task_line])
    new_content = "\n".join(result_lines).strip() + "\n"
    updated = await persist_note(
        session,
        NoteCreate(
            title=note.title,
            slug=note.slug,
            content=new_content,
            path=note.path,
            tags=[item for item in note.tags.split(",") if item],
            links=[item for item in note.links.split(",") if item],
            source_url=note.source_url,
        ),
        device_id="goal-system",
        source="goal-task-sync",
        existing_note=note,
    )
    return updated


def build_goal_ai_fallback(payload: GoalAiPlanRequest) -> GoalAiPlanResponse:
    key_results = payload.key_results or ["明确阶段交付", "建立每日执行节奏", "每周复盘纠偏"]
    plans = [
        GoalPlanSuggestion(
            title="目标澄清与拆解",
            summary="把愿景压缩成明确的阶段产出与衡量标准。",
            tasks=["补充目标背景与边界", f"整理关键结果：{key_results[0]}", "确认本周最重要推进项"],
        ),
        GoalPlanSuggestion(
            title="执行节奏建立",
            summary="把阶段计划拆到日任务，形成固定推进节奏。",
            tasks=["列出 3 个本周任务", "设置每日优先级", "安排今日最重要的一步"],
        ),
        GoalPlanSuggestion(
            title="复盘与纠偏",
            summary="把执行记录沉淀为可回看的复盘结论。",
            tasks=["记录完成与阻塞", "总结偏差原因", "调整下阶段计划"],
        ),
    ]
    return GoalAiPlanResponse(goal_summary=payload.vision or payload.title, plans=plans)


async def persist_note(
    session: Session,
    payload: NoteCreate,
    device_id: str,
    source: str,
    existing_note: Note | None = None,
    owner_user_id: int | None = None,
    space_slug: str | None = None,
) -> Note:
    note = existing_note or Note(
        title=payload.title,
        slug=payload.slug,
        content=payload.content,
        path=payload.path,
        parent_path=str(Path(payload.path).parent).replace("\\", "/"),
        source_url=payload.source_url,
    )

    note.title = payload.title
    note.slug = payload.slug
    note.content = payload.content
    note.path = payload.path
    note.parent_path = str(Path(payload.path).parent).replace("\\", "/") if "/" in payload.path else ""
    note.source_url = payload.source_url
    note.updated_at = datetime.now(UTC)
    note.is_deleted = False

    if existing_note is None:
        session.add(note)

    session.flush()
    markdown_index_service.reindex_note(session, note)
    if existing_note is None and owner_user_id:
        access_control_service.ensure_note_policy(session, note.slug, owner_user_id, space_slug or "")
    session.commit()
    session.refresh(note)
    vault_sync_service.save_note_file(payload)
    await vault_sync_service.queue_sync_event(
        session,
        note_slug=note.slug,
        device_id=device_id,
        event_type="upsert",
        version=note.version,
        path=note.path,
        payload={"source": source, "title": note.title},
    )
    return note


@router.post("/auth/login", response_model=TokenResponse)
def login(payload: LoginRequest, session: Session = Depends(get_session)) -> TokenResponse:
    user = session.exec(select(User).where(User.username == payload.username)).first()
    if user is None or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=401, detail="invalid credentials")
    return TokenResponse(access_token=create_access_token(user.username), username=user.username)


@router.get("/auth/me", response_model=UserRead)
def me(session: Session = Depends(get_session), current_user: User = Depends(get_current_user)) -> UserRead:
    return serialize_user(session, current_user)


@router.get("/instance/config", response_model=InstanceConfigRead)
def instance_config(session: Session = Depends(get_session)) -> InstanceConfigRead:
    return serialize_instance(access_control_service.ensure_instance(session))


@router.get("/tree", response_model=list[FileTreeNode])
def get_tree(session: Session = Depends(get_session), current_user: User = Depends(get_current_user)) -> list[FileTreeNode]:
    notes = access_control_service.visible_notes(session, current_user)
    return [FileTreeNode.model_validate(node) for node in access_control_service.build_tree(notes)]


@router.get("/notes", response_model=list[NoteRead])
def list_notes(
    include_deleted: bool = False,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[NoteRead]:
    notes = access_control_service.visible_notes(session, current_user, include_deleted=include_deleted)
    return [serialize_note(note) for note in notes]


@router.get("/notes/{slug}", response_model=NoteRead)
def get_note(slug: str, session: Session = Depends(get_session), current_user: User = Depends(get_current_user)) -> NoteRead:
    return serialize_note(require_note_access(session, slug, current_user))


@router.post("/notes", response_model=NoteRead)
async def create_note(
    payload: NoteCreate,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> NoteRead:
    existing = session.exec(select(Note).where(Note.slug == payload.slug)).first()
    if existing:
        raise HTTPException(status_code=409, detail="slug already exists")

    instance = access_control_service.ensure_instance(session)
    target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, payload.space_slug)
    note = await persist_note(
        session,
        payload,
        device_id="api",
        source="create",
        owner_user_id=current_user.id or 0,
        space_slug=target_space.slug,
    )
    return serialize_note(note)


@router.put("/notes/{slug}", response_model=NoteRead)
async def update_note(
    slug: str,
    payload: NoteUpdate,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> NoteRead:
    note = require_note_edit(session, slug, current_user)
    if payload.previous_version and note.version != payload.previous_version:
        conflict_payload = vault_sync_service.create_conflict_copy(
            NoteCreate(
                title=payload.title,
                slug=slugify(f"{slug}-conflict"),
                content=payload.content,
                path=payload.path,
                tags=payload.tags,
                links=payload.links,
            ),
            "web",
        )
        conflict = await persist_note(session, conflict_payload, device_id="web", source="conflict-copy")
        raise HTTPException(
            status_code=409,
            detail={"message": "version conflict", "conflict_note": serialize_note(conflict).model_dump()},
        )

    updated = await persist_note(
        session,
        NoteCreate(
            title=payload.title,
            slug=slug,
            content=payload.content,
            path=payload.path,
            tags=payload.tags,
            links=payload.links,
        ),
        device_id="web",
        source="update",
        existing_note=note,
    )
    return serialize_note(updated)


@router.put("/notes/{slug}/move", response_model=NoteRead)
async def move_note(
    slug: str,
    payload: MoveNoteRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> NoteRead:
    note = require_note_edit(session, slug, current_user)
    if payload.previous_version and note.version != payload.previous_version:
        raise HTTPException(status_code=409, detail="version conflict")

    source_path = note.path
    target_path = payload.target_path.strip().replace("\\", "/")
    if not target_path.endswith(".md"):
        target_path = f"{target_path}.md"

    vault_sync_service.move_note_file(note.path, target_path)
    note.path = target_path
    note.parent_path = target_path.rsplit("/", 1)[0] if "/" in target_path else ""
    note.updated_at = datetime.now(UTC)
    markdown_index_service.update_search_index(session, note)
    session.add(note)
    session.commit()
    session.refresh(note)
    await vault_sync_service.queue_sync_event(
        session,
        note_slug=note.slug,
        device_id="web",
        event_type="move",
        version=note.version,
        path=source_path,
        target_path=target_path,
        payload={"title": note.title},
    )
    return serialize_note(note)


@router.delete("/notes/{slug}")
async def delete_note(
    slug: str,
    payload: DeleteRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict[str, str]:
    note = require_note_edit(session, slug, current_user)
    if payload.previous_version and note.version != payload.previous_version:
        raise HTTPException(status_code=409, detail="version conflict")

    note.is_deleted = True
    note.updated_at = datetime.now(UTC)
    markdown_index_service.delete_note_index(note, session)
    session.add(note)
    session.commit()
    vault_sync_service.delete_note_file(note.path)
    await vault_sync_service.queue_sync_event(
        session,
        note_slug=note.slug,
        device_id="web",
        event_type="delete",
        version=note.version,
        path=note.path,
    )
    return {"status": "deleted"}


@router.post("/folders")
async def create_folder(
    payload: FolderCreateRequest,
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> dict[str, str]:
    vault_sync_service.create_folder(payload.path)
    await vault_sync_service.queue_sync_event(
        session,
        note_slug="",
        device_id="web",
        event_type="folder-create",
        version="folder",
        path=payload.path,
    )
    return {"status": "created"}


@router.put("/folders/move")
async def move_folder(
    payload: FolderMoveRequest,
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> dict[str, str]:
    vault_sync_service.move_folder(payload.source_path, payload.target_path, session)
    session.commit()
    await vault_sync_service.queue_sync_event(
        session,
        note_slug="",
        device_id="web",
        event_type="folder-move",
        version="folder",
        path=payload.source_path,
        target_path=payload.target_path,
    )
    return {"status": "moved"}


@router.api_route("/folders", methods=["DELETE"])
async def delete_folder(
    payload: FolderDeleteRequest,
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> dict[str, str]:
    deleted_notes = vault_sync_service.delete_folder(payload.path, session)
    for note in deleted_notes:
        note.is_deleted = True
        markdown_index_service.delete_note_index(note, session)
    session.commit()
    await vault_sync_service.queue_sync_event(
        session,
        note_slug="",
        device_id="web",
        event_type="folder-delete",
        version="folder",
        path=payload.path,
    )
    return {"status": "deleted"}


@router.get("/links/{slug}", response_model=BacklinkBundle)
def backlinks(slug: str, session: Session = Depends(get_session), current_user: User = Depends(get_current_user)) -> BacklinkBundle:
    require_note_access(session, slug, current_user)
    visible_slugs = access_control_service.visible_note_slugs(session, current_user)
    outgoing = session.exec(select(NoteLink).where(NoteLink.source_slug == slug)).all()
    incoming = session.exec(select(NoteLink).where(NoteLink.target_slug == slug)).all()
    return BacklinkBundle(
        note_slug=slug,
        outgoing=[link.model_dump() for link in outgoing if link.target_slug in visible_slugs],
        incoming=[link.model_dump() for link in incoming if link.source_slug in visible_slugs],
    )


@router.get("/tags", response_model=list[TagSummary])
def tags(session: Session = Depends(get_session), current_user: User = Depends(get_current_user)) -> list[TagSummary]:
    counter = access_control_service.count_tags_for_visible_notes(session, current_user)
    return [TagSummary(tag=tag, count=count) for tag, count in sorted(counter.items())]


@router.get("/search", response_model=SearchResponse)
def search(
    q: str = Query(..., min_length=2),
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> SearchResponse:
    return search_service.search(session, current_user, q)


@router.put("/instance/bootstrap", response_model=InstanceConfigRead)
def bootstrap_instance(
    payload: InstanceBootstrapRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> InstanceConfigRead:
    instance = require_instance_admin(session, current_user)
    if payload.edition == "team" and not access_control_service.verify_team_authorization_code(payload.authorization_code):
        raise HTTPException(status_code=403, detail="invalid team authorization code")

    instance.instance_name = payload.instance_name
    instance.deployment_mode = payload.deployment_mode
    instance.edition = payload.edition
    instance.desktop_host_enabled = payload.deployment_mode == "desktop"
    instance.server_host_enabled = payload.deployment_mode == "server"
    instance.auth_required = payload.edition == "team"
    instance.team_license_verified = payload.edition == "personal" or bool(payload.authorization_code)
    instance.team_authorization_code_hash = (
        access_control_service.hash_authorization_code(payload.authorization_code) if payload.edition == "team" else ""
    )
    instance.is_initialized = True
    instance.updated_at = datetime.now(UTC)
    session.add(instance)
    session.commit()
    session.refresh(instance)
    return serialize_instance(instance)


@router.get("/instance/spaces", response_model=list[KnowledgeSpaceRead])
def list_spaces(session: Session = Depends(get_session), current_user: User = Depends(get_current_user)) -> list[KnowledgeSpaceRead]:
    instance = access_control_service.ensure_instance(session)
    owned_spaces = session.exec(
        select(KnowledgeSpace).where(KnowledgeSpace.instance_id == instance.instance_id, KnowledgeSpace.owner_user_id == (current_user.id or 0))
    ).all()
    return [serialize_space(space) for space in owned_spaces]


@router.post("/instance/spaces", response_model=KnowledgeSpaceRead)
def create_space(
    payload: KnowledgeSpaceCreateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> KnowledgeSpaceRead:
    instance = access_control_service.ensure_instance(session)
    slug = slugify(payload.slug)
    existing = session.exec(select(KnowledgeSpace).where(KnowledgeSpace.slug == slug)).first()
    if existing is not None:
        raise HTTPException(status_code=409, detail="space slug already exists")
    if instance.edition == "team" and payload.visibility == "team" and not instance.team_license_verified:
        raise HTTPException(status_code=403, detail="team license not verified")

    space = KnowledgeSpace(
        instance_id=instance.instance_id,
        owner_user_id=current_user.id or 0,
        slug=slug,
        name=payload.name,
        visibility=payload.visibility,
        is_default=False,
    )
    session.add(space)
    session.commit()
    session.refresh(space)
    access_control_service.ensure_membership(session, instance.instance_id, current_user, space.slug, "admin")
    return serialize_space(space)


@router.get("/instance/team/members", response_model=list[TeamMemberRead])
def list_team_members(session: Session = Depends(get_session), current_user: User = Depends(get_current_user)) -> list[TeamMemberRead]:
    instance = require_instance_admin(session, current_user)
    memberships = session.exec(
        select(TeamMembership).where(TeamMembership.instance_id == instance.instance_id, TeamMembership.is_active == True)
    ).all()
    users = {user.id: user for user in session.exec(select(User).where(User.id.in_([membership.user_id for membership in memberships]))).all()}
    spaces = {space.owner_user_id: space for space in session.exec(select(KnowledgeSpace).where(KnowledgeSpace.instance_id == instance.instance_id, KnowledgeSpace.is_default == True)).all()}
    return [
        TeamMemberRead(
            user_id=membership.user_id,
            username=users.get(membership.user_id).username if users.get(membership.user_id) else f"user-{membership.user_id}",
            display_name=users.get(membership.user_id).display_name if users.get(membership.user_id) else membership.role,
            role=membership.role,
            default_space_slug=spaces.get(membership.user_id).slug if spaces.get(membership.user_id) else membership.space_slug,
            content_visibility_scope=membership.content_visibility_scope,
        )
        for membership in memberships
    ]


@router.post("/instance/team/invites", response_model=TeamInviteRead)
def create_team_invite(
    payload: TeamInviteCreateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> TeamInviteRead:
    instance = require_instance_admin(session, current_user)
    if instance.edition != "team" or not instance.team_license_verified:
        raise HTTPException(status_code=403, detail="team edition is not enabled")

    invite = TeamInvite(
        instance_id=instance.instance_id,
        invite_code=secrets.token_urlsafe(18),
        created_by_user_id=current_user.id or 0,
        role=payload.role,
        status="pending",
        expires_at=datetime.now(UTC) + timedelta(hours=payload.expires_in_hours),
    )
    session.add(invite)
    session.commit()
    session.refresh(invite)
    base_url = settings.team_invite_base_url.rstrip("/") if settings.team_invite_base_url else ""
    invite_link = f"{base_url}?invite={invite.invite_code}" if base_url else invite.invite_code
    return TeamInviteRead(
        invite_code=invite.invite_code,
        role=invite.role,
        status=invite.status,
        expires_at=invite.expires_at,
        invite_link=invite_link,
    )


@router.post("/instance/team/invites/accept", response_model=UserRead)
def accept_team_invite(payload: TeamInviteAcceptRequest, session: Session = Depends(get_session)) -> UserRead:
    instance = access_control_service.ensure_instance(session)
    if instance.edition != "team" or not instance.team_license_verified:
        raise HTTPException(status_code=403, detail="team edition is not enabled")

    invite = session.exec(select(TeamInvite).where(TeamInvite.invite_code == payload.invite_code)).first()
    if invite is None or invite.status != "pending":
        raise HTTPException(status_code=404, detail="invite not found")
    expires_at = normalize_utc(invite.expires_at)
    if expires_at and expires_at < datetime.now(UTC):
        raise HTTPException(status_code=410, detail="invite expired")
    if session.exec(select(User).where(User.username == payload.username)).first() is not None:
        raise HTTPException(status_code=409, detail="username already exists")

    user = User(
        username=payload.username,
        password_hash=hash_password(payload.password),
        display_name=payload.display_name,
    )
    session.add(user)
    session.commit()
    session.refresh(user)
    default_space = access_control_service.ensure_user_default_space(session, instance.instance_id, user)
    access_control_service.ensure_membership(
        session,
        instance.instance_id,
        user,
        default_space.slug,
        "manager" if invite.role == "manager" else "member",
    )
    invite.status = "accepted"
    session.add(invite)
    session.commit()
    return serialize_user(session, user)


@router.post("/sync/upload")
async def sync_upload(
    payload: SyncUploadRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict[str, str]:
    vault_sync_service.register_device(session, payload.device_id, payload.device_type)
    note = session.exec(select(Note).where(Note.slug == payload.note.slug)).first()
    if note and not access_control_service.can_edit_note(session, current_user, note.slug):
        raise HTTPException(status_code=403, detail="note edit not allowed")
    if note and payload.previous_version and note.version != payload.previous_version:
        conflict_payload = vault_sync_service.create_conflict_copy(payload.note, payload.device_id)
        instance = access_control_service.ensure_instance(session)
        default_space = access_control_service.ensure_user_default_space(session, instance.instance_id, current_user)
        conflict_note = await persist_note(
            session,
            conflict_payload,
            payload.device_id,
            source="sync-conflict",
            owner_user_id=current_user.id or 0,
            space_slug=default_space.slug,
        )
        return {"status": "conflict", "desktop_sync": conflict_note.slug}

    instance = access_control_service.ensure_instance(session)
    default_space = access_control_service.ensure_user_default_space(session, instance.instance_id, current_user)
    note = await persist_note(
        session,
        payload.note,
        payload.device_id,
        payload.source,
        existing_note=note,
        owner_user_id=current_user.id or 0,
        space_slug=default_space.slug,
    )
    return {"status": "accepted", "desktop_sync": note.version}


@router.get("/sync/events", response_model=list[SyncEventRead])
def get_sync_events(
    device_id: str,
    after_id: int = 0,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[SyncEventRead]:
    visible_slugs = access_control_service.visible_note_slugs(session, current_user, include_deleted=True)
    events = session.exec(
        select(SyncEvent).where(SyncEvent.id > after_id).order_by(SyncEvent.id.asc())
    ).all()
    vault_sync_service.register_device(session, device_id, "desktop")
    return [serialize_event(event) for event in events if event.note_slug in {"", *visible_slugs}]


@router.post("/sync/ack")
def sync_ack(
    payload: SyncAckRequest,
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> dict[str, str]:
    events = session.exec(select(SyncEvent).where(SyncEvent.id.in_(payload.event_ids))).all()
    for event in events:
        event.status = "acked"
        event.acknowledged_by = payload.device_id
    session.commit()
    return {"status": "acked"}


@router.get("/sync/status", response_model=SyncStatusResponse)
def sync_status(session: Session = Depends(get_session), _: User = Depends(get_current_user)) -> SyncStatusResponse:
    events = session.exec(select(SyncEvent).order_by(SyncEvent.id.desc())).all()
    last_event_id = events[0].id if events else None
    pending = sum(1 for event in events if event.status != "acked")
    return SyncStatusResponse(
        pending_events=pending,
        connected_desktop_devices=sync_connection_manager.connected_desktop_devices(),
        last_event_id=last_event_id,
    )


@router.websocket("/sync/ws/{device_id}")
async def sync_ws(websocket: WebSocket, device_id: str) -> None:
    token = websocket.query_params.get("token")
    try:
        decode_token(token or "")
    except Exception:
        await websocket.close(code=4401)
        return

    await sync_connection_manager.connect(device_id, websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        await sync_connection_manager.disconnect(device_id, websocket)


@router.post("/clip", response_model=WebClipResponse)
async def clip_url(
    payload: WebClipRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> WebClipResponse:
    existing_clip = session.exec(select(SavedClip).where(SavedClip.url == payload.url)).first()
    if existing_clip and payload.save_to_note and access_control_service.can_read_note(session, current_user, existing_clip.note_slug):
        note = require_note(session, existing_clip.note_slug)
        return WebClipResponse(
            note=serialize_note(note),
            extracted_title=note.title,
            source_url=payload.url,
            summary=note.summary,
            preview_content=note.content,
            saved=True,
        )

    title, markdown = await clip_service.fetch_markdown(payload.url)
    summary = ""
    content_to_save = markdown
    note_title = title
    if payload.summarize_with_ai:
        ai_result = await ai_service.summarize_external_content(
            session,
            title=title,
            content=markdown,
            prompt=payload.summary_prompt,
            use_ai=payload.use_ai,
            use_rag=payload.use_rag,
            visible_note_slugs=access_control_service.visible_note_slugs(session, current_user),
        )
        summary = ai_result.content
        note_title = f"{title} 摘要"
        content_to_save = (
            f"# {note_title}\n\n来源链接: {payload.url}\n\n## AI 提炼\n\n{summary}\n\n## 原始摘录\n\n{markdown[:4000]}"
        )

    if not payload.save_to_note:
        return WebClipResponse(
            note=None,
            extracted_title=note_title,
            source_url=payload.url,
            summary=summary,
            preview_content=content_to_save,
            saved=False,
        )

    unique_title, unique_path = ai_service.build_unique_note_identity(session, payload.target_folder, note_title)
    note_payload = vault_sync_service.materialize_note(
        unique_title,
        unique_path,
        content_to_save,
        source_url=payload.url,
    )
    instance = access_control_service.ensure_instance(session)
    target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, payload.space_slug)
    note = await persist_note(
        session,
        note_payload,
        payload.device_id,
        source="clip",
        owner_user_id=current_user.id or 0,
        space_slug=target_space.slug,
    )
    note.summary = summary
    session.add(note)
    if existing_clip is None:
        session.add(SavedClip(url=payload.url, note_slug=note.slug, title=title))
    session.commit()
    session.refresh(note)
    return WebClipResponse(
        note=serialize_note(note),
        extracted_title=note_title,
        source_url=payload.url,
        summary=summary,
        preview_content=content_to_save,
        saved=True,
    )


@router.post("/clip/save-preview", response_model=WebClipResponse)
async def save_clip_preview(
    payload: WebClipSaveRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> WebClipResponse:
    existing_clip = session.exec(select(SavedClip).where(SavedClip.url == payload.source_url)).first()
    if existing_clip and access_control_service.can_read_note(session, current_user, existing_clip.note_slug):
        note = require_note(session, existing_clip.note_slug)
        return WebClipResponse(
            note=serialize_note(note),
            extracted_title=note.title,
            source_url=payload.source_url,
            summary=note.summary,
            preview_content=note.content,
            saved=True,
        )

    unique_title, unique_path = ai_service.build_unique_note_identity(session, payload.target_folder, payload.title)
    note_payload = vault_sync_service.materialize_note(
        unique_title,
        unique_path,
        payload.content,
        source_url=payload.source_url,
    )
    instance = access_control_service.ensure_instance(session)
    target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, payload.space_slug)
    note = await persist_note(
        session,
        note_payload,
        payload.device_id,
        source="clip-preview-save",
        owner_user_id=current_user.id or 0,
        space_slug=target_space.slug,
    )
    note.summary = payload.summary
    session.add(note)
    if existing_clip is None:
        session.add(SavedClip(url=payload.source_url, note_slug=note.slug, title=payload.title))
    session.commit()
    session.refresh(note)
    return WebClipResponse(
        note=serialize_note(note),
        extracted_title=unique_title,
        source_url=payload.source_url,
        summary=payload.summary,
        preview_content=payload.content,
        saved=True,
    )


@router.post("/rag/query", response_model=RagQueryResponse)
def rag_query(
    payload: RagQueryRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> RagQueryResponse:
    return rag_index_service.query(
        session,
        payload.query,
        payload.top_k,
        visible_note_slugs=access_control_service.visible_note_slugs(session, current_user),
    )


@router.post("/rag/rebuild")
def rag_rebuild(session: Session = Depends(get_session), current_user: User = Depends(get_current_user)) -> dict[str, int]:
    notes = access_control_service.visible_notes(session, current_user)
    for note in notes:
        markdown_index_service.reindex_note(session, note)
    session.commit()
    return {"reindexed": len(notes)}


@router.post("/ai/generate", response_model=AiGenerateResponse)
async def ai_generate(
    payload: AiGenerateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> AiGenerateResponse:
    if payload.note_slug:
        require_note_access(session, payload.note_slug, current_user)
    try:
        response = await ai_service.generate(
            session,
            payload.action,
            payload.prompt,
            payload.note_slug,
            use_ai=payload.use_ai,
            use_rag=payload.use_rag,
            visible_note_slugs=access_control_service.visible_note_slugs(session, current_user),
        )
    except RuntimeError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

    if payload.save_as_note:
        saved = ai_service.save_generated_note(session, f"AI {payload.action}", payload.target_folder, response.content)
        markdown_index_service.reindex_note(session, saved)
        instance = access_control_service.ensure_instance(session)
        target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, payload.space_slug)
        access_control_service.ensure_note_policy(session, saved.slug, current_user.id or 0, target_space.slug)
        session.commit()
        session.refresh(saved)
        note_payload = NoteCreate(
            title=saved.title,
            slug=saved.slug,
            content=saved.content,
            path=saved.path,
            tags=[item for item in saved.tags.split(",") if item],
            links=[item for item in saved.links.split(",") if item],
        )
        vault_sync_service.save_note_file(note_payload)
        await vault_sync_service.queue_sync_event(
            session,
            note_slug=saved.slug,
            device_id="ai",
            event_type="upsert",
            version=compute_version(saved.content),
            path=saved.path,
            payload={"source": "ai", "title": saved.title},
        )
        response.saved_note = serialize_note(saved)
    return response


@router.get("/ai/providers/catalog", response_model=AiProviderCatalogResponse)
def ai_provider_catalog(
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> AiProviderCatalogResponse:
    return AiProviderCatalogResponse(presets=ai_service.list_provider_presets(), current=ai_service.get_current_provider(session))


@router.put("/ai/providers/current", response_model=AiProviderConfigRead)
def ai_provider_upsert(
    payload: AiProviderConfigUpsert,
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> AiProviderConfigRead:
    return ai_service.upsert_provider(session, payload)


@router.post("/documents/upload", response_model=DocumentUploadResponse)
async def upload_document(
    file: UploadFile = File(...),
    summarize_with_ai: bool = Form(True),
    use_ai: bool = Form(True),
    use_rag: bool = Form(False),
    save_to_note: bool = Form(True),
    target_folder: str = Form("03_Resources"),
    space_slug: str | None = Form(None),
    summary_prompt: str = Form("请提炼这份资料的核心信息、结构脉络和可执行建议。"),
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> DocumentUploadResponse:
    content = await file.read()
    title, extracted_text = document_ingest_service.extract_text(file.filename or "uploaded-document.txt", content)
    if not extracted_text:
        raise HTTPException(status_code=400, detail="document content is empty")

    summary = ""
    if summarize_with_ai:
        ai_result = await ai_service.summarize_external_content(
            session,
            title=title,
            content=extracted_text,
            prompt=summary_prompt,
            use_ai=use_ai,
            use_rag=use_rag,
            visible_note_slugs=access_control_service.visible_note_slugs(session, current_user),
        )
        summary = ai_result.content

    saved_note = None
    if save_to_note:
        body = (
            f"# {title}\n\n## AI 提炼\n\n{summary}\n\n## 原文摘录\n\n{extracted_text[:5000]}"
            if summary
            else f"# {title}\n\n{extracted_text}"
        )
        saved = ai_service.save_generated_note(session, title, target_folder, body)
        markdown_index_service.reindex_note(session, saved)
        instance = access_control_service.ensure_instance(session)
        target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, space_slug)
        access_control_service.ensure_note_policy(session, saved.slug, current_user.id or 0, target_space.slug)
        session.commit()
        session.refresh(saved)
        await vault_sync_service.queue_sync_event(
            session,
            note_slug=saved.slug,
            device_id="upload",
            event_type="upsert",
            version=compute_version(saved.content),
            path=saved.path,
            payload={"source": "document-upload", "title": saved.title},
        )
        saved_note = serialize_note(saved)

    return DocumentUploadResponse(
        filename=file.filename or "uploaded-document",
        extracted_title=title,
        content_preview=extracted_text[:500],
        summary=summary,
        saved_note=saved_note,
    )


@router.get("/notes/{slug}/sharing", response_model=NoteSharingRead)
def get_note_sharing(
    slug: str,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> NoteSharingRead:
    require_note_access(session, slug, current_user)
    policy = session.exec(select(NoteAccessPolicy).where(NoteAccessPolicy.note_slug == slug)).first()
    if policy is None:
        raise HTTPException(status_code=404, detail="sharing policy not found")
    return NoteSharingRead(
        note_slug=policy.note_slug,
        owner_user_id=policy.owner_user_id,
        space_slug=policy.space_slug,
        visibility_scope=policy.visibility_scope,
        shared_member_ids=sorted(access_control_service.parse_shared_member_ids(policy.shared_member_ids)),
    )


@router.put("/notes/{slug}/sharing", response_model=NoteSharingRead)
def update_note_sharing(
    slug: str,
    payload: NoteSharingUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> NoteSharingRead:
    require_note_edit(session, slug, current_user)
    policy = session.exec(select(NoteAccessPolicy).where(NoteAccessPolicy.note_slug == slug)).first()
    if policy is None:
        instance = access_control_service.ensure_instance(session)
        default_space = access_control_service.ensure_user_default_space(session, instance.instance_id, current_user)
        policy = access_control_service.ensure_note_policy(session, slug, current_user.id or 0, default_space.slug)

    instance = access_control_service.ensure_instance(session)
    if payload.visibility_scope != "private" and instance.edition != "team":
        raise HTTPException(status_code=403, detail="sharing requires team edition")

    policy.visibility_scope = payload.visibility_scope
    policy.shared_member_ids = access_control_service.serialize_shared_member_ids(payload.shared_member_ids)
    policy.updated_at = datetime.now(UTC)
    session.add(policy)
    session.commit()
    session.refresh(policy)
    return NoteSharingRead(
        note_slug=policy.note_slug,
        owner_user_id=policy.owner_user_id,
        space_slug=policy.space_slug,
        visibility_scope=policy.visibility_scope,
        shared_member_ids=sorted(access_control_service.parse_shared_member_ids(policy.shared_member_ids)),
    )


@router.get("/passwords/config", response_model=PasswordVaultConfigRead)
def get_password_vault_config(
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> PasswordVaultConfigRead:
    instance = access_control_service.ensure_instance(session)
    config = session.exec(select(PasswordVaultConfig).where(PasswordVaultConfig.instance_id == instance.instance_id)).first()
    return PasswordVaultConfigRead(is_initialized=config is not None)


@router.post("/passwords/config/setup", response_model=PasswordVaultConfigRead)
def setup_password_vault(
    payload: PasswordVaultSetupRequest,
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> PasswordVaultConfigRead:
    instance = access_control_service.ensure_instance(session)
    existing = session.exec(select(PasswordVaultConfig).where(PasswordVaultConfig.instance_id == instance.instance_id)).first()
    if existing is not None:
        raise HTTPException(status_code=409, detail="password vault already initialized")
    verifier_salt, verifier_hash = password_manager_service.create_verifier(payload.master_password, instance.instance_id)
    session.add(PasswordVaultConfig(instance_id=instance.instance_id, verifier_salt=verifier_salt, verifier_hash=verifier_hash))
    session.commit()
    return PasswordVaultConfigRead(is_initialized=True)


@router.post("/passwords/config/verify")
def verify_password_vault(
    payload: PasswordVaultVerifyRequest,
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> dict[str, bool]:
    instance = access_control_service.ensure_instance(session)
    config = require_password_vault_config(session, instance.instance_id)
    return {"verified": password_manager_service.verify_master_password(config, payload.master_password, instance.instance_id)}


@router.post("/passwords/generate", response_model=PasswordGenerateResponse)
def generate_password(payload: PasswordGenerateRequest) -> PasswordGenerateResponse:
    return PasswordGenerateResponse(
        password=password_manager_service.build_password(
            payload.length,
            payload.include_symbols,
            payload.include_numbers,
            payload.include_uppercase,
        )
    )


@router.get("/passwords", response_model=list[PasswordEntryRead])
def list_password_entries(
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[PasswordEntryRead]:
    return [serialize_password_entry(entry) for entry in visible_password_entries(session, current_user)]


@router.post("/passwords", response_model=PasswordEntryRead)
def create_password_entry(
    payload: PasswordEntryCreateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> PasswordEntryRead:
    instance = access_control_service.ensure_instance(session)
    config = require_password_vault_config(session, instance.instance_id)
    if not password_manager_service.verify_master_password(config, payload.master_password, instance.instance_id):
        raise HTTPException(status_code=403, detail="invalid master password")
    target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, payload.space_slug)
    encrypted_password, encryption_salt = password_manager_service.encrypt_password(payload.password, payload.master_password, instance.instance_id)
    entry = PasswordVaultEntry(
        instance_id=instance.instance_id,
        owner_user_id=current_user.id or 0,
        space_slug=target_space.slug,
        title=payload.title,
        username=payload.username,
        encrypted_password=encrypted_password,
        encryption_salt=encryption_salt,
        category=payload.category,
        url=payload.url,
        website=payload.website,
        notes=payload.notes,
        linked_note_slug=payload.linked_note_slug,
        vault_scope=payload.vault_scope if instance.edition == "team" else "private",
        shared_member_ids=serialize_id_csv(payload.shared_member_ids),
        editor_member_ids=serialize_id_csv(payload.editor_member_ids),
    )
    session.add(entry)
    session.commit()
    session.refresh(entry)
    record_password_audit(session, entry, current_user.id or 0, "create", payload.title)
    return serialize_password_entry(entry)


@router.put("/passwords/{entry_id}", response_model=PasswordEntryRead)
def update_password_entry(
    entry_id: int,
    payload: PasswordEntryUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> PasswordEntryRead:
    entry = session.get(PasswordVaultEntry, entry_id)
    if entry is None or entry.is_deleted:
        raise HTTPException(status_code=404, detail="password entry not found")
    ensure_password_edit_access(session, current_user, entry)
    config = require_password_vault_config(session, entry.instance_id)
    if not password_manager_service.verify_master_password(config, payload.master_password, entry.instance_id):
        raise HTTPException(status_code=403, detail="invalid master password")
    entry.title = payload.title
    entry.username = payload.username
    entry.category = payload.category
    entry.url = payload.url
    entry.website = payload.website
    entry.notes = payload.notes
    entry.linked_note_slug = payload.linked_note_slug
    entry.vault_scope = payload.vault_scope
    entry.shared_member_ids = serialize_id_csv(payload.shared_member_ids)
    entry.editor_member_ids = serialize_id_csv(payload.editor_member_ids)
    entry.updated_at = datetime.now(UTC)
    if payload.password:
        entry.encrypted_password, entry.encryption_salt = password_manager_service.encrypt_password(
            payload.password,
            payload.master_password,
            entry.instance_id,
        )
    session.add(entry)
    session.commit()
    session.refresh(entry)
    record_password_audit(session, entry, current_user.id or 0, "update", payload.title)
    return serialize_password_entry(entry)


@router.delete("/passwords/{entry_id}")
def delete_password_entry(
    entry_id: int,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict[str, str]:
    entry = session.get(PasswordVaultEntry, entry_id)
    if entry is None or entry.is_deleted:
        raise HTTPException(status_code=404, detail="password entry not found")
    ensure_password_edit_access(session, current_user, entry)
    entry.is_deleted = True
    entry.updated_at = datetime.now(UTC)
    session.add(entry)
    session.commit()
    record_password_audit(session, entry, current_user.id or 0, "delete", entry.title)
    return {"status": "deleted"}


@router.post("/passwords/{entry_id}/reveal", response_model=PasswordEntrySecretRead)
def reveal_password_entry(
    entry_id: int,
    payload: PasswordRevealRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> PasswordEntrySecretRead:
    entry = session.get(PasswordVaultEntry, entry_id)
    if entry is None or entry.is_deleted:
        raise HTTPException(status_code=404, detail="password entry not found")
    ensure_password_read_access(session, current_user, entry)
    config = require_password_vault_config(session, entry.instance_id)
    if not password_manager_service.verify_master_password(config, payload.master_password, entry.instance_id):
        raise HTTPException(status_code=403, detail="invalid master password")
    password = password_manager_service.decrypt_password(entry, payload.master_password)
    password_manager_service.touch_entry(entry)
    session.add(entry)
    session.commit()
    record_password_audit(session, entry, current_user.id or 0, "reveal", entry.title)
    return PasswordEntrySecretRead(entry=serialize_password_entry(entry), password=password)


@router.post("/passwords/{entry_id}/copy")
def audit_password_copy(
    entry_id: int,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict[str, str]:
    entry = session.get(PasswordVaultEntry, entry_id)
    if entry is None or entry.is_deleted:
        raise HTTPException(status_code=404, detail="password entry not found")
    ensure_password_read_access(session, current_user, entry)
    record_password_audit(session, entry, current_user.id or 0, "copy", entry.title)
    return {"status": "recorded"}


@router.get("/passwords/{entry_id}/audits", response_model=list[PasswordAuditRead])
def list_password_audits(
    entry_id: int,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[PasswordAuditRead]:
    entry = session.get(PasswordVaultEntry, entry_id)
    if entry is None or entry.is_deleted:
        raise HTTPException(status_code=404, detail="password entry not found")
    ensure_password_read_access(session, current_user, entry)
    audits = session.exec(select(PasswordAccessAudit).where(PasswordAccessAudit.entry_id == entry_id).order_by(PasswordAccessAudit.created_at.desc())).all()
    return [
        PasswordAuditRead(
            id=audit.id or 0,
            entry_id=audit.entry_id,
            actor_user_id=audit.actor_user_id,
            action=audit.action,
            detail=audit.detail,
            created_at=to_utc_iso(audit.created_at),
        )
        for audit in audits
    ]


@router.get("/search/unified", response_model=UnifiedSearchResponse)
def unified_search(
    q: str = Query(..., min_length=2),
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> UnifiedSearchResponse:
    note_results = search_service.search(session, current_user, q).results
    password_results = [
        UnifiedSearchItem(
            kind="password",
            title=entry.title,
            path=f"密码库 / {entry.category}",
            snippet=" / ".join(part for part in [entry.username, entry.website or entry.url, entry.notes[:60]] if part),
            password_id=entry.id or 0,
        )
        for entry in visible_password_entries(session, current_user)
        if q.lower() in " ".join([entry.title, entry.username, entry.website, entry.url, entry.notes, entry.category]).lower()
    ]
    note_items = [
        UnifiedSearchItem(kind="note", title=item.title, path=item.path, snippet=item.snippet, note_slug=item.note_slug)
        for item in note_results
    ]
    return UnifiedSearchResponse(results=[*note_items, *password_results][:20])


@router.post("/goals/ai/plan", response_model=GoalAiPlanResponse)
async def build_goal_plan(
    payload: GoalAiPlanRequest,
    session: Session = Depends(get_session),
    _: User = Depends(get_current_user),
) -> GoalAiPlanResponse:
    fallback = build_goal_ai_fallback(payload)
    if not payload.use_ai:
        return fallback
    prompt = (
        f"请将目标拆解为 3 个阶段计划，每个阶段给一句摘要和 3 个任务。\n"
        f"目标：{payload.title}\n愿景：{payload.vision}\n关键结果：{'；'.join(payload.key_results)}"
    )
    try:
        ai_result = await ai_service.generate(session, "create", prompt, use_ai=True, use_rag=False)
        lines = [line.strip("- ") for line in ai_result.content.splitlines() if line.strip()]
        if len(lines) < 3:
            return fallback
        plans: list[GoalPlanSuggestion] = []
        chunk = []
        for line in lines:
            chunk.append(line)
            if len(chunk) == 4:
                plans.append(GoalPlanSuggestion(title=chunk[0], summary=chunk[1], tasks=chunk[2:]))
                chunk = []
        return GoalAiPlanResponse(goal_summary=payload.vision or payload.title, plans=plans or fallback.plans)
    except Exception:
        return fallback


@router.get("/plugins/configs", response_model=list[SystemPluginConfigRead])
def list_plugin_configs(
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[SystemPluginConfigRead]:
    instance = access_control_service.ensure_instance(session)
    known_plugin_ids = ["topics", "ideas"]
    result: list[SystemPluginConfigRead] = []
    for plugin_id in known_plugin_ids:
        config = get_plugin_config(session, instance.instance_id, plugin_id)
        result.append(serialize_plugin_config(plugin_id, config.is_enabled if config else False))
    return result


@router.put("/plugins/configs/{plugin_id}", response_model=SystemPluginConfigRead)
def update_plugin_config(
    plugin_id: str,
    payload: SystemPluginConfigUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> SystemPluginConfigRead:
    instance = require_instance_admin(session, current_user)
    if plugin_id not in {"topics", "ideas"}:
        raise HTTPException(status_code=404, detail="plugin not found")
    config = get_plugin_config(session, instance.instance_id, plugin_id)
    if config is None:
        config = SystemPluginConfig(instance_id=instance.instance_id, plugin_id=plugin_id)
    config.is_enabled = payload.is_enabled
    config.updated_by_user_id = current_user.id or 0
    config.updated_at = datetime.now(UTC)
    session.add(config)
    session.commit()
    session.refresh(config)
    return serialize_plugin_config(plugin_id, config.is_enabled)


@router.get("/ideas/overview", response_model=IdeaOverviewResponse)
def get_ideas_overview(
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> IdeaOverviewResponse:
    instance = access_control_service.ensure_instance(session)
    plugin = get_plugin_config(session, instance.instance_id, "ideas")
    enabled = plugin.is_enabled if plugin else False
    if not enabled:
        return IdeaOverviewResponse(plugin=serialize_plugin_config("ideas", False))
    ideas = visible_ideas(session, current_user)
    idea_ids = [idea.id or 0 for idea in ideas]
    logs = session.exec(
        select(IdeaActivityLog)
        .where(IdeaActivityLog.idea_id.in_(idea_ids))
        .order_by(IdeaActivityLog.created_at.desc())
    ).all() if idea_ids else []
    return IdeaOverviewResponse(
        plugin=serialize_plugin_config("ideas", True),
        ideas=[serialize_idea(idea) for idea in ideas],
        stats=compute_idea_stats(ideas),
        recent_logs=[serialize_idea_log(log) for log in logs[:12]],
    )


@router.post("/ideas", response_model=IdeaRead)
def create_idea(
    payload: IdeaCreateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> IdeaRead:
    instance = access_control_service.ensure_instance(session)
    ensure_plugin_enabled(session, instance.instance_id, "ideas")
    target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, payload.space_slug)
    idea = IdeaRecord(
        instance_id=instance.instance_id,
        owner_user_id=current_user.id or 0,
        space_slug=target_space.slug,
        title=payload.title,
        summary=payload.summary,
        details=payload.details,
        idea_type=payload.idea_type,
        tags="\n".join(payload.tags),
        status=payload.status,
        priority=payload.priority,
        value_score=payload.value_score,
        effort_score=payload.effort_score,
        business_score=payload.business_score,
        linked_note_slug=payload.linked_note_slug,
        linked_goal_id=payload.linked_goal_id,
        linked_topic_id=payload.linked_topic_id,
        source_context=payload.source_context,
        visibility_scope=payload.visibility_scope if instance.edition == "team" else "private",
        assignee_user_id=payload.assignee_user_id if instance.edition == "team" else None,
        next_step=payload.next_step,
    )
    session.add(idea)
    session.commit()
    session.refresh(idea)
    record_idea_activity(session, idea, current_user.id or 0, "created", f"status={idea.status}")
    return serialize_idea(idea)


@router.put("/ideas/{idea_id}", response_model=IdeaRead)
def update_idea(
    idea_id: int,
    payload: IdeaUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> IdeaRead:
    idea = require_idea_edit(session, idea_id, current_user)
    previous_status = idea.status
    validate_idea_status_transition(previous_status, payload.status)
    idea.title = payload.title
    idea.summary = payload.summary
    idea.details = payload.details
    idea.idea_type = payload.idea_type
    idea.tags = "\n".join(payload.tags)
    idea.status = payload.status
    idea.priority = payload.priority
    idea.value_score = payload.value_score
    idea.effort_score = payload.effort_score
    idea.business_score = payload.business_score
    idea.linked_note_slug = payload.linked_note_slug
    idea.linked_goal_id = payload.linked_goal_id
    idea.linked_topic_id = payload.linked_topic_id
    idea.source_context = payload.source_context
    idea.visibility_scope = payload.visibility_scope
    idea.assignee_user_id = payload.assignee_user_id
    idea.next_step = payload.next_step
    idea.updated_at = datetime.now(UTC)
    session.add(idea)
    session.commit()
    session.refresh(idea)
    record_idea_activity(session, idea, current_user.id or 0, "updated", f"{previous_status}->{idea.status}")
    return serialize_idea(idea)


@router.post("/ideas/{idea_id}/status", response_model=IdeaRead)
def update_idea_status(
    idea_id: int,
    payload: IdeaStatusUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> IdeaRead:
    idea = require_idea_edit(session, idea_id, current_user)
    previous_status = idea.status
    validate_idea_status_transition(previous_status, payload.status)
    idea.status = payload.status
    idea.updated_at = datetime.now(UTC)
    session.add(idea)
    session.commit()
    session.refresh(idea)
    record_idea_activity(session, idea, current_user.id or 0, "status-changed", f"{previous_status}->{idea.status}")
    return serialize_idea(idea)


@router.delete("/ideas/{idea_id}")
def delete_idea(
    idea_id: int,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict[str, bool]:
    idea = require_idea_edit(session, idea_id, current_user)
    record_idea_activity(session, idea, current_user.id or 0, "deleted", idea.title)
    session.delete(idea)
    session.commit()
    return {"ok": True}


@router.get("/topics/overview", response_model=TopicOverviewResponse)
def get_topics_overview(
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> TopicOverviewResponse:
    instance = access_control_service.ensure_instance(session)
    plugin = get_plugin_config(session, instance.instance_id, "topics")
    enabled = plugin.is_enabled if plugin else False
    if not enabled:
        return TopicOverviewResponse(plugin=serialize_plugin_config("topics", False))
    topics = visible_topics(session, current_user)
    topic_ids = [topic.id or 0 for topic in topics]
    logs = session.exec(
        select(TopicActivityLog)
        .where(TopicActivityLog.topic_id.in_(topic_ids))
        .order_by(TopicActivityLog.created_at.desc())
    ).all() if topic_ids else []
    return TopicOverviewResponse(
        plugin=serialize_plugin_config("topics", True),
        topics=[serialize_topic(topic) for topic in topics],
        stats=compute_topic_stats(topics),
        recent_logs=[serialize_topic_log(log) for log in logs[:12]],
    )


@router.post("/topics", response_model=TopicRead)
def create_topic(
    payload: TopicCreateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> TopicRead:
    instance = access_control_service.ensure_instance(session)
    ensure_plugin_enabled(session, instance.instance_id, "topics")
    target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, payload.space_slug)
    topic = TopicRecord(
        instance_id=instance.instance_id,
        owner_user_id=current_user.id or 0,
        space_slug=target_space.slug,
        title=payload.title,
        domain=payload.domain,
        keywords="\n".join(payload.keywords),
        source_type=payload.source_type,
        status=payload.status,
        priority=payload.priority,
        heat_score=payload.heat_score,
        trend_summary=payload.trend_summary,
        notes=payload.notes,
        linked_note_slug=payload.linked_note_slug,
        linked_password_entry_ids=serialize_id_csv(payload.linked_password_entry_ids),
        completed_note_slug=payload.completed_note_slug,
        assignee_user_id=payload.assignee_user_id if instance.edition == "team" else None,
        review_status=payload.review_status if instance.edition == "team" else "none",
        due_date=parse_iso_datetime(payload.due_date),
        last_collected_at=datetime.now(UTC) if payload.source_type == "ai" else None,
    )
    session.add(topic)
    session.commit()
    session.refresh(topic)
    record_topic_activity(session, topic, current_user.id or 0, "created", f"status={topic.status}")
    return serialize_topic(topic)


@router.put("/topics/{topic_id}", response_model=TopicRead)
def update_topic(
    topic_id: int,
    payload: TopicUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> TopicRead:
    topic = require_topic_edit(session, topic_id, current_user)
    previous_status = topic.status
    topic.title = payload.title
    topic.domain = payload.domain
    topic.keywords = "\n".join(payload.keywords)
    topic.status = payload.status
    topic.priority = payload.priority
    topic.heat_score = payload.heat_score
    topic.trend_summary = payload.trend_summary
    topic.notes = payload.notes
    topic.linked_note_slug = payload.linked_note_slug
    topic.linked_password_entry_ids = serialize_id_csv(payload.linked_password_entry_ids)
    topic.completed_note_slug = payload.completed_note_slug
    topic.assignee_user_id = payload.assignee_user_id
    topic.review_status = payload.review_status
    topic.due_date = parse_iso_datetime(payload.due_date)
    topic.updated_at = datetime.now(UTC)
    session.add(topic)
    session.commit()
    session.refresh(topic)
    record_topic_activity(session, topic, current_user.id or 0, "updated", f"{previous_status}->{topic.status}")
    return serialize_topic(topic)


@router.post("/topics/{topic_id}/status", response_model=TopicRead)
def update_topic_status(
    topic_id: int,
    payload: TopicStatusUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> TopicRead:
    topic = require_topic_edit(session, topic_id, current_user)
    previous_status = topic.status
    topic.status = payload.status
    topic.updated_at = datetime.now(UTC)
    session.add(topic)
    session.commit()
    session.refresh(topic)
    record_topic_activity(session, topic, current_user.id or 0, "status-changed", f"{previous_status}->{topic.status}")
    return serialize_topic(topic)


@router.delete("/topics/{topic_id}")
def delete_topic(
    topic_id: int,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict[str, bool]:
    topic = require_topic_edit(session, topic_id, current_user)
    record_topic_activity(session, topic, current_user.id or 0, "deleted", topic.title)
    session.delete(topic)
    session.commit()
    return {"ok": True}


@router.post("/topics/ai/discover", response_model=TopicAiDiscoverResponse)
async def discover_topics_with_ai(
    payload: TopicAiDiscoverRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> TopicAiDiscoverResponse:
    instance = access_control_service.ensure_instance(session)
    ensure_plugin_enabled(session, instance.instance_id, "topics")
    target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, payload.space_slug)
    candidates = await generate_topic_candidates(session, payload)
    now = datetime.now(UTC)
    if not payload.save_to_pool:
        return TopicAiDiscoverResponse(
            topics=[
                TopicRead(
                    id=0,
                    title=str(item.get("title", "未命名选题")),
                    domain=payload.domain,
                    keywords=[str(keyword) for keyword in item.get("keywords", payload.keywords)],
                    source_type="ai",
                    status="writable",
                    priority=str(item.get("priority", "medium")),
                    heat_score=int(item.get("heat_score", 50)),
                    trend_summary=str(item.get("trend_summary", "")),
                    notes="",
                    ai_outline="",
                    linked_note_slug="",
                    linked_password_entry_ids=[],
                    completed_note_slug="",
                    assignee_user_id=None,
                    review_status="none",
                    due_date=None,
                    last_collected_at=to_utc_iso(now),
                    created_at=to_utc_iso(now),
                    updated_at=to_utc_iso(now),
                )
                for item in candidates
            ]
        )
    created: list[TopicRead] = []
    for item in candidates:
        topic = TopicRecord(
            instance_id=instance.instance_id,
            owner_user_id=current_user.id or 0,
            space_slug=target_space.slug,
            title=str(item.get("title", "未命名选题")),
            domain=payload.domain,
            keywords="\n".join(str(keyword) for keyword in item.get("keywords", payload.keywords)),
            source_type="ai",
            status="writable",
            priority=str(item.get("priority", "medium")),
            heat_score=int(item.get("heat_score", 50)),
            trend_summary=str(item.get("trend_summary", "")),
            last_collected_at=now,
        )
        session.add(topic)
        session.commit()
        session.refresh(topic)
        record_topic_activity(session, topic, current_user.id or 0, "ai-discovered", topic.title)
        created.append(serialize_topic(topic))
    return TopicAiDiscoverResponse(topics=created)


@router.post("/topics/ai/outline", response_model=TopicAiOutlineResponse)
async def generate_topic_outline_endpoint(
    payload: TopicAiOutlineRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> TopicAiOutlineResponse:
    instance = access_control_service.ensure_instance(session)
    ensure_plugin_enabled(session, instance.instance_id, "topics")
    outline = await generate_topic_outline(session, payload)
    return TopicAiOutlineResponse(outline=outline)


@router.get("/goals/overview", response_model=GoalOverviewResponse)
async def get_goals_overview(
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> GoalOverviewResponse:
    goals = visible_goals(session, current_user)
    goal_ids = [goal.id or 0 for goal in goals]
    plans = session.exec(select(GoalPlanRecord).where(GoalPlanRecord.goal_id.in_(goal_ids)).order_by(GoalPlanRecord.sequence.asc())).all() if goal_ids else []
    tasks = session.exec(select(GoalTaskRecord).where(GoalTaskRecord.goal_id.in_(goal_ids)).order_by(GoalTaskRecord.updated_at.desc())).all() if goal_ids else []
    journals = session.exec(select(GoalJournalEntry).where(GoalJournalEntry.owner_user_id == (current_user.id or 0)).order_by(GoalJournalEntry.updated_at.desc())).all()
    today = datetime.now(UTC).date().isoformat()
    today_tasks = [task for task in tasks if (task.due_date and task.due_date.date().isoformat() == today) or task.status in {"todo", "doing"}][:8]
    task_status_by_goal: dict[int, list[str]] = {}
    task_status_by_plan: dict[int, list[str]] = {}
    for task in tasks:
        task_status_by_goal.setdefault(task.goal_id, []).append(task.status)
        task_status_by_plan.setdefault(task.plan_id, []).append(task.status)
    return GoalOverviewResponse(
        goals=[serialize_goal(goal, compute_progress_ratio(task_status_by_goal.get(goal.id or 0, []))) for goal in goals],
        plans=[serialize_goal_plan(plan, compute_progress_ratio(task_status_by_plan.get(plan.id or 0, []))) for plan in plans],
        today_tasks=[serialize_goal_task(task) for task in today_tasks],
        journals=[serialize_goal_journal(entry) for entry in journals[:10]],
        today_note_slug=slugify(today),
    )


@router.post("/goals", response_model=GoalRead)
def create_goal(
    payload: GoalCreateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> GoalRead:
    instance = access_control_service.ensure_instance(session)
    target_space = access_control_service.resolve_user_space(session, instance.instance_id, current_user, payload.space_slug)
    goal = GoalRecord(
        instance_id=instance.instance_id,
        owner_user_id=current_user.id or 0,
        space_slug=target_space.slug,
        title=payload.title,
        vision=payload.vision,
        key_results="\n".join(payload.key_results),
        priority=payload.priority,
        visibility_scope=payload.visibility_scope if instance.edition == "team" else "private",
        cycle_start=parse_iso_datetime(payload.cycle_start),
        cycle_end=parse_iso_datetime(payload.cycle_end),
    )
    session.add(goal)
    session.commit()
    session.refresh(goal)
    return serialize_goal(goal, 0)


@router.put("/goals/{goal_id}", response_model=GoalRead)
def update_goal(
    goal_id: int,
    payload: GoalUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> GoalRead:
    goal = require_goal_edit(session, goal_id, current_user)
    goal.title = payload.title
    goal.vision = payload.vision
    goal.key_results = "\n".join(payload.key_results)
    goal.priority = payload.priority
    goal.status = payload.status
    goal.visibility_scope = payload.visibility_scope
    goal.cycle_start = parse_iso_datetime(payload.cycle_start)
    goal.cycle_end = parse_iso_datetime(payload.cycle_end)
    goal.updated_at = datetime.now(UTC)
    session.add(goal)
    session.commit()
    plan_ids = [plan.id or 0 for plan in session.exec(select(GoalPlanRecord).where(GoalPlanRecord.goal_id == goal_id)).all()]
    statuses = [task.status for task in session.exec(select(GoalTaskRecord).where(GoalTaskRecord.plan_id.in_(plan_ids))).all()] if plan_ids else []
    return serialize_goal(goal, compute_progress_ratio(statuses))


@router.post("/goals/{goal_id}/plans", response_model=GoalPlanRead)
def create_goal_plan(
    goal_id: int,
    payload: GoalPlanCreateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> GoalPlanRead:
    goal = require_goal_edit(session, goal_id, current_user)
    max_sequence = session.exec(select(GoalPlanRecord).where(GoalPlanRecord.goal_id == goal_id).order_by(GoalPlanRecord.sequence.desc())).first()
    plan = GoalPlanRecord(
        goal_id=goal_id,
        instance_id=goal.instance_id,
        owner_user_id=current_user.id or 0,
        title=payload.title,
        summary=payload.summary,
        priority=payload.priority,
        sequence=(max_sequence.sequence + 1) if max_sequence else 1,
        start_date=parse_iso_datetime(payload.start_date),
        end_date=parse_iso_datetime(payload.end_date),
    )
    session.add(plan)
    session.commit()
    session.refresh(plan)
    return serialize_goal_plan(plan, 0)


@router.post("/goal-tasks", response_model=GoalTaskRead)
async def create_goal_task(
    payload: GoalTaskCreateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> GoalTaskRead:
    goal = require_goal_edit(session, payload.goal_id, current_user)
    plan = session.get(GoalPlanRecord, payload.plan_id)
    if plan is None or plan.goal_id != payload.goal_id:
        raise HTTPException(status_code=404, detail="goal plan not found")
    task = GoalTaskRecord(
        goal_id=payload.goal_id,
        plan_id=payload.plan_id,
        instance_id=goal.instance_id,
        owner_user_id=current_user.id or 0,
        assignee_user_id=payload.assignee_user_id,
        title=payload.title,
        details=payload.details,
        priority=payload.priority,
        due_date=parse_iso_datetime(payload.due_date),
    )
    session.add(task)
    session.commit()
    session.refresh(task)
    target_date = task.due_date.date().isoformat() if task.due_date else datetime.now(UTC).date().isoformat()
    await sync_task_line_to_daily_note(session, current_user, goal.space_slug, target_date, task, plan, goal)
    return serialize_goal_task(task)


@router.put("/goal-tasks/{task_id}", response_model=GoalTaskRead)
async def update_goal_task(
    task_id: int,
    payload: GoalTaskUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> GoalTaskRead:
    task = session.get(GoalTaskRecord, task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="goal task not found")
    goal = require_goal_edit(session, task.goal_id, current_user)
    plan = session.get(GoalPlanRecord, task.plan_id)
    task.title = payload.title
    task.details = payload.details
    task.priority = payload.priority
    task.status = payload.status
    task.due_date = parse_iso_datetime(payload.due_date)
    task.assignee_user_id = payload.assignee_user_id
    task.completed_at = datetime.now(UTC) if payload.status == "done" else None
    task.updated_at = datetime.now(UTC)
    session.add(task)
    session.commit()
    session.refresh(task)
    target_date = task.due_date.date().isoformat() if task.due_date else datetime.now(UTC).date().isoformat()
    await sync_task_line_to_daily_note(session, current_user, goal.space_slug, target_date, task, plan, goal)
    return serialize_goal_task(task)


@router.post("/goal-tasks/{task_id}/toggle", response_model=GoalTaskRead)
async def toggle_goal_task(
    task_id: int,
    payload: GoalTaskToggleRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> GoalTaskRead:
    task = session.get(GoalTaskRecord, task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="goal task not found")
    goal = require_goal_edit(session, task.goal_id, current_user)
    plan = session.get(GoalPlanRecord, task.plan_id)
    task.status = "done" if payload.done else "todo"
    task.completed_at = datetime.now(UTC) if payload.done else None
    task.updated_at = datetime.now(UTC)
    session.add(task)
    session.commit()
    session.refresh(task)
    target_date = task.due_date.date().isoformat() if task.due_date else datetime.now(UTC).date().isoformat()
    await sync_task_line_to_daily_note(session, current_user, goal.space_slug, target_date, task, plan, goal)
    return serialize_goal_task(task)


@router.put("/goal-journals", response_model=GoalJournalRead)
async def update_goal_journal(
    payload: GoalJournalUpdateRequest,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> GoalJournalRead:
    goal = require_goal_access(session, payload.goal_id, current_user) if payload.goal_id else None
    plan = session.get(GoalPlanRecord, payload.plan_id) if payload.plan_id else None
    target_space_slug = goal.space_slug if goal else access_control_service.ensure_user_default_space(
        session,
        access_control_service.ensure_instance(session).instance_id,
        current_user,
    ).slug
    note = await ensure_daily_note_for_date(session, current_user, payload.journal_date, target_space_slug)
    entry = session.exec(
        select(GoalJournalEntry).where(
            GoalJournalEntry.owner_user_id == (current_user.id or 0),
            GoalJournalEntry.journal_date == payload.journal_date,
            GoalJournalEntry.goal_id == payload.goal_id,
            GoalJournalEntry.plan_id == payload.plan_id,
        )
    ).first()
    if entry is None:
        entry = GoalJournalEntry(
            instance_id=access_control_service.ensure_instance(session).instance_id,
            owner_user_id=current_user.id or 0,
            goal_id=payload.goal_id,
            plan_id=payload.plan_id,
            journal_date=payload.journal_date,
            note_slug=note.slug,
        )
    entry.task_ids = serialize_id_csv(payload.task_ids)
    entry.reflection = payload.reflection
    entry.updated_at = datetime.now(UTC)
    session.add(entry)
    session.commit()
    session.refresh(entry)
    marker = f"<!-- goal-journal:{entry.id} -->"
    reflection_line = f"- {payload.reflection} {marker}" if payload.reflection else ""
    lines = [line for line in note.content.splitlines() if marker not in line]
    inserted = False
    result: list[str] = []
    for index, line in enumerate(lines):
        result.append(line)
        if line.strip() == "## 目标复盘" and reflection_line:
            result.append("")
            result.append(reflection_line)
            inserted = True
            remaining = [candidate for candidate in lines[index + 1 :] if candidate.strip()]
            result.extend(remaining)
            break
    if reflection_line and not inserted:
        result.extend(["", "## 目标复盘", "", reflection_line])
    await persist_note(
        session,
        NoteCreate(
            title=note.title,
            slug=note.slug,
            content="\n".join(result).strip() + "\n",
            path=note.path,
            tags=[item for item in note.tags.split(",") if item],
            links=[item for item in note.links.split(",") if item],
            source_url=note.source_url,
        ),
        device_id="goal-system",
        source="goal-journal-sync",
        existing_note=note,
    )
    return serialize_goal_journal(entry)
