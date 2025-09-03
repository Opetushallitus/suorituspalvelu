import { useSuspenseQuery } from '@tanstack/react-query';
import { isPlainObject } from 'remeda';
import { useIsSessionExpired } from './components/SessionExpired';

export function getCookies() {
  return document.cookie.split('; ').reduce(
    (result, cookieStr) => {
      const [key, value] = cookieStr.split(/=(.*)$/, 2).map(decodeURIComponent);
      if (key && value) {
        result[key] = value;
      }
      return result;
    },
    {} as Record<string, string>,
  );
}

// https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-2.html#support-for-newtarget
class OphCustomError extends Error {
  constructor(message?: string) {
    super(message); // 'Error' breaks prototype chain here
    Object.setPrototypeOf(this, new.target.prototype); // restore prototype chain
  }
}

export class SessionExpiredError extends OphCustomError {
  constructor(message = 'Session expired') {
    super(message);
  }
}

export class FetchError extends OphCustomError {
  response: Response;
  constructor(response: Response, message = 'Fetch error') {
    super(message);
    this.response = response;
  }
}

export type HttpClientResponse<D> = {
  headers: Headers;
  data: D;
};

const getContentFilename = (headers: Headers) => {
  const contentDisposition = headers.get('content-disposition');
  return contentDisposition?.match(/ filename="(.*)"$/)?.[1];
};

export type FileResult = {
  fileName?: string;
  blob: Blob;
};

export const createFileResult = async (
  response: HttpClientResponse<Blob>,
): Promise<FileResult> => {
  console.assert(response.data instanceof Blob, 'Response data is not a blob');
  return {
    fileName: getContentFilename(response.headers),
    blob: response.data,
  };
};

const doFetch = async (request: Request) => {
  try {
    const response = await fetch(request);
    return response.status >= 400
      ? Promise.reject(new FetchError(response, (await response.text()) ?? ''))
      : Promise.resolve(response);
  } catch (e) {
    return Promise.reject(e);
  }
};

const isUnauthenticated = (response: Response) => {
  return response?.status === 401;
};

const isRedirected = (response: Response) => {
  return response.redirected;
};

const hasNoContent = (response: Response) => {
  return response.status === 204;
};

const makeBareRequest = (request: Request) => {
  request.headers.set(
    'Caller-Id',
    '1.2.246.562.10.00000000001.valintojen-toteuttaminen',
  );
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method)) {
    const csrfCookie = getCookies()['CSRF'];
    if (csrfCookie) {
      request.headers.set('CSRF', csrfCookie);
    }
  }
  return doFetch(request);
};

type BodyParser<T> = (res: Response) => Promise<T>;

const TEXT_PARSER = (res: Response) => res.text();
const BLOB_PARSER = (response: Response) => response.blob();

const RESPONSE_BODY_PARSERS: Record<string, BodyParser<unknown>> = {
  'application/json': async (response: Response) => await response.json(),
  'application/octet-stream': BLOB_PARSER,
  'application/pdf': BLOB_PARSER,
  'application/vnd.ms-excel': BLOB_PARSER,
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet':
    BLOB_PARSER,
  'binary/octet-stream': BLOB_PARSER,
  'text/plain': TEXT_PARSER,
};

const responseToData = async <Result = unknown>(
  res: Response,
): Promise<{ headers: Headers; data: Result }> => {
  if (hasNoContent(res)) {
    return { headers: res.headers, data: undefined as Result };
  }
  const contentType =
    res.headers.get('content-type')?.split(';')?.[0] ?? 'text/plain';

  const parseBody = (RESPONSE_BODY_PARSERS?.[contentType] ??
    TEXT_PARSER) as BodyParser<Result>;

  try {
    return {
      headers: res.headers,
      data: await parseBody(res),
    };
  } catch (e) {
    console.error(`Parsing fetch response body as "${contentType}" failed!`);
    return Promise.reject(e);
  }
};

const makeRequest = async <Result>(request: Request) => {
  try {
    const response = await makeBareRequest(request);
    const responseUrl = new URL(response.url);
    if (
      isRedirected(response) &&
      responseUrl.pathname.startsWith('/cas/login')
    ) {
      throw new SessionExpiredError();
    }
    return responseToData<Result>(response);
  } catch (error: unknown) {
    if (error instanceof FetchError && isUnauthenticated(error.response)) {
      throw new SessionExpiredError();
    }
    return Promise.reject(error);
  }
};

type BodyType = BodyInit | JSONData;
type UrlType = string | URL;

export type JSONData = Record<string, unknown> | Array<unknown>;

const isJson = (val: unknown): val is JSONData =>
  Array.isArray(val) || isPlainObject(val);

const modRequest = <Result = unknown>(
  method: string,
  url: UrlType,
  body: BodyType,
  options: RequestInit,
) => {
  return makeRequest<Result>(
    new Request(url, {
      method,
      body: isJson(body) ? JSON.stringify(body) : body,
      ...options,
      headers: {
        ...(isJson(body) ? { 'content-type': 'application/json' } : {}),
        ...(options.headers ?? {}),
      },
    }),
  );
};

export const client = {
  get: <Result = unknown>(url: UrlType, options: RequestInit = {}) =>
    makeRequest<Result>(new Request(url, { method: 'GET', ...options })),
  post: <Result = unknown>(
    url: UrlType,
    body: BodyType,
    options: RequestInit = {},
  ) => modRequest<Result>('POST', url, body, options),
  put: <Result = unknown>(
    url: UrlType,
    body: BodyType,
    options: RequestInit = {},
  ) => modRequest<Result>('PUT', url, body, options),
  patch: <Result = unknown>(
    url: UrlType,
    body: BodyType,
    options: RequestInit = {},
  ) => modRequest<Result>('PATCH', url, body, options),
  delete: <Result = unknown>(url: UrlType, options: RequestInit = {}) =>
    makeRequest<Result>(new Request(url, { method: 'DELETE', ...options })),
} as const;

export const useApiSuspenseQuery: typeof useSuspenseQuery = (options) => {
  const { setIsSessionExpired } = useIsSessionExpired();

  const queryResult = useSuspenseQuery({
    ...options,
    queryFn: async (context) => {
      try {
        const result = await options?.queryFn?.(context);
        return result;
      } catch (error) {
        if (error instanceof SessionExpiredError) {
          setIsSessionExpired(true);

          // Jos autentikaatio feilasi, mutta data on jo ladattu, palautetaan aiempi data.
          // Näin voidaan näyttää vanha data UI:ssa kunnes käyttäjä kirjautuu uudelleen.
          const data = context.client.getQueryData(options.queryKey);
          if (data) {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            return data as any;
          }
        }
        throw error;
      }
    },
  });

  if (queryResult.error && !queryResult.isFetching) {
    throw queryResult.error;
  }

  return queryResult;
};
