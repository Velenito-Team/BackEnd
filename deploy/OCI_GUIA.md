# Despliegue de El Veleñito en OCI

Fecha: 26 de noviembre de 2026. Versión desplegada: `20261126-173905`.

## Qué se hizo

Se accedió por SSH como `ubuntu` a `TU_IP_PUBLICA`, puerto **22**. SSH se usa para administrar el servidor; el celular consultará comprobantes por **HTTPS, puerto 443**. Son servicios distintos y no necesitan compartir puerto.

El servidor ya tenía Docker, MongoDB y Nginx. Se instaló el complemento oficial Docker Compose v5.8.0 para Linux ARM64, comprobando su SHA-256 contra el archivo oficial de sumas. No se reemplazaron MongoDB ni Nginx.

Se copiaron las fuentes y la configuración a:

```text
/home/ubuntu/velenito-backend
```

Docker compiló el backend con Maven y Java 21 directamente en el servidor ARM64. La imagen resultante es `el-velenito-backend:20261126-173905`. Compose creó dos contenedores:

| Servicio | Función | Puerto publicado |
|---|---|---|
| backend | API Java y conexión con MongoDB | 127.0.0.1:8080, solo dentro del servidor |
| comprobantes | Caddy, HTTPS y filtro de rutas públicas | 443 TCP |

Nginx continúa en el puerto 80. MongoDB continúa como servicio existente en el 27017 y con su replica set. Este despliegue no modificó su configuración ni creó ventas de prueba.

La ruta de una consulta será:

```text
App Android → HTTPS :443 → Caddy → backend :8080 → MongoDB existente :27017
```

## Configuración y secretos

El archivo `.env.server` del servidor tiene permisos `600`: solo su propietario puede leerlo y escribirlo. Contiene la conexión MongoDB, el operador y el secreto de comprobantes ya existentes. Se conservó el secreto para que los enlaces actuales sigan siendo reconocidos. No se incluyen sus valores en esta guía ni en Git.

En la conexión MongoDB, el host se cambió a `host.docker.internal`. Compose lo resuelve a la puerta de enlace del host Linux: el contenedor accede al MongoDB de su propio servidor sin salir hacia la IP pública. Se conservaron la base de autenticación, `replicaSet=rs0` y `directConnection=true`.

Las variables de publicación son:

```properties
COMPROBANTES_DOMAIN=TU_IP_PUBLICA
CADDYFILE=Caddyfile.ip
BACKEND_TAG=20261126-173905
```

## Estado verificado

- Ambos contenedores iniciaron correctamente.
- La API interna `GET /api/products` respondió **200** y consultó la base existente.
- Un comprobante vigente existente respondió **200**, sin iniciar sesión.
- Sus campos públicos y los de sus líneas coincidieron con la lista permitida; incluyó productos, cantidades, total y forma de pago.
- La respuesta del comprobante incluyó `Cache-Control: no-store`.
- Un token desconocido y otro con formato inválido devolvieron **404** con `Comprobante no disponible.`.
- Caddy alcanzó el backend por la red interna de Docker.
- Docker está habilitado para iniciar con el servidor. Los dos servicios usan `restart: unless-stopped`.

**Verificación pública completada:** después de habilitar la entrada 443, Let's Encrypt emitió el certificado para la IP. Se añadió `default_sni {$COMPROBANTES_DOMAIN}` en Caddy para los clientes que conectan por IP sin SNI, ya que Docker presenta una dirección interna como destino. La configuración se validó y recargó correctamente.

Desde el computador, por Internet y con la validación del certificado activada, un comprobante real vigente respondió **200**. Se verificaron productos, cantidades, total, forma de pago, ausencia de campos privados y `Cache-Control: no-store`. Los tokens desconocidos o inválidos respondieron **404** genérico. Las rutas públicas `/api/products`, `/api/credits` y `POST /api/invoices` respondieron **404** genérico; no se creó ninguna venta. El escaneo con cámara en un teléfono físico aún debe comprobarse desde la APK.

## Regla de entrada necesaria en OCI

En la consola de OCI, ubica la instancia con IP pública `TU_IP_PUBLICA`, su VNIC y su subred. Revisa las listas de seguridad de la subred y los grupos de seguridad de red (NSG) asociados. La entrada HTTPS necesita una regla **con estado** con:

| Campo | Valor |
|---|---|
| Tipo de origen | CIDR |
| CIDR de origen | 0.0.0.0/0 |
| Protocolo IP | TCP |
| Puerto de origen | Todos |
| Puerto de destino | 443 |
| Descripción | Comprobantes HTTPS El Veleñito |

No hace falta abrir el puerto 8080 para la app. Conserva la regla SSH existente en 22. Después de abrir 443, Caddy reintentará la emisión automáticamente. Si deseas provocar un nuevo intento, puedes reiniciar únicamente el proxy:

```sh
cd /home/ubuntu/velenito-backend
sudo docker compose -p velenito --env-file .env.server -f compose.server.yaml restart comprobantes
```

Los certificados para IP se solicitan a Let's Encrypt con perfil `shortlived` y desafío TLS-ALPN en 443. Caddy conserva y renueva los certificados en sus volúmenes. No borres esos volúmenes ni aceptes un certificado inválido como solución.

## Comandos de uso diario

Desde Termius, abre la conexión OCI con el usuario `ubuntu` y ejecuta:

```sh
cd /home/ubuntu/velenito-backend
sudo docker compose -p velenito --env-file .env.server -f compose.server.yaml ps
```

Para revisar el arranque del backend:

```sh
sudo docker compose -p velenito --env-file .env.server -f compose.server.yaml logs --tail=50 backend
```

Para revisar la emisión del certificado:

```sh
sudo docker compose -p velenito --env-file .env.server -f compose.server.yaml logs --tail=50 comprobantes
```

Para actualizar después de subir fuentes nuevas:

```sh
sudo docker compose -p velenito --env-file .env.server -f compose.server.yaml config --quiet
sudo docker compose -p velenito --env-file .env.server -f compose.server.yaml up -d --build
```

`config --quiet` valida sin imprimir los secretos. Usa solo `compose.server.yaml`: el Compose de desarrollo tiene una publicación de puertos distinta. No ejecutes `down -v` para una actualización.

## Conectar la APK

Instala `app-movil/dist/velenito-comprobantes-debug.apk`. En **Configurar servidor**, guarda:

```text
https://TU_IP_PUBLICA
```

No agregues `/api` ni el puerto SSH. Escanea un QR emitido por el POS o pega su enlace. La app extrae su token y consulta el servidor configurado incluso si el QR anterior contiene `localhost`.

Solo se publica `GET /api/invoices/receipt/<token hexadecimal de 64 caracteres>`. Las rutas privadas de productos, ventas, clientes y escritura tienen un error genérico 404 en el proxy público. El despliegue sirve la API de comprobantes para la app; no contiene el frontend web del POS.

La caja local puede seguir generando ventas en la misma base si conserva el mismo secreto. No necesita cambiar su backend para que la app lea los comprobantes.

## Archivos del proyecto

- `compose.server.yaml`: servicios de producción, puertos, memoria, reinicio y registros limitados.
- `deploy/Caddyfile.ip`: HTTPS para la IP y filtro de rutas.
- `deploy/Caddyfile`: alternativa para un dominio.
- `deploy/env.server.example`: plantilla sin secretos.
- `.dockerignore`: evita enviar `.env`, Git y archivos de compilación al constructor de imágenes.

Referencias: [instalación oficial de Compose](https://docs.docker.com/compose/install/linux/), [certificados públicos para IP](https://letsencrypt.org/2026/01/15/6day-and-ip-general-availability), [HTTPS automático de Caddy](https://caddyserver.com/docs/automatic-https).
