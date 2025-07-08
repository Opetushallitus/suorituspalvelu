import path from 'path';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { type Plugin } from 'vite';

const escapeRegex = (str: string) =>
  str.replace(/[/\-\\^$*+?.()|[\]{}]/g, '\\$&');

/**
 * Transforms the named imports to direct imports for the given packages
 */
const optimizePackageImports = (pkgNames: Array<string>) => {
  const importRegex = new RegExp(
    `^import\\s+{\\s*([^}]+)\\s*}\\s+from\\s+['"](${pkgNames.map((p) => escapeRegex(p)).join('|')})['"]`,
    'm', // <- only match first occurrence
  );

  return {
    name: 'optimize-package-imports',
    transform(code, id) {
      if (id.endsWith('.tsx') || id.endsWith('.ts')) {
        return code.replace(
          importRegex,
          (_match, imports: string, matchedPackageName: string) => {
            return imports
              .split(',')
              .map((importName) => {
                const [subName, asName = subName] = importName
                  .split(' as ')
                  .map((s) => s.trim());
                return `import ${asName} from '${matchedPackageName}/${subName}'`;
              })
              .join('\n');
          },
        );
      }
      return code;
    },
  } as Plugin;
};

export default defineConfig({
  plugins: [react(), optimizePackageImports(['@mui/icons-material'])],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    dir: './src',
    include: ['**/**.test.?(c|m)[jt]s?(x)'],
    coverage: {
      include: ['src/**'],
    },
    exclude: ['./cdk'],
    setupFiles: ['./vitest-setup.mts'],
    server: {
      deps: {
        inline: ['@opetushallitus/oph-design-system'],
      },
    },
  },
});
