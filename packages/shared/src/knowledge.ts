export type SyncStatus = 'pending' | 'acked' | 'failed'

export interface NoteEntity {
  id: string
  title: string
  slug: string
  path: string
  tags: string[]
  links: string[]
  updatedAt: string
}

export interface SyncEvent {
  id: string
  noteSlug: string
  source: 'web' | 'mobile' | 'desktop'
  version: string
  status: SyncStatus
}
