# Guía de contribución

Este proyecto sigue **GitHub Flow**. La regla central: **un ticket = una rama = un PR**. Los tickets están definidos en [`BACKLOG.md`](BACKLOG.md) (`T-00`, `T-01`, ...), y el id del ticket aparece en la rama, en los commits y en el PR para tener trazabilidad.

## 1. Reglas generales

- `main` siempre está estable. **No se commitea ni se pushea directo a `main`.**
- Se trabaja un ticket a la vez. Un ticket con dependencias solo empieza cuando sus dependencias ya están mergeadas en `main`.
- No se apilan ramas sobre ramas sin mergear.

## 2. Ramas

Formato: `<tipo>/T-XX-slug-corto`

| Tipo | Uso |
|---|---|
| `feat` | funcionalidad nueva |
| `fix` | corrección de errores (incluye seguridad) |
| `refactor` | cambio interno sin alterar comportamiento |
| `test` | solo tests |
| `docs` | solo documentación |
| `chore` | tareas de mantenimiento y proceso |
| `ci` | pipelines y automatización |

Ejemplos: `chore/T-00-workflow`, `fix/T-03-idor-reservas`, `feat/T-10-recordatorios`.

Para arrancar un ticket:

```bash
git switch main
git pull --ff-only
git switch -c <tipo>/T-XX-slug
```

## 3. Commits

Se usa [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/) con el id del ticket al final del asunto:

```
<tipo>(<ámbito opcional>): <descripción en imperativo> [T-XX]
```

Ejemplos:

```
fix(reservas): validar dueño en cancelar [T-03]
feat(chat): guardar mensajes en MongoDB [T-08]
docs: agregar guía de contribución [T-00]
```

Reglas:
- Commits pequeños y atómicos: cada uno hace una sola cosa y deja el proyecto compilando.
- Asunto de hasta 72 caracteres. Si hace falta explicar el porqué, va en el cuerpo, separado por una línea en blanco.
- Nunca se commitean secretos ni archivos `.env`. Solo `.env.example`.

## 4. Push

- Se pushea la rama apenas tiene su primer commit, para dejar trazabilidad: `git push -u origin <rama>`.
- **Prohibido `--force` sobre `main`.** En ramas propias, si hay que reescribir historia, solo `--force-with-lease`.
- Antes de pedir revisión, la rama se actualiza con `main` (`git fetch origin && git rebase origin/main` o merge de `main`) y se resuelven los conflictos.

## 5. Pull Request

- Se abre contra `main`, con el título `<tipo>: <descripción> [T-XX]`. Puede abrirse como borrador mientras el trabajo está en curso.
- Se completa la plantilla (`.github/pull_request_template.md`): ticket, qué cambia, cómo se probó y checklist de criterios de aceptación copiados del ticket.
- Un PR cubre **un solo ticket**. Si surge trabajo fuera de alcance, se anota en `BACKLOG.md` como ticket nuevo.

## 6. Merge

- Se mergea solo con el PR revisado y, cuando exista el pipeline (T-16), con los checks en verde.
- Estrategia: **squash and merge**, con el mensaje final en formato Conventional Commits con el id del ticket.
- Se borra la rama después del merge.
- Después de mergear: `git switch main && git pull --ff-only`, y recién ahí se empieza el siguiente ticket.

## 7. Protección de `main` (configuración en GitHub)

Estas reglas las configura el dueño del repositorio en **Settings → Rules → Rulesets** (o **Branches → Branch protection rules**) sobre `main`:

- [ ] Requerir un pull request antes de mergear (con al menos 1 aprobación, si hay otro colaborador que revise).
- [ ] Bloquear los pushes directos y los force-push.
- [ ] Requerir que la rama esté actualizada antes de mergear.
- [ ] Permitir solo **squash merge** (Settings → General → Pull Requests).
- [ ] Borrar automáticamente las ramas mergeadas (Settings → General → Pull Requests).
- [ ] Cuando exista el pipeline (T-16): requerir que los checks de backend y frontend pasen.

## 8. Checklist rápida

1. `git switch main && git pull --ff-only`
2. `git switch -c <tipo>/T-XX-slug`
3. Commits atómicos con `[T-XX]`
4. `git push -u origin <rama>`
5. PR contra `main` con la plantilla completa
6. Revisión → squash merge → borrar rama
7. `git switch main && git pull --ff-only`
