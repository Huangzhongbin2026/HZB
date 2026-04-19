from __future__ import annotations

import re
from urllib.parse import urlparse

import httpx
from bs4 import BeautifulSoup
from markdownify import markdownify as md
from readability import Document

from app.core.config import settings


class ClipService:
    async def fetch_markdown(self, url: str) -> tuple[str, str]:
        async with httpx.AsyncClient(
            timeout=settings.clip_timeout_seconds,
            follow_redirects=True,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    "(KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"
                ),
                "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            },
        ) as client:
            response = await client.get(url)
            response.raise_for_status()

        html = self.decode_html(response, url)
        if self.is_wechat_article(url):
            title, body = self.extract_wechat_markdown(html, url)
            if body:
                return title, body

        document = Document(html)
        title = document.short_title() or url
        content_html = document.summary(html_partial=True)
        markdown = md(content_html, heading_style="ATX")
        markdown = re.sub(r"\n{3,}", "\n\n", markdown).strip()
        body = f"# {title}\n\n来源: {url}\n\n{markdown}\n"
        return title, body

    def decode_html(self, response: httpx.Response, url: str) -> str:
        if self.is_wechat_article(url):
            return response.content.decode("utf-8", errors="ignore")
        return response.text

    def is_wechat_article(self, url: str) -> bool:
        return "mp.weixin.qq.com" in urlparse(url).netloc

    def extract_wechat_markdown(self, html: str, url: str) -> tuple[str, str]:
        soup = BeautifulSoup(html, "html.parser")
        title = self.extract_wechat_title(soup, url)
        content = soup.select_one("#js_content")
        if content is None:
            return title, ""

        for node in content.select("script, style, noscript"):
            node.decompose()

        for iframe in content.find_all("iframe"):
            video_url = iframe.get("data-src") or iframe.get("src")
            placeholder = soup.new_tag("p")
            placeholder.string = f"视频: {video_url}" if video_url else "视频"
            iframe.replace_with(placeholder)

        for image in content.find_all("img"):
            source = image.get("data-src") or image.get("src")
            if source:
                image["src"] = source

        markdown = md(str(content), heading_style="ATX")
        markdown = re.sub(r"\n{3,}", "\n\n", markdown)
        markdown = re.sub(r"^(?:\s*[，。、：；]\s*)+$", "", markdown, flags=re.MULTILINE)
        markdown = markdown.strip()
        if not markdown:
            return title, ""
        return title, f"# {title}\n\n来源: {url}\n\n{markdown}\n"

    def extract_wechat_title(self, soup: BeautifulSoup, url: str) -> str:
        meta_title = soup.select_one('meta[property="og:title"]')
        if meta_title and meta_title.get("content"):
            return meta_title["content"].strip()

        heading = soup.select_one("#activity-name")
        if heading:
            text = heading.get_text(" ", strip=True)
            if text:
                return text

        return url


clip_service = ClipService()