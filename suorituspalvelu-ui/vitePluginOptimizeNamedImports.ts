import { type Plugin } from 'vite';

const escapeRegex = (str: string) =>
  str.replace(/[/\-\\^$*+?.()|[\]{}]/g, '\\$&');

/**
 * Transforms the named imports to direct imports for the given packages
 */
export const vitePluginOptimizeNamedImports = (pkgNames: Array<string>) => {
  const importRegex = new RegExp(
    `^import\\s+{\\s*([^}]+)\\s*}\\s+from\\s+['"](${pkgNames.map((p) => escapeRegex(p)).join('|')})['"]`,
    'm', // <- only match first line occurrence
  );

  return {
    name: 'optimize-named-imports',
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
