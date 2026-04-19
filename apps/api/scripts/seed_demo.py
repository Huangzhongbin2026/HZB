from sqlmodel import Session, select

from app.db.session import init_db, engine
from app.models.note import Note
from app.schemas.note import NoteCreate
from app.services.markdown_index import markdown_index_service
from app.services.vault_sync import vault_sync_service


DEMO_NOTES = [
    NoteCreate(
        title="欢迎来到 Knowledge Cloud",
        slug="welcome-knowledge-cloud",
        path="00_Inbox/welcome-knowledge-cloud.md",
        content="# 欢迎来到 Knowledge Cloud\n\n这是你的私有知识云首页。\n\n- 用 [[系统设计总览]] 进入架构笔记\n- 用 [[研究卡片]] 浏览知识网络\n- 给今天的思考打上 #daily #inbox 标签",
        tags=[],
        links=[],
        source_url="",
    ),
    NoteCreate(
        title="系统设计总览",
        slug="system-overview",
        path="01_Notes/system-overview.md",
        content="# 系统设计总览\n\n核心链路：Web / App 编辑 -> 云端落盘 -> 主电脑同步。\n\n关联 [[RAG 工作流]] 与 [[同步状态设计]]。\n\n#architecture #sync",
        tags=[],
        links=[],
        source_url="",
    ),
    NoteCreate(
        title="RAG 工作流",
        slug="rag-workflow",
        path="02_Articles/rag-workflow.md",
        content="# RAG 工作流\n\n1. 解析 Markdown\n2. 切片\n3. 向量化\n4. 检索\n5. AI 生成\n\n引用 [[系统设计总览]]。\n\n#ai #rag",
        tags=[],
        links=[],
        source_url="",
    ),
    NoteCreate(
        title="研究卡片",
        slug="research-cards",
        path="03_Resources/research-cards.md",
        content="# 研究卡片\n\n这里沉淀 Logseq、Joplin、Blossom 的结构参考。\n\n链接 [[系统设计总览]]。\n\n#research #pkm",
        tags=[],
        links=[],
        source_url="",
    ),
    NoteCreate(
        title="同步状态设计",
        slug="sync-status-design",
        path="04_Templates/sync-status-design.md",
        content="# 同步状态设计\n\n展示 pending、acked、offline、conflict 四类状态。\n\n参见 [[系统设计总览]]。\n\n#template #sync",
        tags=[],
        links=[],
        source_url="",
    ),
]


def main() -> None:
    init_db()
    vault_sync_service.ensure_default_structure()
    with Session(engine) as session:
        for payload in DEMO_NOTES:
            note = session.exec(select(Note).where(Note.slug == payload.slug)).first()
            if note is None:
                note = Note(
                    title=payload.title,
                    slug=payload.slug,
                    content=payload.content,
                    path=payload.path,
                    parent_path=payload.path.rsplit("/", 1)[0],
                )
                session.add(note)
                session.flush()

            note.title = payload.title
            note.content = payload.content
            note.path = payload.path
            note.parent_path = payload.path.rsplit("/", 1)[0]
            note.is_deleted = False
            markdown_index_service.reindex_note(session, note)
            vault_sync_service.save_note_file(payload)

        session.commit()
    print("demo-seed-ready")


if __name__ == "__main__":
    main()
