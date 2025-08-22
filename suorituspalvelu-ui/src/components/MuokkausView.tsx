import { useTranslations } from '@/hooks/useTranslations';
import { Box, Stack } from '@mui/material';
import {
  OphButton,
  OphInputFormField,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { useMutation } from '@tanstack/react-query';

type CommonEntries = {
  id: string;
  label: string;
  description: string;
};

type EditableFieldSpec =
  | (CommonEntries & {
      type: 'number' | 'string';
      value: string;
    })
  | (CommonEntries & {
      type: 'select';
      value: string;
      options: Array<{ label: string; value: string }>;
    });

const EDITABLE_FIELDS: Array<EditableFieldSpec> = [
  {
    id: 'hissa',
    label: 'Hissa',
    description: 'Historian arvosana',
    type: 'number',
    value: '10',
  },
  {
    id: 'sanallinen',
    label: 'Sanallinen arviointi',
    description: 'Sanallinen arviointi',
    type: 'string',
    value: 'Esimerkki syy',
  },
  {
    id: 'kieli',
    label: 'Kieli',
    description: 'Opetettava kieli',
    type: 'select',
    options: [
      { label: 'Suomi', value: 'fi' },
      { label: 'Ruotsi', value: 'sv' },
      { label: 'Englanti', value: 'en' },
    ],
    value: 'fi',
  },
];

type EditableFieldsProps = {
  fields: Array<EditableFieldSpec>;
};

const EditableFields = ({ fields }: EditableFieldsProps) => {
  const { t } = useTranslations();

  const { mutate } = useMutation({
    mutationFn: async (data: Record<string, string>) => {
      console.log('saving data');
      console.log({ data });
    },
    onSuccess: () => {
      alert('Data saved successfully');
    },
    onError: () => {
      alert('Error saving data');
    },
  });
  return (
    <Box
      component="form"
      onKeyDown={(event) => {
        if (event.key === 'Enter') {
          event.preventDefault();
        }
      }}
      onSubmit={(event) => {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        const data: Record<string, string> = {};
        formData.forEach((value, key) => {
          data[key] = value.toString();
        });
        mutate(data);
      }}
      sx={{
        display: 'flex',
        flexDirection: 'column',
        flexWrap: 'wrap',
        padding: 2,
        gap: 2,
      }}
    >
      <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 2 }}>
        {fields.map((field) =>
          field.type === 'select' ? (
            <OphSelectFormField
              sx={{ minWidth: '250px' }}
              label={field.label}
              options={field.options}
              key={field.id}
              inputProps={{ name: field.id }}
              defaultValue={field.value}
            />
          ) : (
            <OphInputFormField
              sx={{ minWidth: '250px' }}
              label={field.label}
              key={field.id}
              inputProps={{ name: field.id }}
              defaultValue={field.value}
            />
          ),
        )}
      </Stack>
      <OphButton
        variant="contained"
        type="submit"
        sx={{ alignSelf: 'flex-end' }}
      >
        {t('tallenna')}
      </OphButton>
    </Box>
  );
};

export const MuokkausView = () => {
  return <EditableFields fields={EDITABLE_FIELDS} />;
};
