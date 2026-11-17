/**
 * Stylelint-Regeln für das Cosmos-Theme (src/main/resources/META-INF/resources/custom-theme).
 * Formatierung übernimmt Prettier; Stylelint prüft nur inhaltliche Regeln.
 * Befehle und Grenzen: docs/entwicklung.md, Abschnitt „CSS prüfen und formatieren“.
 */

/** Dateien, die eigene Farbwerte enthalten dürfen: Palette, Hintergrundszene, Google-Markenfarben. */
const colorSourceFiles = [
  '**/custom-theme/cosmos-palette.css',
  '**/custom-theme/cosmos-background-*.css',
  '**/custom-theme/cosmos-external-login.css',
];

export default {
  reportNeedlessDisables: true,
  reportInvalidScopeDisables: true,
  rules: {
    // Farben nur über die Palette: keine Hexwerte und keine rgb()/hsl()-Literale.
    'color-no-hex': true,
    'color-no-invalid-hex': true,
    'function-disallowed-list': ['rgb', 'rgba', 'hsl', 'hsla'],

    // Keine Lumo-Variablen mehr: weder definieren noch verwenden.
    // Das Muster bedeutet: Der Name nach „--“ darf nicht mit „lumo-“ beginnen.
    'custom-property-pattern': [
      '^(?!lumo-)',
      { message: (name) => `Lumo-Variable ${name} ist nicht erlaubt; Cosmos- oder Aura-Token verwenden.` },
    ],
    // Das Muster bedeutet: irgendwo im Wert steht „--lumo-“, etwa in var(--lumo-space-m).
    'declaration-property-value-disallowed-list': [
      { '/.*/': ['/--lumo-/'] },
      { message: 'Lumo-Variablen sind nicht erlaubt; Cosmos- oder Aura-Token verwenden.' },
    ],

    // Doppelte Regeln und Deklarationen vermeiden.
    'no-duplicate-selectors': true,
    'declaration-block-no-duplicate-properties': true,
    'declaration-block-no-duplicate-custom-properties': true,

    // Tippfehler und leere Blöcke.
    'property-no-unknown': true,
    'block-no-empty': true,
  },
  overrides: [
    {
      files: colorSourceFiles,
      rules: {
        'color-no-hex': null,
        'function-disallowed-list': null,
      },
    },
  ],
};
