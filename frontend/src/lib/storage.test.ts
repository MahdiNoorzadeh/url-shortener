import { afterEach, describe, expect, it } from 'vitest'
import { exportDashboard, importDashboard, parseLinks, readDashboard, STORAGE_KEY, writeDashboard, type SavedLink } from './storage'

const sample: SavedLink = {
  id: 'id-1', shortCode: 'abc1234', shortUrl: 'http://localhost:8080/abc1234', originalUrl: 'https://example.com/path',
  displayName: 'Example', createdAt: '2026-10-22T10:00:00Z', expiresAt: null,
}

afterEach(() => localStorage.clear())

describe('browser dashboard storage', () => {
  it('writes and reads versioned dashboard data', () => {
    writeDashboard([sample])
    expect(readDashboard().links).toEqual([sample])
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY)!).version).toBe(1)
  })
  it('rejects unsafe URLs and malformed entries', () => {
    expect(() => parseLinks([{ ...sample, originalUrl: 'javascript:alert(1)' }])).toThrow()
  })
  it('round-trips export/import JSON', () => {
    expect(importDashboard(exportDashboard([sample]))).toEqual([sample])
  })
  it('does not overwrite corrupted storage during read', () => {
    localStorage.setItem(STORAGE_KEY, '{broken')
    expect(readDashboard().warning).toBeTruthy()
    expect(localStorage.getItem(STORAGE_KEY)).toBe('{broken')
  })
})
