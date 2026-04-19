from __future__ import annotations

import asyncio
import json
import sys
from dataclasses import dataclass
from pathlib import Path

from sqlmodel import Session, select

from app.core.config import settings
from app.models.note import AiProviderConfig, Note
from app.schemas.note import AiGenerateResponse, AiProviderConfigRead, AiProviderConfigUpsert, AiProviderPreset
from app.services.markdown_index import slugify
from app.services.rag_index import rag_index_service
from app.services.vault_sync import vault_sync_service


@dataclass
class RuntimeAiConfig:
    provider_id: str
    provider_label: str
    base_url: str
    api_key: str
    model_name: str


PROVIDER_PRESETS: list[AiProviderPreset] = [
    AiProviderPreset(
        provider_id="custom",
        label="自定义 OpenAI 兼容",
        base_url="https://api.openai.com/v1",
        models=["gpt-5", "gpt-4.1-mini", "custom-model"],
    ),
    AiProviderPreset(
        provider_id="openai",
        label="OpenAI",
        base_url="https://api.openai.com/v1",
        models=["gpt-5", "gpt-5-mini", "gpt-4.1-mini", "gpt-4o-mini", "gpt-4.1"],
    ),
    AiProviderPreset(
        provider_id="uniapi",
        label="UniAPI",
        base_url="https://your-uniapi-endpoint/v1",
        models=["gpt-5.3", "gpt-5", "claude-3-7-sonnet", "gemini-2.5-pro-preview"],
    ),
    AiProviderPreset(
        provider_id="openrouter",
        label="OpenRouter",
        base_url="https://openrouter.ai/api/v1",
        models=["openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet", "google/gemini-2.0-flash-001"],
    ),
    AiProviderPreset(
        provider_id="deepseek",
        label="DeepSeek",
        base_url="https://api.deepseek.com/v1",
        models=["deepseek-chat", "deepseek-reasoner"],
    ),
    AiProviderPreset(
        provider_id="qwen",
        label="通义千问",
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
        models=["qwen-turbo", "qwen-plus", "qwen-max"],
    ),
    AiProviderPreset(
        provider_id="moonshot",
        label="Moonshot",
        base_url="https://api.moonshot.cn/v1",
        models=["moonshot-v1-8k", "moonshot-v1-32k"],
    ),
    AiProviderPreset(
        provider_id="siliconflow",
        label="SiliconFlow",
        base_url="https://api.siliconflow.cn/v1",
        models=["Qwen/Qwen2.5-72B-Instruct", "deepseek-ai/DeepSeek-V3"],
    ),
    AiProviderPreset(
        provider_id="ollama",
        label="Ollama",
        base_url="http://127.0.0.1:11434/v1",
        models=["qwen2.5:7b", "llama3.1:8b", "deepseek-r1:7b"],
    ),
]

PYTHON_HTTP_BRIDGE = """
import json
import sys
import urllib.error
import urllib.request

url = sys.argv[1]
api_key = sys.argv[2]
payload = json.loads(sys.argv[3])
body = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(
    url,
    data=body,
    headers={
        'Authorization': f'Bearer {api_key}',
        'Content-Type': 'application/json',
    },
)

try:
    with urllib.request.urlopen(req, timeout=120) as response:
        result = {'status': response.status, 'payload': json.loads(response.read().decode('utf-8'))}
except urllib.error.HTTPError as exc:
    detail = exc.read().decode('utf-8')
    try:
        payload = json.loads(detail)
    except json.JSONDecodeError:
        payload = {'error': {'message': detail or str(exc)}}
    result = {'status': exc.code, 'payload': payload}

print(json.dumps(result))
""".strip()

class AiService:
    REMOTE_PROMPT_LIMIT = 3200
    REMOTE_NOTE_LIMIT = 1200
    REMOTE_CONTEXT_LIMIT = 1200

    def build_unique_note_identity(self, session: Session, folder: str, title: str) -> tuple[str, str]:
        safe_title = title[:40] if title else "AI Draft"
        candidate_title = safe_title
        candidate_slug = slugify(candidate_title)
        candidate_path = f"{folder}/{vault_sync_service.safe_filename(candidate_title)}.md"
        suffix = 2

        while session.exec(select(Note).where(Note.slug == candidate_slug)).first() is not None:
            candidate_title = f"{safe_title} {suffix}"
            candidate_slug = slugify(candidate_title)
            candidate_path = f"{folder}/{vault_sync_service.safe_filename(candidate_title)}.md"
            suffix += 1

        return candidate_title, candidate_path

    def list_provider_presets(self) -> list[AiProviderPreset]:
        return PROVIDER_PRESETS

    def get_current_provider(self, session: Session) -> AiProviderConfigRead | None:
        config = session.exec(
            select(AiProviderConfig).where(AiProviderConfig.is_default == True).order_by(AiProviderConfig.updated_at.desc())
        ).first()
        if config:
            return self.serialize_provider(config)
        if settings.ai_api_base and settings.ai_model:
            return AiProviderConfigRead(
                id=0,
                provider_id="env-default",
                provider_label=settings.ai_provider_label,
                base_url=settings.ai_api_base,
                model_name=settings.ai_model,
                api_key_masked=self.mask_key(settings.ai_api_key or ""),
                has_api_key=bool(settings.ai_api_key),
                is_enabled=bool(settings.ai_api_key),
                is_default=True,
            )
        return None

    def upsert_provider(self, session: Session, payload: AiProviderConfigUpsert) -> AiProviderConfigRead:
        existing = session.exec(
            select(AiProviderConfig).where(AiProviderConfig.is_default == True).order_by(AiProviderConfig.updated_at.desc())
        ).first()
        if existing is None:
            existing = AiProviderConfig()
            session.add(existing)

        existing.provider_id = payload.provider_id
        existing.provider_label = payload.provider_label
        existing.base_url = payload.base_url.strip().rstrip("/")
        incoming_api_key = payload.api_key.strip()
        if incoming_api_key:
            existing.api_key = incoming_api_key
        existing.model_name = payload.model_name.strip()
        existing.is_enabled = payload.is_enabled
        existing.is_default = True
        from datetime import UTC, datetime

        existing.updated_at = datetime.now(UTC)
        session.commit()
        session.refresh(existing)
        return self.serialize_provider(existing)

    def serialize_provider(self, config: AiProviderConfig) -> AiProviderConfigRead:
        return AiProviderConfigRead(
            id=config.id or 0,
            provider_id=config.provider_id,
            provider_label=config.provider_label,
            base_url=config.base_url,
            model_name=config.model_name,
            api_key_masked=self.mask_key(config.api_key),
            has_api_key=bool(config.api_key),
            is_enabled=config.is_enabled,
            is_default=config.is_default,
        )

    def mask_key(self, value: str) -> str:
        if not value:
            return ""
        if len(value) <= 8:
            return "*" * len(value)
        return f"{value[:4]}***{value[-4:]}"

    def resolve_runtime_config(self, session: Session, use_ai: bool = True) -> RuntimeAiConfig | None:
        if not use_ai:
            return None

        stored = session.exec(
            select(AiProviderConfig).where(AiProviderConfig.is_default == True).order_by(AiProviderConfig.updated_at.desc())
        ).first()
        if stored and stored.is_enabled and stored.base_url and stored.model_name and stored.api_key:
            return RuntimeAiConfig(
                provider_id=stored.provider_id,
                provider_label=stored.provider_label,
                base_url=stored.base_url.rstrip("/"),
                api_key=stored.api_key,
                model_name=stored.model_name,
            )

        if settings.ai_api_base and settings.ai_api_key and settings.ai_model:
            return RuntimeAiConfig(
                provider_id="env-default",
                provider_label=settings.ai_provider_label,
                base_url=settings.ai_api_base.rstrip("/"),
                api_key=settings.ai_api_key,
                model_name=settings.ai_model,
            )
        return None

    async def generate(
        self,
        session: Session,
        action: str,
        prompt: str,
        note_slug: str | None = None,
        *,
        use_ai: bool = True,
        use_rag: bool = True,
        visible_note_slugs: set[str] | None = None,
    ) -> AiGenerateResponse:
        references = rag_index_service.query(session, prompt, top_k=4, visible_note_slugs=visible_note_slugs).hits if use_rag else []
        context = "\n\n".join(f"[{item['title']}]\n{item['snippet']}" for item in references)
        note = None
        if note_slug:
            note = session.exec(select(Note).where(Note.slug == note_slug, Note.is_deleted == False)).first()

        runtime = self.resolve_runtime_config(session, use_ai=use_ai)
        if runtime:
            try:
                content = await self.remote_completion(runtime, action, prompt, context, note.content if note else "")
            except RuntimeError:
                content = self.local_completion(action, prompt, context, note.content if note else "")
        else:
            content = self.local_completion(action, prompt, context, note.content if note else "")

        return AiGenerateResponse(content=content, references=references, saved_note=None)

    async def summarize_external_content(
        self,
        session: Session,
        *,
        title: str,
        content: str,
        prompt: str,
        use_ai: bool = True,
        use_rag: bool = False,
        visible_note_slugs: set[str] | None = None,
    ) -> AiGenerateResponse:
        combined_prompt = f"标题: {title}\n\n待提炼内容:\n{content[:3000]}\n\n额外要求:\n{prompt}"
        return await self.generate(
            session,
            "summary",
            combined_prompt,
            use_ai=use_ai,
            use_rag=use_rag,
            visible_note_slugs=visible_note_slugs,
        )

    async def remote_completion(
        self,
        runtime: RuntimeAiConfig,
        action: str,
        prompt: str,
        context: str,
        note_content: str,
    ) -> str:
        trimmed_prompt = prompt[: self.REMOTE_PROMPT_LIMIT]
        trimmed_note = note_content[: self.REMOTE_NOTE_LIMIT]
        trimmed_context = context[: self.REMOTE_CONTEXT_LIMIT]
        system_prompt = (
            "你是私有知识库写作助手。基于用户上下文与知识库证据输出简洁、结构化 Markdown。"
        )
        user_prompt = (
            f"任务: {action}\n\n用户意图:\n{trimmed_prompt}\n\n当前笔记:\n{trimmed_note}\n\n检索上下文:\n{trimmed_context}"
        )
        responses_status, responses_payload = await self.post_json(
            f"{runtime.base_url.rstrip('/')}/responses",
            runtime.api_key,
            {
                "model": runtime.model_name,
                "max_output_tokens": 1024,
                "reasoning": {"effort": "minimal"},
                "text": {"verbosity": "low"},
                "input": f"{system_prompt}\n\n{user_prompt}",
            },
        )
        if 200 <= responses_status < 300:
            text = self.extract_responses_text(responses_payload)
            if text:
                return text

        chat_status, chat_payload = await self.post_json(
            f"{runtime.base_url.rstrip('/')}/chat/completions",
            runtime.api_key,
            {
                "model": runtime.model_name,
                "max_completion_tokens": 1024,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
            },
        )
        if 200 <= chat_status < 300:
            return chat_payload["choices"][0]["message"]["content"].strip()

        detail = (
            chat_payload.get("error", {}).get("message")
            or responses_payload.get("error", {}).get("message")
            or "远端模型调用失败"
        )
        raise RuntimeError(detail)

    def extract_responses_text(self, payload: dict) -> str:
        output_items = payload.get("output", [])
        fragments: list[str] = []

        for item in output_items:
            for content in item.get("content", []):
                text = (content.get("text") or "").strip()
                if content.get("type") == "output_text" and text:
                    fragments.append(text)

        if fragments:
            return "\n".join(fragments).strip()

        return (payload.get("output_text") or "").strip()

    async def post_json(self, url: str, api_key: str, payload: dict) -> tuple[int, dict]:
        process = await asyncio.create_subprocess_exec(
            sys.executable,
            "-c",
            PYTHON_HTTP_BRIDGE,
            url,
            api_key,
            json.dumps(payload, ensure_ascii=False),
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        stdout, stderr = await process.communicate()
        if process.returncode != 0:
            detail = stderr.decode("utf-8", errors="ignore").strip() or "远端模型调用失败"
            return 599, {"error": {"message": detail}}

        result = json.loads(stdout.decode("utf-8"))
        return int(result["status"]), result["payload"]

    def local_completion(self, action: str, prompt: str, context: str, note_content: str) -> str:
        if action == "summary":
            return f"# 总结\n\n{prompt}\n\n## 当前要点\n\n{note_content[:500]}\n\n## 知识库补充\n\n{context[:700]}"
        if action == "expand":
            return f"# 扩写草稿\n\n{note_content}\n\n## 建议展开\n\n- {prompt}\n- 结合这些上下文补足背景与例子\n\n{context[:900]}"
        if action == "polish":
            polished = note_content.replace("  ", " ").strip()
            return f"# 润色结果\n\n{polished}\n\n## 润色说明\n\n- 统一段落结构\n- 保留原意并强化表达"
        if action == "create":
            return (
                f"# 创作草稿\n\n## 创作任务\n\n{prompt}\n\n## 可复用素材\n\n{context[:1000]}"
                f"\n\n## 建议结构\n\n1. 先用一句话定义主题\n2. 展开关键观点\n3. 给出结论或行动建议"
            )
        return f"# 回答\n\n基于知识库检索，先给出直接答案：\n\n{prompt}\n\n## 证据整理\n\n{context[:1000]}\n\n## 建议\n\n- 将结果沉淀为新笔记或补充到现有卡片"

    def save_generated_note(self, session: Session, title: str, folder: str, content: str, source_url: str = "") -> Note:
        unique_title, path = self.build_unique_note_identity(session, folder, title)
        payload = vault_sync_service.materialize_note(unique_title, path, content, source_url=source_url)
        note = Note(
            title=payload.title,
            slug=payload.slug,
            content=payload.content,
            path=payload.path,
            parent_path=str(Path(payload.path).parent).replace("\\", "/"),
            source_url=source_url,
        )
        session.add(note)
        session.commit()
        session.refresh(note)
        return note


ai_service = AiService()