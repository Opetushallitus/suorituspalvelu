import { render } from '@testing-library/react';
import { screen, waitFor } from '@testing-library/dom';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SearchInput } from './SearchInput';
import { useState } from 'react';

vi.mock('@/hooks/useTranslations', () => ({
  useTranslations: () => ({
    t: (key: string) => key,
    translateKielistetty: (
      obj: { fi?: string; sv?: string; en?: string } | undefined,
    ) => obj?.fi || '',
    getLanguage: () => 'fi' as const,
  }),
}));

describe('SearchInput', () => {
  const defaultProps = {
    onClear: vi.fn(),
    onChange: vi.fn(),
    value: '',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders with initial value', () => {
    render(<SearchInput {...defaultProps} value="test value" />);
    expect(screen.getByRole('textbox')).toHaveValue('test value');
  });

  it('renders search icon', () => {
    render(<SearchInput {...defaultProps} />);
    expect(
      document.querySelector('.SearchInput_searchIcon'),
    ).toBeInTheDocument();
  });

  it('shows clear button when value is present', () => {
    render(<SearchInput {...defaultProps} value="test" />);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('hides clear button when value is empty', () => {
    render(<SearchInput {...defaultProps} />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('calls onClear when clear button is clicked', async () => {
    const user = userEvent.setup();
    render(<SearchInput {...defaultProps} value="test" />);

    await user.click(screen.getByRole('button', { name: 'search.tyhjenna' }));
    expect(defaultProps.onClear).toHaveBeenCalledTimes(1);
  });

  it('updates local value on input change', async () => {
    const user = userEvent.setup();
    render(<SearchInput {...defaultProps} />);

    const input = screen.getByRole('textbox');
    await user.type(input, 'new value');

    expect(input).toHaveValue('new value');
  });

  it('debounces onChange calls', async () => {
    const user = userEvent.setup();
    render(<SearchInput {...defaultProps} debounceMs={50} />);

    const input = screen.getByRole('textbox');

    // Wait for initial render's onChange to fire
    await new Promise((resolve) => setTimeout(resolve, 60));
    defaultProps.onChange.mockClear();

    await user.type(input, 'abc');

    // Check immediately - should not have been called yet due to debounce
    expect(defaultProps.onChange).not.toHaveBeenCalled();

    // Wait for debounce to finish
    await waitFor(
      () => {
        expect(defaultProps.onChange).toHaveBeenCalledWith('abc');
      },
      { timeout: 100 },
    );
  });

  it('uses custom debounce time', async () => {
    const user = userEvent.setup();
    render(<SearchInput {...defaultProps} debounceMs={100} />);

    const input = screen.getByRole('textbox');
    await user.type(input, 'test');

    expect(defaultProps.onChange).not.toHaveBeenCalled();

    await waitFor(
      () => {
        expect(defaultProps.onChange).toHaveBeenCalledWith('test');
      },
      { timeout: 150 },
    );
  });

  it('syncs external value changes when not debouncing', async () => {
    const { rerender } = render(
      <SearchInput {...defaultProps} value="initial" debounceMs={10} />,
    );

    // Let the initial debounce timeout complete
    await new Promise((resolve) => setTimeout(resolve, 20));

    rerender(<SearchInput {...defaultProps} value="updated" debounceMs={10} />);

    expect(screen.getByRole('textbox')).toHaveValue('updated');
  });

  it('does not sync external value during debouncing', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <SearchInput {...defaultProps} value="" debounceMs={100} />,
    );

    await user.type(screen.getByRole('textbox'), 'typing');

    rerender(
      <SearchInput {...defaultProps} value="external" debounceMs={100} />,
    );

    expect(screen.getByRole('textbox')).toHaveValue('typing');
  });

  it('passes additional props to OphInputFormField', () => {
    render(
      <SearchInput {...defaultProps} placeholder="Search here" disabled />,
    );

    const input = screen.getByRole('textbox');
    expect(input).toHaveAttribute('placeholder', 'Search here');
    expect(input).toBeDisabled();
  });

  it('does not cause infinite rerender loop with controlled state', async () => {
    const renderSpy = vi.fn();
    let renderCount = 0;

    const ControlledSearchInput = () => {
      const [searchValue, setSearchValue] = useState('');

      renderCount++;
      renderSpy();

      const handleChange = (value: string) => {
        setSearchValue(value);
      };

      const handleClear = () => {
        setSearchValue('');
      };

      return (
        <div>
          <SearchInput
            value={searchValue}
            onChange={handleChange}
            onClear={handleClear}
            debounceMs={50}
          />
          <div data-testid="search-value">{searchValue}</div>
        </div>
      );
    };

    const user = userEvent.setup();
    render(<ControlledSearchInput />);

    // Wait for initial render and debounce to settle
    await new Promise((resolve) => setTimeout(resolve, 60));

    renderCount = 0;
    renderSpy.mockClear();

    // Type some text
    const input = screen.getByRole('textbox');
    await user.type(input, 'test');

    // Wait for debounce to trigger onChange
    await waitFor(
      () => {
        expect(screen.getByTestId('search-value')).toHaveTextContent('test');
      },
      { timeout: 150 },
    );

    // After onChange updates the parent state, there should be exactly 1 rerender
    // If there's an infinite loop, renderCount would be very high (hundreds/thousands)
    expect(renderCount).toBe(1);
    expect(renderCount).toBeLessThan(20);

    // Wait for any pending debounce to complete before clearing
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Clear the input
    renderCount = 0;
    const clearButton = screen.getByRole('button', { name: 'search.tyhjenna' });
    await user.click(clearButton);

    // Wait for the clear to complete
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Should trigger 1-2 rerenders (onClear + potential debounce)
    // If there's an infinite loop, this would be much higher (hundreds/thousands)
    expect(renderCount).toBeGreaterThanOrEqual(1);
    expect(renderCount).toBeLessThanOrEqual(2);
    expect(screen.getByTestId('search-value')).toBeEmptyDOMElement();
    expect(input).toHaveValue('');

    // Verify we never had an infinite loop during the entire test
    // If there was a loop, the total render count would be much higher (hundreds/thousands)
    const totalRenders = renderSpy.mock.calls.length;
    expect(totalRenders).toBeLessThan(10);
  });
});
