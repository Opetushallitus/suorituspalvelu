import { useTranslations } from '@/hooks/useTranslations';
import { Box, Stack } from '@mui/material';
import {
  OphButton,
  OphInputFormField,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { OphModal } from './OphModal';
import { Add, Remove } from '@mui/icons-material';
import { FullSpinner } from './FullSpinner';

type CommonEntries = {
  id: string;
  description: string;
  unsaved?: boolean;
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
    id: 'POHJAKOULUTUS',
    description: 'Pohjakoulutus',
    type: 'select',
    value: '1',
    options: [
      { value: '0', label: 'Ulkomainen tutkinto' },
      { value: '1', label: 'Peruskoulu' },
      { value: '2', label: 'Osittain yksilöllistetty' },
      { value: '3', label: 'Alueittain yksilöllistetty' },
      { value: '6', label: 'Yksilöllistetty' },
      { value: '7', label: 'Keskeytynyt' },
      { value: '9', label: 'Ylioppilas' },
    ],
  },
  {
    id: 'valintatuloksen-julkaisulupa',
    description: 'Valintatuloksen julkaisulupa',
    type: 'string',
    value: 'Kyllä',
  },
  {
    id: 'PK_FY',
    description: 'Peruskoulun fysiikan arvosana',
    type: 'string',
    value: '9',
  },
  {
    id: 'yks_mat_ai',
    description: 'Yksilöllistetty matematiikka',
    type: 'string',
    value: 'false',
  },
];

type EditableFieldsProps = {
  fields: Array<EditableFieldSpec>;
};

const INITIAL_FIELD_SPEC: EditableFieldSpec = {
  id: '',
  description: '',
  type: 'string',
  value: '',
  unsaved: true,
};

const AddFieldModal = ({
  isOpen,
  onClose: onCloseProp,
  onAdd,
}: {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (field: EditableFieldSpec) => void;
}) => {
  const { t } = useTranslations();
  const [field, setField] = useState<EditableFieldSpec>(
    () => INITIAL_FIELD_SPEC,
  );

  const onClose = () => {
    onCloseProp();
  };

  return (
    <OphModal
      title={t('muokkaus.lisaa-kentta-otsikko')}
      open={isOpen}
      onClose={onClose}
      actions={
        <>
          <OphButton variant="outlined" onClick={onClose}>
            {t('muokkaus.peruuta')}
          </OphButton>
          <OphButton
            variant="contained"
            onClick={() => {
              onAdd(field);
              setField(INITIAL_FIELD_SPEC);
            }}
          >
            {t('muokkaus.lisaa-kentta')}
          </OphButton>
        </>
      }
    >
      <Stack spacing={2}>
        <OphInputFormField
          label={t('muokkaus.lisaa-kentta-tunniste')}
          onChange={(event) =>
            setField((f) => ({ ...f, id: event.target.value }))
          }
          value={field.id}
        />
        <OphInputFormField
          label={t('muokkaus.lisaa-kentta-kuvaus')}
          multiline={true}
          maxRows={20}
          onChange={(event) =>
            setField((f) => ({ ...f, description: event.target.value }))
          }
          value={field.description}
        />
      </Stack>
    </OphModal>
  );
};

const EditableFields = ({ fields: fieldsProp }: EditableFieldsProps) => {
  const { t } = useTranslations();

  const { mutate, isPending } = useMutation({
    mutationFn: (data: Record<string, string>) => {
      console.log('Saving data');
      console.log({ data });
      return new Promise((resolve) => {
        setTimeout(resolve, 1000);
      });
    },
    onSuccess: () => {
      alert('Data saved successfully');
    },
    onError: () => {
      alert('Error saving data');
    },
  });

  const [editableFields, setEditableFields] =
    useState<Array<EditableFieldSpec>>(fieldsProp);
  const [lisaaModalIsOpen, setLisaaModalIsOpen] = useState<boolean>(false);

  const removeField = (id: string) => {
    setEditableFields((fields) => fields.filter((field) => field.id !== id));
  };

  return isPending ? (
    <FullSpinner />
  ) : (
    <Box
      component="form"
      onKeyDown={(event) => {
        if (event.key === 'Enter') {
          event.preventDefault(); // Don't trigger form submit
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
        padding: 2,
        gap: 2,
      }}
    >
      <AddFieldModal
        isOpen={lisaaModalIsOpen}
        onClose={() => setLisaaModalIsOpen(false)}
        onAdd={(field) => {
          const hasFieldId = editableFields.some((f) => f.id === field.id);
          if (hasFieldId) {
            alert(`Kenttä tunnisteella ${field.id} on jo olemassa!`);
            return;
          }
          setEditableFields([...editableFields, field]);
          setLisaaModalIsOpen(false);
        }}
      />
      <>
        {editableFields.map((field) => {
          return (
            <Stack
              key={field.id}
              direction="row"
              sx={{ gap: 2, justifyContent: 'stretch', alignItems: 'flex-end' }}
            >
              {field.type === 'select' ? (
                <OphSelectFormField
                  sx={{ minWidth: '250px', flex: 1 }}
                  label={field.id}
                  options={field.options}
                  helperText={field.description}
                  inputProps={{ name: field.id }}
                  defaultValue={field.value}
                />
              ) : (
                <OphInputFormField
                  sx={{ minWidth: '250px', flex: 1 }}
                  label={field.id}
                  helperText={field.description}
                  inputProps={{ name: field.id }}
                  defaultValue={field.value}
                />
              )}
              {field.unsaved && (
                <OphButton
                  startIcon={<Remove />}
                  variant="outlined"
                  onClick={() => removeField(field.id)}
                >
                  {t('muokkaus.poista-kentta')}
                </OphButton>
              )}
            </Stack>
          );
        })}
      </>
      <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
        <OphButton
          startIcon={<Add />}
          variant="outlined"
          onClick={() => setLisaaModalIsOpen(true)}
        >
          {t('muokkaus.lisaa-kentta')}
        </OphButton>
        <OphButton variant="contained" type="submit">
          {t('muokkaus.tallenna')}
        </OphButton>
      </Stack>
    </Box>
  );
};

export const MuokkausView = () => {
  return <EditableFields fields={EDITABLE_FIELDS} />;
};
