import { configPromise } from '@/lib/configuration';

export const getOppilaitosLinkUrl = (
  config: Awaited<typeof configPromise>,
  oppilaitosOid: string,
) => {
  const baseUrl = config.routes.yleiset.organisaatioLinkUrl;
  return `${baseUrl}/${oppilaitosOid}`;
};
