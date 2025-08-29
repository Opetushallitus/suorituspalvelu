import {
  useOppijatSearch,
  useOppijatSearchURLParams,
} from '@/hooks/useSearchOppijat';
import { Link, useParams } from 'react-router';
import { QuerySuspenseBoundary } from './QuerySuspenseBoundary';
import { LeftPanel } from './LeftPanel';
import { useState } from 'react';
import { NAV_LIST_SELECTED_ITEM_CLASS, NavigationList } from './NavigationList';
import { useTranslate } from '@tolgee/react';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';

const HenkilotSidebarContent = () => {
  const params = useOppijatSearchURLParams();
  const { result, hasEmptySearchParams } = useOppijatSearch();

  const { t } = useTranslate();

  const { oppijaNumero } = useParams();

  return hasEmptySearchParams ? (
    <div></div>
  ) : (
    <div>
      <OphTypography variant="body2" sx={{ paddingBottom: 1, margin: 0 }}>
        {t('sivupalkki.henkilo-maara', {
          count: result.data.oppijat?.length ?? 0,
        })}
      </OphTypography>
      <NavigationList tabIndex={0} aria-label={t('sivupalkki.navigaatio')}>
        {result.data.oppijat?.map((oppija) => (
          <Link
            key={oppija.oppijaNumero}
            className={
              oppijaNumero === oppija.oppijaNumero
                ? NAV_LIST_SELECTED_ITEM_CLASS
                : ''
            }
            to={{
              pathname: `/henkilo/${oppija.oppijaNumero}`,
              search: new URLSearchParams({
                ...params,
              }).toString(),
            }}
          >
            <OphTypography variant="label" color="inherit">
              {oppija.nimi}
            </OphTypography>
            <OphTypography color={ophColors.black}>{oppija.hetu}</OphTypography>
          </Link>
        ))}
      </NavigationList>
    </div>
  );
};

export function HenkilotSidebar() {
  const [isOpen, setIsOpen] = useState(true);

  return (
    <LeftPanel isOpen={isOpen} setIsOpen={setIsOpen}>
      <QuerySuspenseBoundary>
        <HenkilotSidebarContent />
      </QuerySuspenseBoundary>
    </LeftPanel>
  );
}
