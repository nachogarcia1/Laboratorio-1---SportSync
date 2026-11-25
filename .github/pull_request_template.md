## Ticket

<!-- Id y título tal como figuran en BACKLOG.md. Ej.: T-03 Autorización por dueño (corregir IDOR) -->
T-XX:

## Qué cambia

<!-- Resumen breve de los cambios y el porqué. Un PR cubre un solo ticket. -->

## Cómo se probó

<!-- Comandos ejecutados, tests agregados, pruebas manuales. -->

## Criterios de aceptación

<!-- Copiá los criterios del ticket en BACKLOG.md y marcá los cumplidos. -->
- [ ] 
- [ ] 

## Checklist

- [ ] La rama sigue el formato `<tipo>/T-XX-slug` y está actualizada con `main`
- [ ] Los commits siguen Conventional Commits e incluyen `[T-XX]`
- [ ] No hay secretos ni archivos `.env` en el diff
- [ ] Si cambia el esquema de la base, hay un script en `db/migrations` con su rollback
- [ ] Si cambia la configuración, se actualizó `.env.example` y la documentación
- [ ] Las dependencias del ticket ya están mergeadas en `main`
