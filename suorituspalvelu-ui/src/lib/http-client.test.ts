import { describe, it, expect, beforeEach } from 'vitest';
import { getCookies, createFileResult } from './http-client';

describe('getCookies', () => {
  beforeEach(() => {
    Object.defineProperty(document, 'cookie', {
      writable: true,
      value: 'foo=bar; baz=qux; CSRF=token123',
    });
  });

  it('parses cookies correctly', () => {
    expect(getCookies()).toEqual({
      foo: 'bar',
      baz: 'qux',
      CSRF: 'token123',
    });
  });

  it('returns empty object if no cookies', () => {
    document.cookie = '';
    expect(getCookies()).toEqual({});
  });
});

describe('createFileResult', () => {
  it('returns fileName and blob from response', async () => {
    const headers = new Headers({
      'content-disposition': 'attachment; filename="test.pdf"',
    });
    const blob = new Blob(['data'], { type: 'application/pdf' });
    const result = await createFileResult({ headers, data: blob });
    expect(result.fileName).toBe('test.pdf');
    expect(result.blob).toBe(blob);
  });

  it('returns undefined fileName if not present', async () => {
    const headers = new Headers();
    const blob = new Blob(['data']);
    const result = await createFileResult({ headers, data: blob });
    expect(result.fileName).toBeUndefined();
    expect(result.blob).toBe(blob);
  });
});
