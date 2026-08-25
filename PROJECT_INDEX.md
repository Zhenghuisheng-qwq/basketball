# Basketball Project Index

This index reflects the current repository layout. Generated output and dependency directories are intentionally omitted.

## Main Projects

| Project | Type | Entry points |
| --- | --- | --- |
| `basketball-web-ui` | Public/member web frontend | `src/main.ts`, `src/App.vue`, `src/router/index.ts` |
| `basketball-admin-ui` | Admin frontend | `src/main.ts`, `src/App.vue`, `src/router/index.ts` |
| `basketball-admin` | Spring Boot backend | `src/main/java`, `pom.xml` |
| `generator` | Code generator | `src`, `pom.xml` |

## Frontend Index

### `basketball-web-ui`

- `src/api`: public, account, team, event and match APIs
- `src/assets`: images and global Sass/CSS
- `src/components`: shared account, event, team and public components
- `src/router`: route definitions
- `src/stores`: Pinia state modules
- `src/views`: page-level views
- `src/assets/styles/index.scss`: global style entry
- `src/assets/styles/variables.scss`: design variables
- `package.json`: Vite development and production build scripts

### `basketball-admin-ui`

- `src/api`: basket, CNPA, monitor, system and tool APIs
- `src/assets/styles`: admin global styles
- `src/components`: reusable RuoYi and business components
- `src/layout`: shell, sidebar, header and tags layout
- `src/router`: static and permission routes
- `src/store`: Pinia stores
- `src/views`: dashboard, CNPA, event, identity, system and monitor pages
- `src/main.ts`: application bootstrap
- `package.json`: Vite development and production build scripts

## Backend Index

### `basketball-admin`

- `src/main/java`: controllers, services, domain objects and infrastructure
- `src/main/resources`: Spring configuration, mapper XML, templates and static resources
- `src/test/java`: unit and integration tests
- `pom.xml`: Maven build and dependency definition
- `scripts`: smoke tests, seed data and verification utilities

## Supporting Directories

- `sql`: database schema and seed SQL
- `docs`: business rules, technical decisions and test reports
- `deploy`: deployment configuration and scripts
- `scripts`: repository-level maintenance scripts
- `image`: design or reference images
- `backup`: database and execution backups
- `logs`: local execution logs
- `ui`: shared or experimental UI materials
- `需求`, `核心业务`, `测试`, `文档`: Chinese project notes and acceptance materials

## Excluded Generated Directories

The following are build or dependency output and should not be edited directly:

- `node_modules`
- `dist`
- `target`
- `.git`

## Common Commands

```powershell
cd basketball-web-ui; npm run dev
cd basketball-web-ui; npm run build
cd basketball-admin-ui; npm run dev
cd basketball-admin-ui; npm run build:prod
cd basketball-admin; mvn spring-boot:run
```

