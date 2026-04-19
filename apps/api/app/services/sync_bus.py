from __future__ import annotations

import asyncio
from collections import defaultdict

from fastapi import WebSocket


class SyncConnectionManager:
    def __init__(self) -> None:
        self.connections: dict[str, set[WebSocket]] = defaultdict(set)
        self._lock = asyncio.Lock()

    async def connect(self, device_id: str, websocket: WebSocket) -> None:
        await websocket.accept()
        async with self._lock:
            self.connections[device_id].add(websocket)

    async def disconnect(self, device_id: str, websocket: WebSocket) -> None:
        async with self._lock:
            if device_id in self.connections:
                self.connections[device_id].discard(websocket)
                if not self.connections[device_id]:
                    self.connections.pop(device_id, None)

    async def push_event(self, payload: dict) -> None:
        async with self._lock:
            targets = [socket for sockets in self.connections.values() for socket in sockets]

        stale: list[tuple[str, WebSocket]] = []
        for device_id, sockets in list(self.connections.items()):
            for socket in list(sockets):
                try:
                    await socket.send_json(payload)
                except Exception:
                    stale.append((device_id, socket))

        for device_id, socket in stale:
            await self.disconnect(device_id, socket)

    def connected_desktop_devices(self) -> int:
        return len(self.connections)


sync_connection_manager = SyncConnectionManager()