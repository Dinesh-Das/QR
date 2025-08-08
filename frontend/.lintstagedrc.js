module.exports = {
  '*.{js,jsx,ts,tsx}': [
    'eslint --fix',
    'prettier --write',
    'npm run test -- --bail --findRelatedTests --passWithNoTests',
  ],
  '*.{json,css,md}': [
    'prettier --write',
  ],
};