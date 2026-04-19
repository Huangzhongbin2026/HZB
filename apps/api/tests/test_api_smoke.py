from uuid import uuid4
from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import app
from app.services.ai import ai_service
from app.services.clipper import clip_service


def auth_headers(client: TestClient) -> dict[str, str]:
    response = client.post(
        "/api/v1/auth/login",
        json={"username": "admin", "password": "admin123456"},
    )
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


def test_auth_note_search_and_rag_flow() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)
        create_response = client.post(
            "/api/v1/notes",
            headers=headers,
            json={
                "title": "测试笔记",
                "slug": "test-note",
                "content": "# 测试笔记\n\n关联 [[上线清单]] 并添加 #测试 标签。",
                "path": "01_Notes/test-note.md",
                "tags": [],
                "links": [],
                "source_url": "",
            },
        )
        assert create_response.status_code in {200, 409}

        search_response = client.get("/api/v1/search", params={"q": "测试"}, headers=headers)
        assert search_response.status_code == 200
        assert search_response.json()["results"]

        rag_response = client.post(
            "/api/v1/rag/query",
            headers=headers,
            json={"query": "测试 上线", "top_k": 3},
        )
        assert rag_response.status_code == 200
        assert "hits" in rag_response.json()


def test_sync_status_and_tree() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)
        tree_response = client.get("/api/v1/tree", headers=headers)
        sync_response = client.get("/api/v1/sync/status", headers=headers)

        assert tree_response.status_code == 200
        assert any(item["path"] == "00_Inbox" for item in tree_response.json())
        assert sync_response.status_code == 200
        assert "pending_events" in sync_response.json()


def test_move_note_flow() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)
        create_response = client.post(
            "/api/v1/notes",
            headers=headers,
            json={
                "title": "移动测试",
                "slug": "move-note-test",
                "content": "# 移动测试\n\n用于验证移动接口。",
                "path": "01_Notes/move-note-test.md",
                "tags": [],
                "links": [],
                "source_url": "",
            },
        )
        assert create_response.status_code in {200, 409}

        note = client.get("/api/v1/notes/move-note-test", headers=headers).json()
        move_response = client.put(
            "/api/v1/notes/move-note-test/move",
            headers=headers,
            json={
                "target_path": "02_Articles/move-note-test.md",
                "previous_version": note["version"],
            },
        )
        assert move_response.status_code == 200
        assert move_response.json()["path"] == "02_Articles/move-note-test.md"


def test_ai_provider_and_document_upload_flow() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)

        provider_response = client.put(
            "/api/v1/ai/providers/current",
            headers=headers,
            json={
                "provider_id": "ollama",
                "provider_label": "Ollama",
                "base_url": "http://127.0.0.1:11434/v1",
                "api_key": "local-key",
                "model_name": "qwen2.5:7b",
                "is_enabled": False,
            },
        )
        assert provider_response.status_code == 200
        assert provider_response.json()["provider_id"] == "ollama"

        upload_response = client.post(
            "/api/v1/documents/upload",
            headers=headers,
            data={
                "summarize_with_ai": "false",
                "save_to_note": "true",
                "target_folder": "03_Resources",
            },
            files={
                "file": ("brief.txt", "第一部分：需求背景\n第二部分：关键结论\n第三部分：执行建议".encode("utf-8"), "text/plain")
            },
        )
        assert upload_response.status_code == 200
        assert upload_response.json()["extracted_title"] == "brief"
        assert upload_response.json()["saved_note"] is not None


def test_space_slug_persists_across_saved_content_flows() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)

        space_response = client.post(
            '/api/v1/instance/spaces',
            headers=headers,
            json={
                'name': '项目空间',
                'slug': f'project-space-{uuid4().hex[:8]}',
                'visibility': 'private',
            },
        )
        assert space_response.status_code == 200
        target_space_slug = space_response.json()['slug']

        note_response = client.post(
            '/api/v1/notes',
            headers=headers,
            json={
                'title': '空间测试笔记',
                'slug': f'space-note-{uuid4().hex[:8]}',
                'content': '# 空间测试',
                'path': '01_Notes/space-note.md',
                'space_slug': target_space_slug,
                'tags': [],
                'links': [],
                'source_url': '',
            },
        )
        assert note_response.status_code == 200
        created_slug = note_response.json()['slug']

        created_sharing = client.get(f'/api/v1/notes/{created_slug}/sharing', headers=headers)
        assert created_sharing.status_code == 200
        assert created_sharing.json()['space_slug'] == target_space_slug

        clip_save_response = client.post(
            '/api/v1/clip/save-preview',
            headers=headers,
            json={
                'title': '空间剪藏',
                'source_url': f'https://example.com/{uuid4().hex}',
                'content': '# 空间剪藏\n\n正文',
                'summary': '摘要',
                'target_folder': '00_Inbox',
                'space_slug': target_space_slug,
                'device_id': 'web-clipper',
            },
        )
        assert clip_save_response.status_code == 200
        clip_slug = clip_save_response.json()['note']['slug']
        clip_sharing = client.get(f'/api/v1/notes/{clip_slug}/sharing', headers=headers)
        assert clip_sharing.status_code == 200
        assert clip_sharing.json()['space_slug'] == target_space_slug

        ai_response = client.post(
            '/api/v1/ai/generate',
            headers=headers,
            json={
                'action': 'create',
                'prompt': '写一篇空间测试草稿',
                'save_as_note': True,
                'target_folder': '00_Inbox',
                'space_slug': target_space_slug,
                'use_ai': False,
                'use_rag': False,
            },
        )
        assert ai_response.status_code == 200
        ai_slug = ai_response.json()['saved_note']['slug']
        ai_sharing = client.get(f'/api/v1/notes/{ai_slug}/sharing', headers=headers)
        assert ai_sharing.status_code == 200
        assert ai_sharing.json()['space_slug'] == target_space_slug

        upload_response = client.post(
            '/api/v1/documents/upload',
            headers=headers,
            data={
                'summarize_with_ai': 'false',
                'save_to_note': 'true',
                'target_folder': '03_Resources',
                'space_slug': target_space_slug,
            },
            files={
                'file': ('space-brief.txt', '空间归档内容'.encode('utf-8'), 'text/plain')
            },
        )
        assert upload_response.status_code == 200
        upload_slug = upload_response.json()['saved_note']['slug']
        upload_sharing = client.get(f'/api/v1/notes/{upload_slug}/sharing', headers=headers)
        assert upload_sharing.status_code == 200
        assert upload_sharing.json()['space_slug'] == target_space_slug


def test_clip_preview_and_save_flow() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)

        with patch('app.api.v1.routes.clip_service.fetch_markdown', new=AsyncMock(return_value=("示例网页", "# 示例网页\n\n正文内容\n\n![图像](https://example.com/demo.png)"))):
            preview_response = client.post(
                "/api/v1/clip",
                headers=headers,
                json={
                    "url": "https://example.com",
                    "device_id": "web-clipper",
                    "summarize_with_ai": False,
                    "save_to_note": False,
                },
            )
        assert preview_response.status_code == 200
        preview_payload = preview_response.json()
        assert preview_payload["saved"] is False
        assert preview_payload["preview_content"]

        save_response = client.post(
            "/api/v1/clip/save-preview",
            headers=headers,
            json={
                "title": preview_payload["extracted_title"],
                "source_url": preview_payload["source_url"],
                "content": preview_payload["preview_content"],
                "summary": preview_payload["summary"],
                "target_folder": "00_Inbox",
                "device_id": "web-clipper",
            },
        )
        assert save_response.status_code == 200
        assert save_response.json()["saved"] is True
        assert save_response.json()["note"] is not None


def test_ai_provider_preserves_existing_key_when_empty() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)

        first_save = client.put(
            "/api/v1/ai/providers/current",
            headers=headers,
            json={
                "provider_id": "openai",
                "provider_label": "OpenAI",
                "base_url": "https://api.openai.com/v1",
                "api_key": "sk-test-12345678",
                "model_name": "gpt-4.1-mini",
                "is_enabled": True,
            },
        )
        assert first_save.status_code == 200

        second_save = client.put(
            "/api/v1/ai/providers/current",
            headers=headers,
            json={
                "provider_id": "openai",
                "provider_label": "OpenAI",
                "base_url": "https://api.openai.com/v1",
                "api_key": "",
                "model_name": "gpt-5",
                "is_enabled": True,
            },
        )
        assert second_save.status_code == 200
        assert second_save.json()["has_api_key"] is True


def test_topic_plugin_flow() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)

        plugin_list = client.get('/api/v1/plugins/configs', headers=headers)
        assert plugin_list.status_code == 200
        assert any(item['plugin_id'] == 'topics' for item in plugin_list.json())

        enable_plugin = client.put('/api/v1/plugins/configs/topics', headers=headers, json={'is_enabled': True})
        assert enable_plugin.status_code == 200
        assert enable_plugin.json()['is_enabled'] is True

        topic_response = client.post(
            '/api/v1/topics',
            headers=headers,
            json={
                'title': 'AI 编程工作流选题',
                'domain': 'AI 编程',
                'keywords': ['Copilot', '私有化知识库'],
                'status': 'writable',
                'priority': 'high',
                'heat_score': 88,
                'trend_summary': '结合 AI 编程实践与知识沉淀闭环。',
                'notes': '先写工作流，再补案例。',
                'linked_note_slug': '',
                'linked_password_entry_ids': [],
                'completed_note_slug': '',
                'review_status': 'none',
                'source_type': 'manual',
            },
        )
        assert topic_response.status_code == 200
        topic_id = topic_response.json()['id']

        outline_response = client.post(
            '/api/v1/topics/ai/outline',
            headers=headers,
            json={
                'title': 'AI 编程工作流选题',
                'domain': 'AI 编程',
                'keywords': ['Copilot', '私有化知识库'],
                'trend_summary': '结合 AI 编程实践与知识沉淀闭环。',
                'use_ai': False,
            },
        )
        assert outline_response.status_code == 200
        assert '写作结构' in outline_response.json()['outline']

        update_status = client.post(
            f'/api/v1/topics/{topic_id}/status',
            headers=headers,
            json={'status': 'completed'},
        )
        assert update_status.status_code == 200
        assert update_status.json()['status'] == 'completed'

        overview = client.get('/api/v1/topics/overview', headers=headers)
        assert overview.status_code == 200
        assert overview.json()['plugin']['is_enabled'] is True
        assert overview.json()['stats']['completed'] >= 1
        assert any(item['id'] == topic_id for item in overview.json()['topics'])


def test_idea_plugin_flow() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)

        plugin_list = client.get('/api/v1/plugins/configs', headers=headers)
        assert plugin_list.status_code == 200
        assert any(item['plugin_id'] == 'ideas' for item in plugin_list.json())

        enable_plugin = client.put('/api/v1/plugins/configs/ideas', headers=headers, json={'is_enabled': True})
        assert enable_plugin.status_code == 200
        assert enable_plugin.json()['is_enabled'] is True

        idea_response = client.post(
            '/api/v1/ideas',
            headers=headers,
            json={
                'title': '创意插件快速捕捉入口',
                'summary': '给系统增加一个轻量浮层，用来快速记录需求与创业点子。',
                'details': '要求支持分类、状态、优先级、知识库关联和负责人。',
                'idea_type': 'product_opportunity',
                'tags': ['插件', '需求管理'],
                'status': 'pending_review',
                'priority': 'high',
                'value_score': 92,
                'effort_score': 36,
                'business_score': 85,
                'linked_note_slug': '',
                'linked_goal_id': None,
                'linked_topic_id': None,
                'source_context': '用户在工作台中临时记录',
                'visibility_scope': 'private',
                'next_step': '先完成 MVI 工作台',
            },
        )
        assert idea_response.status_code == 200
        idea_id = idea_response.json()['id']
        assert idea_response.json()['opportunity_score'] >= 70

        accept_response = client.post(
            f'/api/v1/ideas/{idea_id}/status',
            headers=headers,
            json={'status': 'accepted'},
        )
        assert accept_response.status_code == 200
        assert accept_response.json()['status'] == 'accepted'

        update_response = client.put(
            f'/api/v1/ideas/{idea_id}',
            headers=headers,
            json={
                'title': '创意插件快速捕捉入口',
                'summary': '补充规划阶段字段。',
                'details': '需要与目标、选题、笔记联动，支持后续团队协作扩展。',
                'idea_type': 'product_opportunity',
                'tags': ['插件', '工作台'],
                'status': 'planning',
                'priority': 'high',
                'value_score': 92,
                'effort_score': 40,
                'business_score': 88,
                'linked_note_slug': '',
                'linked_goal_id': None,
                'linked_topic_id': None,
                'source_context': '产品需求池',
                'visibility_scope': 'private',
                'assignee_user_id': None,
                'next_step': '拆成后端和前端两段实现',
            },
        )
        assert update_response.status_code == 200
        assert update_response.json()['status'] == 'planning'

        overview = client.get('/api/v1/ideas/overview', headers=headers)
        assert overview.status_code == 200
        assert overview.json()['plugin']['is_enabled'] is True
        assert overview.json()['stats']['planning'] >= 1
        assert any(item['id'] == idea_id for item in overview.json()['ideas'])


def test_extract_responses_text_from_reasoning_payload() -> None:
    payload = {
        "output": [
            {"type": "reasoning", "summary": []},
            {
                "type": "message",
                "content": [
                    {
                        "type": "output_text",
                        "text": "人工智能是让机器具备学习与推理能力的技术。",
                    }
                ],
            },
        ]
    }

    assert ai_service.extract_responses_text(payload) == "人工智能是让机器具备学习与推理能力的技术。"


def test_extract_wechat_markdown_prefers_js_content() -> None:
    html = """
    <html>
        <head>
            <meta property="og:title" content="OpenAI 发布首个生命科学模型 GPT-Rosalind" />
        </head>
        <body>
            <div id="js_content">
                <p>今天，OpenAI 发布了 GPT-Rosalind。</p>
                <p>这是一个面向生命科学的专用模型。</p>
            </div>
        </body>
    </html>
    """

    title, body = clip_service.extract_wechat_markdown(html, "https://mp.weixin.qq.com/s/demo")

    assert title == "OpenAI 发布首个生命科学模型 GPT-Rosalind"
    assert "GPT-Rosalind" in body
    assert "生命科学" in body


def test_ai_generate_falls_back_to_local_when_remote_fails() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)
        with patch('app.services.ai.ai_service.remote_completion', new=AsyncMock(side_effect=RuntimeError('upstream failed'))):
            response = client.post(
                '/api/v1/ai/generate',
                headers=headers,
                json={
                    'action': 'summary',
                    'prompt': '总结一下',
                    'use_ai': True,
                    'use_rag': False,
                },
            )

        assert response.status_code == 200
        assert '总结' in response.json()['content']


def test_instance_bootstrap_requires_team_authorization_code() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)
        original_code = settings.team_authorization_code
        settings.team_authorization_code = 'TEAM-CODE-2026'
        try:
            denied = client.put(
                '/api/v1/instance/bootstrap',
                headers=headers,
                json={
                    'instance_name': '团队知识库',
                    'deployment_mode': 'server',
                    'edition': 'team',
                    'authorization_code': 'wrong-code',
                },
            )
            assert denied.status_code == 403

            success = client.put(
                '/api/v1/instance/bootstrap',
                headers=headers,
                json={
                    'instance_name': '团队知识库',
                    'deployment_mode': 'server',
                    'edition': 'team',
                    'authorization_code': 'TEAM-CODE-2026',
                },
            )
            assert success.status_code == 200
            assert success.json()['edition'] == 'team'
            assert success.json()['server_host_enabled'] is True

            public_config = client.get('/api/v1/instance/config')
            assert public_config.status_code == 200
            assert public_config.json()['team_license_verified'] is True
        finally:
            settings.team_authorization_code = original_code


def test_team_sharing_affects_search_and_rag_visibility() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)
        original_code = settings.team_authorization_code
        settings.team_authorization_code = 'TEAM-CODE-2026'
        try:
            bootstrap = client.put(
                '/api/v1/instance/bootstrap',
                headers=headers,
                json={
                    'instance_name': '团队知识库',
                    'deployment_mode': 'server',
                    'edition': 'team',
                    'authorization_code': 'TEAM-CODE-2026',
                },
            )
            assert bootstrap.status_code == 200

            invite = client.post('/api/v1/instance/team/invites', headers=headers, json={'role': 'member', 'expires_in_hours': 24})
            assert invite.status_code == 200

            member_username = f'member-{uuid4().hex[:8]}'
            accepted = client.post(
                '/api/v1/instance/team/invites/accept',
                json={
                    'invite_code': invite.json()['invite_code'],
                    'username': member_username,
                    'password': 'member123456',
                    'display_name': 'Team Member',
                },
            )
            assert accepted.status_code == 200

            member_login = client.post(
                '/api/v1/auth/login',
                json={'username': member_username, 'password': 'member123456'},
            )
            assert member_login.status_code == 200
            member_headers = {'Authorization': f"Bearer {member_login.json()['access_token']}"}

            shared_slug = f'shared-{uuid4().hex[:10]}'
            private_slug = f'private-{uuid4().hex[:10]}'
            shared_keyword = f'共享北极星-{uuid4().hex[:6]}'
            private_keyword = f'机密鲸鱼-{uuid4().hex[:6]}'

            shared_note = client.post(
                '/api/v1/notes',
                headers=headers,
                json={
                    'title': '团队共享笔记',
                    'slug': shared_slug,
                    'content': f'# 团队共享笔记\n\n{shared_keyword} 只应该在团队共享后被成员看到。',
                    'path': f'01_Notes/{shared_slug}.md',
                    'tags': [],
                    'links': [],
                    'source_url': '',
                },
            )
            assert shared_note.status_code == 200

            private_note = client.post(
                '/api/v1/notes',
                headers=headers,
                json={
                    'title': '管理员私有笔记',
                    'slug': private_slug,
                    'content': f'# 管理员私有笔记\n\n{private_keyword} 不应该被团队成员检索到。',
                    'path': f'01_Notes/{private_slug}.md',
                    'tags': [],
                    'links': [],
                    'source_url': '',
                },
            )
            assert private_note.status_code == 200

            sharing = client.put(
                f'/api/v1/notes/{shared_slug}/sharing',
                headers=headers,
                json={'visibility_scope': 'team', 'shared_member_ids': []},
            )
            assert sharing.status_code == 200
            assert sharing.json()['visibility_scope'] == 'team'

            search_shared = client.get('/api/v1/search', headers=member_headers, params={'q': shared_keyword})
            assert search_shared.status_code == 200
            assert any(item['note_slug'] == shared_slug for item in search_shared.json()['results'])

            search_private = client.get('/api/v1/search', headers=member_headers, params={'q': private_keyword})
            assert search_private.status_code == 200
            assert all(item['note_slug'] != private_slug for item in search_private.json()['results'])

            rag_response = client.post(
                '/api/v1/rag/query',
                headers=member_headers,
                json={'query': shared_keyword, 'top_k': 5},
            )
            assert rag_response.status_code == 200
            assert any(item['note_slug'] == shared_slug for item in rag_response.json()['hits'])
        finally:
            settings.team_authorization_code = original_code


def test_password_vault_flow() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)
        master_password = 'MasterPwd#2026'
        title = f'GitHub-{uuid4().hex[:6]}'

        config_response = client.get('/api/v1/passwords/config', headers=headers)
        assert config_response.status_code == 200

        if not config_response.json()['is_initialized']:
            setup_response = client.post(
                '/api/v1/passwords/config/setup',
                headers=headers,
                json={'master_password': master_password},
            )
            assert setup_response.status_code == 200

        verify_response = client.post(
            '/api/v1/passwords/config/verify',
            headers=headers,
            json={'master_password': master_password},
        )
        assert verify_response.status_code == 200
        if config_response.json()['is_initialized'] and not verify_response.json()['verified']:
            pytest.skip('当前环境已存在不同主密码的密码库，跳过持久环境冲突测试。')
        assert verify_response.json()['verified'] is True

        create_response = client.post(
            '/api/v1/passwords',
            headers=headers,
            json={
                'title': title,
                'username': 'tester@example.com',
                'password': 'EntryPwd#123',
                'master_password': master_password,
                'category': 'work',
                'website': 'GitHub',
                'url': 'https://github.com/login',
                'notes': '密码烟测',
                'linked_note_slug': '',
                'vault_scope': 'private',
                'shared_member_ids': [],
                'editor_member_ids': [],
            },
        )
        assert create_response.status_code == 200
        entry_id = create_response.json()['id']

        reveal_response = client.post(
            f'/api/v1/passwords/{entry_id}/reveal',
            headers=headers,
            json={'master_password': master_password},
        )
        assert reveal_response.status_code == 200
        assert reveal_response.json()['password'] == 'EntryPwd#123'

        copy_response = client.post(f'/api/v1/passwords/{entry_id}/copy', headers=headers)
        assert copy_response.status_code == 200

        audits_response = client.get(f'/api/v1/passwords/{entry_id}/audits', headers=headers)
        assert audits_response.status_code == 200
        assert any(item['action'] == 'copy' for item in audits_response.json())


def test_goal_planning_and_journal_flow() -> None:
    with TestClient(app) as client:
        headers = auth_headers(client)
        goal_title = f'年度目标-{uuid4().hex[:6]}'

        ai_plan_response = client.post(
            '/api/v1/goals/ai/plan',
            headers=headers,
            json={
                'title': goal_title,
                'vision': '提升知识库产品的日更与执行力。',
                'key_results': ['连续 30 天记录日记', '每周推进 3 个关键任务'],
                'use_ai': False,
            },
        )
        assert ai_plan_response.status_code == 200
        assert ai_plan_response.json()['plans']

        goal_response = client.post(
            '/api/v1/goals',
            headers=headers,
            json={
                'title': goal_title,
                'vision': '提升知识库产品的日更与执行力。',
                'key_results': ['连续 30 天记录日记', '每周推进 3 个关键任务'],
                'priority': 'high',
                'visibility_scope': 'private',
            },
        )
        assert goal_response.status_code == 200
        goal_id = goal_response.json()['id']

        plan_response = client.post(
            f'/api/v1/goals/{goal_id}/plans',
            headers=headers,
            json={
                'title': '第一阶段',
                'summary': '建立日计划与复盘闭环',
                'priority': 'high',
            },
        )
        assert plan_response.status_code == 200
        plan_id = plan_response.json()['id']

        task_response = client.post(
            '/api/v1/goal-tasks',
            headers=headers,
            json={
                'goal_id': goal_id,
                'plan_id': plan_id,
                'title': '完成首页目标看板',
                'details': '联动日记与任务状态',
                'priority': 'high',
            },
        )
        assert task_response.status_code == 200
        task_id = task_response.json()['id']

        toggle_response = client.post(
            f'/api/v1/goal-tasks/{task_id}/toggle',
            headers=headers,
            json={'done': True},
        )
        assert toggle_response.status_code == 200
        assert toggle_response.json()['status'] == 'done'

        journal_date = '2026-01-15'
        journal_response = client.put(
            '/api/v1/goal-journals',
            headers=headers,
            json={
                'journal_date': journal_date,
                'goal_id': goal_id,
                'plan_id': plan_id,
                'reflection': '今天完成了首页目标计划模块的主界面串联。',
                'task_ids': [task_id],
            },
        )
        assert journal_response.status_code == 200
        assert journal_response.json()['task_ids'] == [task_id]

        overview_response = client.get('/api/v1/goals/overview', headers=headers)
        assert overview_response.status_code == 200
        payload = overview_response.json()
        assert any(item['id'] == goal_id for item in payload['goals'])
        assert any(item['id'] == plan_id for item in payload['plans'])
        assert any(item['id'] == journal_response.json()['id'] for item in payload['journals'])
