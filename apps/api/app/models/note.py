from datetime import UTC, datetime
from typing import Optional

from sqlmodel import Field, SQLModel


class User(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    username: str = Field(index=True, unique=True)
    password_hash: str
    display_name: str
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class Device(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    device_id: str = Field(index=True, unique=True)
    device_type: str = Field(index=True)
    display_name: str
    last_seen_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    sync_status: str = Field(default="offline", index=True)


class Note(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    title: str = Field(index=True)
    slug: str = Field(index=True, unique=True)
    content: str
    path: str = Field(index=True)
    parent_path: str = Field(default="01_Notes", index=True)
    tags: str = ""
    links: str = ""
    summary: str = ""
    source_url: str = ""
    version: str = Field(default="")
    is_deleted: bool = Field(default=False, index=True)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class NoteTag(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    note_slug: str = Field(index=True)
    tag: str = Field(index=True)


class NoteLink(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    source_slug: str = Field(index=True)
    target_slug: str = Field(index=True)
    target_title: str
    is_resolved: bool = Field(default=False, index=True)


class SyncEvent(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    note_slug: str = Field(index=True)
    device_id: str = Field(index=True)
    event_type: str
    path: str = Field(default="", index=True)
    target_path: str = Field(default="")
    payload_json: str = Field(default="{}")
    version: str
    status: str = Field(default="pending", index=True)
    acknowledged_by: str = Field(default="")
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class RagChunk(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    note_slug: str = Field(index=True)
    chunk_index: int
    content: str
    token_count: int = 0
    embedding_ref: str = ""


class SavedClip(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    url: str = Field(index=True, unique=True)
    note_slug: str = Field(index=True)
    title: str
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class AiProviderConfig(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    provider_id: str = Field(index=True, default="custom")
    provider_label: str = Field(default="自定义")
    base_url: str = Field(default="")
    api_key: str = Field(default="")
    model_name: str = Field(default="")
    is_enabled: bool = Field(default=True, index=True)
    is_default: bool = Field(default=True, index=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class InstanceConfig(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True, unique=True)
    instance_name: str = Field(default="个人双向知识库")
    deployment_mode: str = Field(default="desktop", index=True)
    edition: str = Field(default="personal", index=True)
    desktop_host_enabled: bool = Field(default=True)
    server_host_enabled: bool = Field(default=False)
    auth_required: bool = Field(default=False)
    team_license_verified: bool = Field(default=False)
    team_authorization_code_hash: str = Field(default="")
    is_initialized: bool = Field(default=False)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class KnowledgeSpace(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True)
    owner_user_id: int = Field(index=True)
    slug: str = Field(index=True, unique=True)
    name: str
    visibility: str = Field(default="private", index=True)
    is_default: bool = Field(default=False, index=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class TeamMembership(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True)
    user_id: int = Field(index=True)
    space_slug: str = Field(default="", index=True)
    role: str = Field(default="member", index=True)
    content_visibility_scope: str = Field(default="private", index=True)
    is_active: bool = Field(default=True, index=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class TeamInvite(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True)
    invite_code: str = Field(index=True, unique=True)
    created_by_user_id: int = Field(index=True)
    role: str = Field(default="member", index=True)
    status: str = Field(default="pending", index=True)
    expires_at: Optional[datetime] = Field(default=None)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class NoteAccessPolicy(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    note_slug: str = Field(index=True, unique=True)
    owner_user_id: int = Field(index=True)
    space_slug: str = Field(default="", index=True)
    visibility_scope: str = Field(default="private", index=True)
    shared_member_ids: str = Field(default="")
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class PasswordVaultConfig(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True, unique=True)
    verifier_salt: str = Field(default="")
    verifier_hash: str = Field(default="")
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class PasswordVaultEntry(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True)
    owner_user_id: int = Field(index=True)
    space_slug: str = Field(default="", index=True)
    title: str = Field(index=True)
    username: str = Field(default="", index=True)
    encrypted_password: str
    encryption_salt: str
    category: str = Field(default="general", index=True)
    url: str = Field(default="")
    website: str = Field(default="", index=True)
    notes: str = Field(default="")
    linked_note_slug: str = Field(default="", index=True)
    vault_scope: str = Field(default="private", index=True)
    shared_member_ids: str = Field(default="")
    editor_member_ids: str = Field(default="")
    is_deleted: bool = Field(default=False, index=True)
    last_used_at: Optional[datetime] = Field(default=None, nullable=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class PasswordAccessAudit(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    entry_id: int = Field(index=True)
    instance_id: str = Field(index=True)
    actor_user_id: int = Field(index=True)
    action: str = Field(index=True)
    detail: str = Field(default="")
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class SystemPluginConfig(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True)
    plugin_id: str = Field(index=True)
    is_enabled: bool = Field(default=False, index=True)
    updated_by_user_id: int = Field(default=0, index=True)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class TopicRecord(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True)
    owner_user_id: int = Field(index=True)
    space_slug: str = Field(default="", index=True)
    title: str = Field(index=True)
    domain: str = Field(default="", index=True)
    keywords: str = Field(default="")
    source_type: str = Field(default="manual", index=True)
    status: str = Field(default="writable", index=True)
    priority: str = Field(default="medium", index=True)
    heat_score: int = Field(default=50, index=True)
    trend_summary: str = Field(default="")
    notes: str = Field(default="")
    ai_outline: str = Field(default="")
    linked_note_slug: str = Field(default="", index=True)
    linked_password_entry_ids: str = Field(default="")
    completed_note_slug: str = Field(default="", index=True)
    assignee_user_id: Optional[int] = Field(default=None, index=True, nullable=True)
    review_status: str = Field(default="none", index=True)
    due_date: Optional[datetime] = Field(default=None, index=True, nullable=True)
    last_collected_at: Optional[datetime] = Field(default=None, nullable=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class TopicActivityLog(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    topic_id: int = Field(index=True)
    instance_id: str = Field(index=True)
    actor_user_id: int = Field(index=True)
    action: str = Field(index=True)
    detail: str = Field(default="")
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class IdeaRecord(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True)
    owner_user_id: int = Field(index=True)
    space_slug: str = Field(default="", index=True)
    title: str = Field(index=True)
    summary: str = Field(default="")
    details: str = Field(default="")
    idea_type: str = Field(default="creative_idea", index=True)
    tags: str = Field(default="")
    status: str = Field(default="pending_review", index=True)
    priority: str = Field(default="medium", index=True)
    value_score: int = Field(default=60, index=True)
    effort_score: int = Field(default=40, index=True)
    business_score: int = Field(default=50, index=True)
    linked_note_slug: str = Field(default="", index=True)
    linked_goal_id: Optional[int] = Field(default=None, index=True, nullable=True)
    linked_topic_id: Optional[int] = Field(default=None, index=True, nullable=True)
    source_context: str = Field(default="")
    visibility_scope: str = Field(default="private", index=True)
    assignee_user_id: Optional[int] = Field(default=None, index=True, nullable=True)
    next_step: str = Field(default="")
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class IdeaActivityLog(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    idea_id: int = Field(index=True)
    instance_id: str = Field(index=True)
    actor_user_id: int = Field(index=True)
    action: str = Field(index=True)
    detail: str = Field(default="")
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class GoalRecord(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True)
    owner_user_id: int = Field(index=True)
    space_slug: str = Field(default="", index=True)
    title: str = Field(index=True)
    vision: str = Field(default="")
    key_results: str = Field(default="")
    priority: str = Field(default="medium", index=True)
    status: str = Field(default="active", index=True)
    visibility_scope: str = Field(default="private", index=True)
    cycle_start: Optional[datetime] = Field(default=None, nullable=True)
    cycle_end: Optional[datetime] = Field(default=None, nullable=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class GoalPlanRecord(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    goal_id: int = Field(index=True)
    instance_id: str = Field(index=True)
    owner_user_id: int = Field(index=True)
    title: str = Field(index=True)
    summary: str = Field(default="")
    priority: str = Field(default="medium", index=True)
    status: str = Field(default="active", index=True)
    sequence: int = Field(default=0, index=True)
    start_date: Optional[datetime] = Field(default=None, nullable=True)
    end_date: Optional[datetime] = Field(default=None, nullable=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class GoalTaskRecord(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    goal_id: int = Field(index=True)
    plan_id: int = Field(index=True)
    instance_id: str = Field(index=True)
    owner_user_id: int = Field(index=True)
    assignee_user_id: Optional[int] = Field(default=None, index=True, nullable=True)
    title: str = Field(index=True)
    details: str = Field(default="")
    priority: str = Field(default="medium", index=True)
    status: str = Field(default="todo", index=True)
    due_date: Optional[datetime] = Field(default=None, index=True, nullable=True)
    completed_at: Optional[datetime] = Field(default=None, nullable=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)


class GoalJournalEntry(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    instance_id: str = Field(index=True)
    owner_user_id: int = Field(index=True)
    goal_id: int = Field(default=0, index=True)
    plan_id: int = Field(default=0, index=True)
    journal_date: str = Field(index=True)
    note_slug: str = Field(default="", index=True)
    task_ids: str = Field(default="")
    reflection: str = Field(default="")
    ai_summary: str = Field(default="")
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
    updated_at: datetime = Field(default_factory=lambda: datetime.now(UTC), nullable=False)
