import type {
  DIASuoritus,
  EBOppiaine,
  IBOppiaine,
  DIAOppiaine,
  LukionOppiaine,
  LukioSuoritus,
  YOKoe,
  Kielistetty,
} from '@/types/ui-types';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { ophColors } from '@opetushallitus/oph-design-system';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { useTranslations } from '@/hooks/useTranslations';
import { LabeledInfoItem } from '../LabeledInfoItem';
import { styled } from '@/lib/theme';
import { TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { StripedTable } from '../StripedTable';
import { isEmpty } from 'remeda';
import { pointToComma, formatFinnishDate } from '@/lib/common';

const UnorderedList = styled('ul')(({ theme }) => ({
  margin: 0,
  paddingLeft: theme.spacing(2),
}));

const LukionOppiaineetList = ({
  oppiaineet,
}: {
  oppiaineet: Array<LukionOppiaine>;
}) => {
  const { t, translateKielistetty } = useTranslations();
  return (
    <LabeledInfoItem
      label={t('oppija.oppiaineet')}
      value={
        <UnorderedList>
          {oppiaineet.map((oppiaine) => (
            <li key={oppiaine.tunniste}>
              {translateKielistetty(oppiaine.nimi)}
            </li>
          ))}
        </UnorderedList>
      }
    />
  );
};

const IndentedCell = styled(TableCell)(({ theme }) => ({
  textIndent: theme.spacing(1),
}));

const EBOppiaineetTable = ({
  oppiaineet,
}: {
  oppiaineet: Array<EBOppiaine>;
}) => {
  const { t, translateKielistetty } = useTranslations();

  return (
    <StripedTable stripeGroup="body">
      <TableHead>
        <TableRow>
          <TableCell>{t('oppija.oppiaine')}</TableCell>
          <TableCell>{t('oppija.suorituskieli')}</TableCell>
          <TableCell>
            {t('oppija.laajuus-yksikolla', {
              unit: t('oppija.lyhenne-vuosiviikkotunti'),
            })}
          </TableCell>
          <TableCell>{t('oppija.arvosana')}</TableCell>
        </TableRow>
      </TableHead>
      {oppiaineet.map((row) => (
        <TableBody key={row.tunniste}>
          <TableRow>
            <TableCell>{translateKielistetty(row.nimi)}</TableCell>
            <TableCell>{row.suorituskieli}</TableCell>
            <TableCell>{row.laajuus}</TableCell>
            <TableCell />
          </TableRow>
          <TableRow>
            <IndentedCell colSpan={3}>{t('oppija.eb-written')}</IndentedCell>
            <TableCell>{pointToComma(row.written?.arvosana)}</TableCell>
          </TableRow>
          <TableRow>
            <IndentedCell colSpan={3}>{t('oppija.eb-oral')}</IndentedCell>
            <TableCell>{pointToComma(row.oral?.arvosana)}</TableCell>
          </TableRow>
          <TableRow>
            <IndentedCell colSpan={3}>{t('oppija.eb-final')}</IndentedCell>
            <TableCell>{pointToComma(row.final?.arvosana)}</TableCell>
          </TableRow>
        </TableBody>
      ))}
    </StripedTable>
  );
};

const IBOppiaineetTable = ({
  oppiaineet,
}: {
  oppiaineet: Array<IBOppiaine>;
}) => {
  const { t, translateKielistetty } = useTranslations();

  return (
    <StripedTable stripeGroup="body">
      <TableHead>
        <TableRow>
          <TableCell>{t('oppija.oppiaine')}</TableCell>
          <TableCell>
            {t('oppija.laajuus-yksikolla', {
              unit: t('oppija.lyhenne-vuosiviikkotunti'),
            })}
          </TableCell>
          <TableCell>{t('oppija.predicted-grade')}</TableCell>
          <TableCell>{t('oppija.arvosana')}</TableCell>
        </TableRow>
      </TableHead>
      {oppiaineet.map((oppiaine) => (
        <TableBody key={oppiaine.tunniste}>
          <TableRow>
            <TableCell colSpan={4}>
              {translateKielistetty(oppiaine.nimi)}
            </TableCell>
          </TableRow>
          {oppiaine.suoritukset.map((suoritus) => (
            <TableRow key={suoritus.tunniste}>
              <IndentedCell>{translateKielistetty(suoritus.nimi)}</IndentedCell>
              <TableCell>{suoritus.laajuus}</TableCell>
              <TableCell>{suoritus.predictedGrade}</TableCell>
              <TableCell>{suoritus.arvosana}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      ))}
    </StripedTable>
  );
};

interface DiaOppiaineCategory {
  label: string;
  oppiaineet: Array<DIAOppiaine>;
}

const DiaOppiaineSection = ({
  category,
  translateKielistetty,
}: {
  category: DiaOppiaineCategory;
  translateKielistetty: (kielistetty: Kielistetty) => string;
}) => {
  if (isEmpty(category.oppiaineet)) {
    return null;
  }

  return (
    <TableBody>
      <TableRow>
        <TableCell colSpan={5}>{category.label}</TableCell>
      </TableRow>
      {category.oppiaineet.map((oppiaine) => (
        <TableRow key={oppiaine.tunniste}>
          <IndentedCell>{translateKielistetty(oppiaine.nimi)}</IndentedCell>
          <TableCell>{oppiaine?.kirjallinen}</TableCell>
          <TableCell>{oppiaine?.suullinen}</TableCell>
          <TableCell>{oppiaine?.vastaavuustodistus}</TableCell>
          <TableCell>{oppiaine.laajuus}</TableCell>
        </TableRow>
      ))}
    </TableBody>
  );
};

function DiaTutkintoOppiaineet({ suoritus }: { suoritus: DIASuoritus }) {
  const { t, translateKielistetty } = useTranslations();

  const categories: Array<DiaOppiaineCategory> = [
    {
      label: t('oppija.kielet-kirjallisuus-taide'),
      oppiaineet: suoritus.kieletKirjallisuusTaide || [],
    },
    {
      label: t('oppija.matematiikka-ja-luonnontieteet'),
      oppiaineet: suoritus.matematiikkaLuonnontieteet || [],
    },
    {
      label: t('oppija.yhteiskuntatieteet'),
      oppiaineet: suoritus.yhteiskuntatieteet || [],
    },
  ];

  return (
    <StripedTable stripeGroup="body">
      <TableHead>
        <TableRow>
          <TableCell>{t('oppija.oppiaine')}</TableCell>
          <TableCell>{t('oppija.kirjallinen')}</TableCell>
          <TableCell>{t('oppija.suullinen')}</TableCell>
          <TableCell>{t('oppija.vastaavuustodistus')}</TableCell>
          <TableCell>
            {t('oppija.laajuus-yksikolla', {
              unit: t('oppija.lyhenne-vuosiviikkotunti'),
            })}
          </TableCell>
        </TableRow>
      </TableHead>
      {categories.map((category) => (
        <DiaOppiaineSection
          key={category.label}
          category={category}
          translateKielistetty={translateKielistetty}
        />
      ))}
    </StripedTable>
  );
}

function LukionOppiaineet({ suoritus }: { suoritus: LukioSuoritus }) {
  if ('oppiaineet' in suoritus) {
    switch (suoritus.koulutustyyppi) {
      case 'eb':
        return <EBOppiaineetTable oppiaineet={suoritus.oppiaineet} />;
      case 'ib':
        return <IBOppiaineetTable oppiaineet={suoritus.oppiaineet} />;
      default:
        return <LukionOppiaineetList oppiaineet={suoritus.oppiaineet} />;
    }
  } else if (suoritus.koulutustyyppi === 'dia') {
    return <DiaTutkintoOppiaineet suoritus={suoritus} />;
  }
  return null;
}

const YoKokeetTable = ({ yoKokeet }: { yoKokeet: Array<YOKoe> }) => {
  const { t, translateKielistetty } = useTranslations();

  return (
    <StripedTable>
      <TableHead>
        <TableRow>
          <TableCell>{t('oppija.yo-kokeen-aine')}</TableCell>
          <TableCell>{t('oppija.arvosana')}</TableCell>
          <TableCell>{t('oppija.yo-kokeen-yhteispistemaara')}</TableCell>
          <TableCell>{t('oppija.yo-kokeen-tutkintokerta')}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {yoKokeet.map((row) => (
          <TableRow key={row.tunniste}>
            <TableCell>{translateKielistetty(row.nimi)}</TableCell>
            <TableCell>{row.arvosana}</TableCell>
            <TableCell>{row.yhteispistemaara?.toString()}</TableCell>
            <TableCell>{formatFinnishDate(row.tutkintokerta)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </StripedTable>
  );
};

export const LukioSuoritusPaper = ({
  suoritus,
}: {
  suoritus: LukioSuoritus;
}) => {
  return (
    <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.blue2}>
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      <LukionOppiaineet suoritus={suoritus} />
      {'yoKokeet' in suoritus && <YoKokeetTable yoKokeet={suoritus.yoKokeet} />}
    </SuoritusInfoPaper>
  );
};
