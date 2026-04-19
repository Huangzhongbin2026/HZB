from typing import Literal, Optional

from pydantic import BaseModel, Field


class NoteCreate(BaseModel):
    title: str
    slug: str
    content: str
    path: str
    space_slug: str | None = None
    tags: list[str] = Field(default_factory=list)
    links: list[str] = Field(default_factory=list)
    source_url: str = ""


class NoteRead(BaseModel):
    id: int
    title: str
    slug: str
    content: str
    path: str
    parent_path: str
    tags: list[str]
    links: list[str]
    summary: str
    version: str
    updated_at: str


class NoteUpdate(BaseModel):
    title: str
    content: str
    path: str
    tags: list[str] = Field(default_factory=list)
    links: list[str] = Field(default_factory=list)
    previous_version: Optional[str] = None


class MoveNoteRequest(BaseModel):
    target_path: str
    previous_version: Optional[str] = None


class DeleteRequest(BaseModel):
    previous_version: Optional[str] = None


class FolderCreateRequest(BaseModel):
    path: str


class FolderMoveRequest(BaseModel):
    source_path: str
    target_path: str


class FolderDeleteRequest(BaseModel):
    path: str


class FileTreeNode(BaseModel):
    name: str
    path: str
    node_type: Literal["folder", "file"]
    children: list["FileTreeNode"] = Field(default_factory=list)
    note_slug: Optional[str] = None


class LinkRead(BaseModel):
    source_slug: str
    target_slug: str
    target_title: str
    is_resolved: bool


class BacklinkBundle(BaseModel):
    note_slug: str
    outgoing: list[LinkRead]
    incoming: list[LinkRead]


class SearchResult(BaseModel):
    note_slug: str
    title: str
    path: str
    snippet: str


class SearchResponse(BaseModel):
    results: list[SearchResult]


class TagSummary(BaseModel):
    tag: str
    count: int


class SyncEventRead(BaseModel):
    id: int
    note_slug: str
    event_type: str
    path: str
    target_path: str
    payload_json: str
    version: str
    status: str
    created_at: str


class SyncAckRequest(BaseModel):
    event_ids: list[int]
    device_id: str


class SyncStatusResponse(BaseModel):
    pending_events: int
    connected_desktop_devices: int
    last_event_id: int | None


class SyncUploadRequest(BaseModel):
    device_id: str
    device_type: str = "web"
    source: str
    note: NoteCreate
    version: str
    previous_version: Optional[str] = None


class WebClipRequest(BaseModel):
    url: str
    device_id: str = "web-clipper"
    summarize_with_ai: bool = False
    use_ai: bool = True
    use_rag: bool = False
    summary_prompt: str = "请基于网页正文提炼要点、结构和行动建议。"
    target_folder: str = "00_Inbox"
    space_slug: str | None = None
    save_to_note: bool = True


class WebClipResponse(BaseModel):
    note: Optional[NoteRead] = None
    extracted_title: str
    source_url: str
    summary: str = ""
    preview_content: str = ""
    saved: bool = False


class WebClipSaveRequest(BaseModel):
    title: str
    source_url: str
    content: str
    summary: str = ""
    target_folder: str = "00_Inbox"
    space_slug: str | None = None
    device_id: str = "web-clipper"


class RagQueryRequest(BaseModel):
    query: str
    top_k: int = 5


class RagQueryResponse(BaseModel):
    hits: list[dict[str, str]]


class AiGenerateRequest(BaseModel):
    action: Literal["summary", "expand", "polish", "qa", "create"]
    note_slug: Optional[str] = None
    prompt: str
    save_as_note: bool = False
    target_folder: str = "00_Inbox"
    space_slug: str | None = None
    use_ai: bool = True
    use_rag: bool = True


class AiProviderPreset(BaseModel):
    provider_id: str
    label: str
    base_url: str
    models: list[str] = Field(default_factory=list)


class AiProviderConfigUpsert(BaseModel):
    provider_id: str = "custom"
    provider_label: str = "自定义"
    base_url: str
    api_key: str = ""
    model_name: str
    is_enabled: bool = True


class AiProviderConfigRead(BaseModel):
    id: int = 0
    provider_id: str
    provider_label: str
    base_url: str
    model_name: str
    api_key_masked: str = ""
    has_api_key: bool = False
    is_enabled: bool = True
    is_default: bool = True


class AiProviderCatalogResponse(BaseModel):
    presets: list[AiProviderPreset] = Field(default_factory=list)
    current: Optional[AiProviderConfigRead] = None


class DocumentUploadResponse(BaseModel):
    filename: str
    extracted_title: str
    content_preview: str
    summary: str = ""
    saved_note: Optional[NoteRead] = None


class AiGenerateResponse(BaseModel):
    content: str
    references: list[dict[str, str]]
    saved_note: Optional[NoteRead] = None


FileTreeNode.model_rebuild()
