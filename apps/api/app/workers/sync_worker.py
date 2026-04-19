class SyncWorker:
    def enqueue_desktop_push(self, note_slug: str, version: str) -> dict[str, str]:
        return {
            "note_slug": note_slug,
            "version": version,
            "status": "queued",
        }


sync_worker = SyncWorker()
