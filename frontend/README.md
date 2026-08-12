# AutoCare Frontend

React 19 SPA built with Vite. Consumes the Spring Boot API under `/api/v1` and
renders the vehicle history, dashboard and per-vehicle analytics. The interface
is in Brazilian Portuguese.

See the repository root `README.md` and `docs/ARCHITECTURE.md` for the full
picture.

## Commands

```bash
npm install       # install dependencies
npm run dev       # dev server on :5173, proxying /api to localhost:8080
npm test          # vitest run
npm run test:watch
npm run lint      # eslint
npm run build     # production bundle into dist/
npm run preview   # serve the built bundle
```

The backend must be running on `localhost:8080` for `npm run dev` to be useful.
The Vite proxy (see `vite.config.js`) keeps everything on one origin so the
HttpOnly refresh cookie stays first-party — nginx does the same in production.

## Layout

```
src/
├── api/          One module per resource; all share the axios client
├── auth/         AuthContext: user, login, register, logout
├── components/   Presentational components + Recharts wrappers
├── hooks/        useAsyncData — load, error and reload for a page
├── pages/        One component per route (see App.jsx)
├── utils/        pt-BR formatters, client-side validation mirrors
└── styles/       global.css — the whole design system
```

## Conventions

- **No state-management library.** Server data is loaded per page through
  `useAsyncData`; the authenticated user is the only shared client state.
- **The access token never touches storage.** It lives in a module variable in
  `api/client.js`; sessions survive reloads through the refresh cookie instead.
  A single shared refresh promise prevents a stampede when several requests hit
  `401` at once.
- **Validation in `utils/validation.js` mirrors the backend but is never
  authoritative.** It exists for instant feedback; the server re-checks
  everything.
- **Styling is one global stylesheet** driven by CSS custom properties. Prefer an
  existing class over a new one; the design system already covers cards, tables,
  forms, badges, tabs, dialogs and the app shell.
- **Labels and units come from `utils/labels.js` and `utils/format.js`,** so enum
  values and currency/units are never formatted ad hoc in a page.
