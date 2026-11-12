# Releva · Relevamiento de obras

**App de Android (APK):** cada vez que se sube un cambio, GitHub arma solo el archivo `Releva.apk` y lo deja en **Releases → ultima** (columna derecha de la página del repositorio). Desde el celular: abrí ese link, descargá `Releva.apk` y abrilo para instalar. Para actualizar, se instala el nuevo encima: los relevamientos no se pierden.

App web para relevar obras desde el celular, la tablet o la compu: muros y ambientes, aberturas, instalaciones eléctricas, sanitarias y de gas, estructura, escaleras, niveles, plantas y asoleamiento.

Es una sola página (`index.html`) que funciona sin servidor. Una vez abierta, se puede **instalar** como app y **funciona sin internet**.

## Archivos

| Archivo | Para qué sirve |
|---|---|
| `index.html` | La app completa |
| `manifest.webmanifest` | Nombre, colores e íconos para instalarla |
| `sw.js` | Guarda la app en el dispositivo para usarla sin conexión |
| `icons/` | Íconos de la app |
| `android/` | El proyecto de la app de Android (no hace falta tocarlo) |
| `.github/workflows/build.yml` | Arma el APK solo en GitHub cada vez que subís cambios |
| `.nojekyll` | Le indica a GitHub Pages que publique los archivos tal cual |

## Publicarla en GitHub Pages (sin programar)

1. Entrá a github.com y creá un repositorio nuevo, por ejemplo `releva` (puede ser público).
2. Tocá **Add file → Upload files** y arrastrá **todo el contenido** de esta carpeta (incluida la carpeta `icons`). Tocá **Commit changes**.
3. Andá a **Settings → Pages**. En *Build and deployment* elegí **Deploy from a branch**, rama **main** y carpeta **/ (root)**. Guardá.
4. En uno o dos minutos aparece el link, del estilo `https://TU-USUARIO.github.io/releva/`.

## Instalarla en el celular

- **Android (Chrome):** abrí el link → menú ⋮ → **Instalar app** (o *Agregar a la pantalla principal*). También aparece en el menú ⋯ de Releva como *Instalar en este dispositivo*.
- **iPhone / iPad (Safari):** abrí el link → botón **Compartir** → **Agregar a inicio**.

Después de abrirla una vez con internet, funciona sin conexión en la obra.

## Tus datos

- Los relevamientos se guardan **solos en el dispositivo** (menú ⋯ → *Mis relevamientos*). No se suben a ningún lado.
- Si se borran los datos del navegador o cambiás de celular, se pierden. Por eso, al terminar cada relevamiento: menú ⋯ → **Guardar archivo / compartir**. Genera un archivo `.releva` que podés mandarte por WhatsApp o mail.
- Para abrirlo en otro dispositivo: menú ⋯ → **Abrir archivo**.

## Subir una versión nueva

1. Reemplazá `index.html` en el repositorio (Add file → Upload files).
2. En `sw.js` cambiá la línea `const VERSION = 'releva-1.0';` por un número nuevo (por ejemplo `releva-1.1`).
3. La próxima vez que abran la app con internet, se actualiza sola.
