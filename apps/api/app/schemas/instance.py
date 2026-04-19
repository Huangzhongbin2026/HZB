from datetime import datetime
from typing import Literal, Optional

from pydantic import BaseModel, Field


class InstanceConfigRead(BaseModel):
    instance_id: str
    instance_name: str
    deployment_mode: Literal["desktop", "server"]
    edition: Literal["personal", "team"]
    desktop_host_enabled: bool
    server_host_enabled: bool
    auth_required: bool
    team_license_verified: bool
    is_initialized: bool


class InstanceBootstrapRequest(BaseModel):
    instance_name: str = Field(min_length=2, max_length=64)
    deployment_mode: Literal["desktop", "server"]
    edition: Literal["personal", "team"]
    authorization_code: str = ""


class KnowledgeSpaceRead(BaseModel):
    id: int
    instance_id: str
    owner_user_id: int
    slug: str
    name: str
    visibility: Literal["private", "team"]
    is_default: bool


class KnowledgeSpaceCreateRequest(BaseModel):
    name: str = Field(min_length=2, max_length=64)
    slug: str = Field(min_length=2, max_length=64)
    visibility: Literal["private", "team"] = "private"


class TeamInviteCreateRequest(BaseModel):
    role: Literal["member", "manager"] = "member"
    expires_in_hours: int = Field(default=72, ge=1, le=24 * 30)


class TeamInviteRead(BaseModel):
    invite_code: str
    role: str
    status: str
    expires_at: Optional[datetime] = None
    invite_link: str = ""


class TeamInviteAcceptRequest(BaseModel):
    invite_code: str
    username: str = Field(min_length=3, max_length=32)
    password: str = Field(min_length=8, max_length=128)
    display_name: str = Field(min_length=2, max_length=32)


class TeamMemberRead(BaseModel):
    user_id: int
    username: str
    display_name: str
    role: str
    default_space_slug: str
    content_visibility_scope: str


class NoteSharingUpdateRequest(BaseModel):
    visibility_scope: Literal["private", "team", "selected"] = "private"
    shared_member_ids: list[int] = Field(default_factory=list)


class NoteSharingRead(BaseModel):
    note_slug: str
    owner_user_id: int
    space_slug: str
    visibility_scope: str
    shared_member_ids: list[int] = Field(default_factory=list)