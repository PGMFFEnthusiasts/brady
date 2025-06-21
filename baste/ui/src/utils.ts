export const currentPath = () =>
  globalThis.location.pathname.split('/').findLast(Boolean);
