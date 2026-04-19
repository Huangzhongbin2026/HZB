from typing import Literal, Optional

from pydantic import BaseModel, Field


class SystemPluginConfigRead(BaseModel):
    plugin_id: str
    is_enabled: bool


class SystemPluginConfigUpdateRequest(BaseModel):
    is_enabled: bool


class PasswordVaultConfigRead(BaseModel):
    is_initialized: bool


class PasswordVaultSetupRequest(BaseModel):
    master_password: str = Field(min_length=8)


class PasswordVaultVerifyRequest(BaseModel):
    master_password: str = Field(min_length=8)


class PasswordGenerateRequest(BaseModel):
    length: int = Field(default=20, ge=12, le=64)
    include_symbols: bool = True
    include_numbers: bool = True
    include_uppercase: bool = True


class PasswordGenerateResponse(BaseModel):
    password: str


class PasswordEntryCreateRequest(BaseModel):
    title: str
    username: str = ""
    password: str
    master_password: str = Field(min_length=8)
    category: str = "general"
    url: str = ""
    website: str = ""
    notes: str = ""
    linked_note_slug: str = ""
    vault_scope: Literal["private", "team", "selected"] = "private"
    shared_member_ids: list[int] = Field(default_factory=list)
    editor_member_ids: list[int] = Field(default_factory=list)
    space_slug: Optional[str] = None


class PasswordEntryUpdateRequest(BaseModel):
    title: str
    username: str = ""
    master_password: str = Field(min_length=8)
    password: Optional[str] = None
    category: str = "general"
    url: str = ""
    website: str = ""
    notes: str = ""
    linked_note_slug: str = ""
    vault_scope: Literal["private", "team", "selected"] = "private"
    shared_member_ids: list[int] = Field(default_factory=list)
    editor_member_ids: list[int] = Field(default_factory=list)


class PasswordRevealRequest(BaseModel):
    master_password: str = Field(min_length=8)


class PasswordEntryRead(BaseModel):
    id: int
    title: str
    username: str
    category: str
    url: str
    website: str
    notes: str
    linked_note_slug: str
    vault_scope: str
    shared_member_ids: list[int] = Field(default_factory=list)
    editor_member_ids: list[int] = Field(default_factory=list)
    created_at: str
    updated_at: str
    last_used_at: Optional[str] = None


class PasswordEntrySecretRead(BaseModel):
    entry: PasswordEntryRead
    password: str


class PasswordAuditRead(BaseModel):
    id: int
    entry_id: int
    actor_user_id: int
    action: str
    detail: str
    created_at: str


class UnifiedSearchItem(BaseModel):
    kind: Literal["note", "password"]
    title: str
    path: str
    snippet: str
    note_slug: Optional[str] = None
    password_id: Optional[int] = None


class UnifiedSearchResponse(BaseModel):
    results: list[UnifiedSearchItem] = Field(default_factory=list)


class GoalCreateRequest(BaseModel):
    title: str
    vision: str = ""
    key_results: list[str] = Field(default_factory=list)
    priority: Literal["low", "medium", "high"] = "medium"
    visibility_scope: Literal["private", "team"] = "private"
    cycle_start: Optional[str] = None
    cycle_end: Optional[str] = None
    space_slug: Optional[str] = None


class GoalUpdateRequest(BaseModel):
    title: str
    vision: str = ""
    key_results: list[str] = Field(default_factory=list)
    priority: Literal["low", "medium", "high"] = "medium"
    status: Literal["active", "paused", "completed"] = "active"
    visibility_scope: Literal["private", "team"] = "private"
    cycle_start: Optional[str] = None
    cycle_end: Optional[str] = None


class GoalPlanCreateRequest(BaseModel):
    title: str
    summary: str = ""
    priority: Literal["low", "medium", "high"] = "medium"
    start_date: Optional[str] = None
    end_date: Optional[str] = None


class GoalTaskCreateRequest(BaseModel):
    goal_id: int
    plan_id: int
    title: str
    details: str = ""
    priority: Literal["low", "medium", "high"] = "medium"
    due_date: Optional[str] = None
    assignee_user_id: Optional[int] = None


class GoalTaskUpdateRequest(BaseModel):
    title: str
    details: str = ""
    priority: Literal["low", "medium", "high"] = "medium"
    status: Literal["todo", "doing", "done", "cancelled"] = "todo"
    due_date: Optional[str] = None
    assignee_user_id: Optional[int] = None


class GoalTaskToggleRequest(BaseModel):
    done: bool


class GoalJournalUpdateRequest(BaseModel):
    journal_date: str
    goal_id: int = 0
    plan_id: int = 0
    reflection: str = ""
    task_ids: list[int] = Field(default_factory=list)


class GoalAiPlanRequest(BaseModel):
    title: str
    vision: str = ""
    key_results: list[str] = Field(default_factory=list)
    use_ai: bool = True


class GoalPlanSuggestion(BaseModel):
    title: str
    summary: str
    tasks: list[str] = Field(default_factory=list)


class GoalAiPlanResponse(BaseModel):
    goal_summary: str
    plans: list[GoalPlanSuggestion] = Field(default_factory=list)


class GoalRead(BaseModel):
    id: int
    title: str
    vision: str
    key_results: list[str] = Field(default_factory=list)
    priority: str
    status: str
    visibility_scope: str
    cycle_start: Optional[str] = None
    cycle_end: Optional[str] = None
    progress_percent: int = 0
    created_at: str
    updated_at: str


class GoalPlanRead(BaseModel):
    id: int
    goal_id: int
    title: str
    summary: str
    priority: str
    status: str
    sequence: int
    start_date: Optional[str] = None
    end_date: Optional[str] = None
    progress_percent: int = 0
    created_at: str
    updated_at: str


class GoalTaskRead(BaseModel):
    id: int
    goal_id: int
    plan_id: int
    title: str
    details: str
    priority: str
    status: str
    due_date: Optional[str] = None
    assignee_user_id: Optional[int] = None
    completed_at: Optional[str] = None
    created_at: str
    updated_at: str


class GoalJournalRead(BaseModel):
    id: int
    goal_id: int
    plan_id: int
    journal_date: str
    note_slug: str
    task_ids: list[int] = Field(default_factory=list)
    reflection: str
    ai_summary: str
    updated_at: str


class GoalOverviewResponse(BaseModel):
    goals: list[GoalRead] = Field(default_factory=list)
    plans: list[GoalPlanRead] = Field(default_factory=list)
    today_tasks: list[GoalTaskRead] = Field(default_factory=list)
    journals: list[GoalJournalRead] = Field(default_factory=list)
    today_note_slug: str = ""


class TopicCreateRequest(BaseModel):
    title: str
    domain: str = ""
    keywords: list[str] = Field(default_factory=list)
    status: Literal["writable", "pending", "in_progress", "completed", "shelved"] = "writable"
    priority: Literal["low", "medium", "high"] = "medium"
    heat_score: int = Field(default=50, ge=0, le=100)
    trend_summary: str = ""
    notes: str = ""
    linked_note_slug: str = ""
    linked_password_entry_ids: list[int] = Field(default_factory=list)
    completed_note_slug: str = ""
    assignee_user_id: Optional[int] = None
    review_status: Literal["none", "pending", "approved", "needs_revision"] = "none"
    due_date: Optional[str] = None
    source_type: Literal["manual", "ai", "team"] = "manual"
    space_slug: Optional[str] = None


class TopicUpdateRequest(BaseModel):
    title: str
    domain: str = ""
    keywords: list[str] = Field(default_factory=list)
    status: Literal["writable", "pending", "in_progress", "completed", "shelved"] = "writable"
    priority: Literal["low", "medium", "high"] = "medium"
    heat_score: int = Field(default=50, ge=0, le=100)
    trend_summary: str = ""
    notes: str = ""
    linked_note_slug: str = ""
    linked_password_entry_ids: list[int] = Field(default_factory=list)
    completed_note_slug: str = ""
    assignee_user_id: Optional[int] = None
    review_status: Literal["none", "pending", "approved", "needs_revision"] = "none"
    due_date: Optional[str] = None


class TopicStatusUpdateRequest(BaseModel):
    status: Literal["writable", "pending", "in_progress", "completed", "shelved"]


class TopicAiDiscoverRequest(BaseModel):
    domain: str = ""
    keywords: list[str] = Field(default_factory=list)
    count: int = Field(default=5, ge=1, le=10)
    use_ai: bool = True
    save_to_pool: bool = True
    space_slug: Optional[str] = None


class TopicAiOutlineRequest(BaseModel):
    title: str
    domain: str = ""
    keywords: list[str] = Field(default_factory=list)
    trend_summary: str = ""
    use_ai: bool = True


class TopicRead(BaseModel):
    id: int
    title: str
    domain: str
    keywords: list[str] = Field(default_factory=list)
    source_type: str
    status: str
    priority: str
    heat_score: int
    trend_summary: str
    notes: str
    ai_outline: str
    linked_note_slug: str
    linked_password_entry_ids: list[int] = Field(default_factory=list)
    completed_note_slug: str
    assignee_user_id: Optional[int] = None
    review_status: str
    due_date: Optional[str] = None
    last_collected_at: Optional[str] = None
    created_at: str
    updated_at: str


class TopicActivityLogRead(BaseModel):
    id: int
    topic_id: int
    actor_user_id: int
    action: str
    detail: str
    created_at: str


class TopicStatsRead(BaseModel):
    total: int = 0
    completed: int = 0
    writable: int = 0
    pending: int = 0
    in_progress: int = 0
    shelved: int = 0
    high_priority: int = 0
    completion_rate: int = 0


class TopicOverviewResponse(BaseModel):
    plugin: SystemPluginConfigRead
    topics: list[TopicRead] = Field(default_factory=list)
    stats: TopicStatsRead = Field(default_factory=TopicStatsRead)
    recent_logs: list[TopicActivityLogRead] = Field(default_factory=list)


class TopicAiOutlineResponse(BaseModel):
    outline: str


class TopicAiDiscoverResponse(BaseModel):
    topics: list[TopicRead] = Field(default_factory=list)


class IdeaCreateRequest(BaseModel):
    title: str
    summary: str = ""
    details: str = ""
    idea_type: Literal["creative_idea", "user_need", "product_opportunity", "optimization"] = "creative_idea"
    tags: list[str] = Field(default_factory=list)
    status: Literal["pending_review", "accepted", "planning", "building", "launched", "shelved"] = "pending_review"
    priority: Literal["low", "medium", "high"] = "medium"
    value_score: int = Field(default=60, ge=0, le=100)
    effort_score: int = Field(default=40, ge=0, le=100)
    business_score: int = Field(default=50, ge=0, le=100)
    linked_note_slug: str = ""
    linked_goal_id: Optional[int] = None
    linked_topic_id: Optional[int] = None
    source_context: str = ""
    visibility_scope: Literal["private", "team"] = "private"
    assignee_user_id: Optional[int] = None
    next_step: str = ""
    space_slug: Optional[str] = None


class IdeaUpdateRequest(BaseModel):
    title: str
    summary: str = ""
    details: str = ""
    idea_type: Literal["creative_idea", "user_need", "product_opportunity", "optimization"] = "creative_idea"
    tags: list[str] = Field(default_factory=list)
    status: Literal["pending_review", "accepted", "planning", "building", "launched", "shelved"] = "pending_review"
    priority: Literal["low", "medium", "high"] = "medium"
    value_score: int = Field(default=60, ge=0, le=100)
    effort_score: int = Field(default=40, ge=0, le=100)
    business_score: int = Field(default=50, ge=0, le=100)
    linked_note_slug: str = ""
    linked_goal_id: Optional[int] = None
    linked_topic_id: Optional[int] = None
    source_context: str = ""
    visibility_scope: Literal["private", "team"] = "private"
    assignee_user_id: Optional[int] = None
    next_step: str = ""


class IdeaStatusUpdateRequest(BaseModel):
    status: Literal["pending_review", "accepted", "planning", "building", "launched", "shelved"]


class IdeaRead(BaseModel):
    id: int
    title: str
    summary: str
    details: str
    idea_type: str
    tags: list[str] = Field(default_factory=list)
    status: str
    priority: str
    value_score: int
    effort_score: int
    business_score: int
    opportunity_score: int
    linked_note_slug: str
    linked_goal_id: Optional[int] = None
    linked_topic_id: Optional[int] = None
    source_context: str
    visibility_scope: str
    assignee_user_id: Optional[int] = None
    next_step: str
    created_at: str
    updated_at: str


class IdeaActivityLogRead(BaseModel):
    id: int
    idea_id: int
    actor_user_id: int
    action: str
    detail: str
    created_at: str


class IdeaStatsRead(BaseModel):
    total: int = 0
    pending_review: int = 0
    accepted: int = 0
    planning: int = 0
    building: int = 0
    launched: int = 0
    shelved: int = 0
    high_priority: int = 0
    average_opportunity_score: int = 0


class IdeaOverviewResponse(BaseModel):
    plugin: SystemPluginConfigRead
    ideas: list[IdeaRead] = Field(default_factory=list)
    stats: IdeaStatsRead = Field(default_factory=IdeaStatsRead)
    recent_logs: list[IdeaActivityLogRead] = Field(default_factory=list)
