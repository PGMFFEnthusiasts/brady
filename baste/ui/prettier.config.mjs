import {
  prettierConfigBase,
  prettierConfigTailwind,
  merge,
} from '@hiddenability/opinionated-defaults/prettier';

export default merge(prettierConfigBase, prettierConfigTailwind);
