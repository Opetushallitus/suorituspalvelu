import { configPromise } from '@/configuration';

export const getOppilaitosLinkUrl = (
  config: Awaited<typeof configPromise>,
  oppilaitosOid: string,
) => {
  const baseUrl = config.routes.yleiset.organisaatioLinkUrl;
  return `${baseUrl}/${oppilaitosOid}`;
};
