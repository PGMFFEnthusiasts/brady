import {
  eslintConfigBase,
  eslintConfigTypescript,
  eslintConfigRelative,
  eslintConfigPrettier,
} from '@hiddenability/opinionated-defaults/eslint';

export default [
  ...eslintConfigBase,
  ...eslintConfigTypescript,
  ...eslintConfigRelative,
  ...eslintConfigPrettier,
];
