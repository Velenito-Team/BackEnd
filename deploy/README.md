# Backend para la APK

`compose.server.yaml` despliega el backend y una entrada HTTPS para consultar comprobantes. El Compose de desarrollo se conserva para trabajar localmente.

## Preparar el servidor

Se necesita un servidor Linux con Docker y Compose, acceso a la base MongoDB actual y una IP pública o un dominio cuyo DNS apunte al servidor. El puerto 443 debe estar disponible para HTTPS. El Compose no ocupa el puerto 80: en OCI ya lo usa Nginx. Si otro servicio ocupa 443, intégralo antes de iniciar este Compose.

### Si solo tienes una IP pública

No es obligatorio comprar un dominio. En `.env.server`, configura:

```properties
COMPROBANTES_DOMAIN=TU_IP_PUBLICA
CADDYFILE=Caddyfile.ip
```

Esta variante solicita a Let's Encrypt un certificado público para la IP con el perfil `shortlived`, mediante TLS-ALPN en 443. Es distinto al certificado local autofirmado que Caddy usaría por defecto para una IP. La emisión requiere que la autoridad alcance ese puerto; tener una configuración válida no garantiza que el firewall o el proveedor permitan emitirlo. No se modifica el Nginx existente en 80 ni se publica una redirección HTTP.

Los certificados de IP duran seis días y Caddy debe permanecer activo con sus volúmenes para renovarlos. En la APK se configura `https://TU_IP_PUBLICA`. No se desactiva la comprobación de certificados en la app.

Referencia: [certificados públicos para IP de Let's Encrypt](https://letsencrypt.org/2026/01/15/6day-and-ip-general-availability).

No se reemplaza MongoDB ni se crea una base nueva. Se conserva el replica set y el operador existentes.

1. Transfiere `pom.xml`, `src/`, `Dockerfile`, `.dockerignore`, `compose.server.yaml` y `deploy/` al directorio elegido del servidor.
2. Crea `.env.server` a partir de `deploy/env.server.example`. Copia los valores actuales de `MONGODB_URI`, `VELENITO_POS_USER` y `VELENITO_RECEIPT_SECRET`. No cambies el secreto de comprobantes ni publiques este archivo.
3. Indica el dominio real en `COMPROBANTES_DOMAIN` y protege el archivo:

```sh
chmod 600 .env.server
docker compose --env-file .env.server -f compose.server.yaml config --quiet
docker compose --env-file .env.server -f compose.server.yaml up -d --build
docker compose --env-file .env.server -f compose.server.yaml ps
```

Usa solamente `-f compose.server.yaml`. Combinarlo con el Compose de desarrollo puede conservar una publicación abierta del puerto 8080.

## Qué queda publicado

- Público por HTTPS: solo `GET /api/invoices/receipt/<token válido en formato>`.
- Resto de rutas y métodos: error genérico 404.
- Backend completo: `127.0.0.1:8080` en el servidor, para administración privada o túnel SSH.
- El proxy conserva certificados en un volumen y elimina la URI de sus registros de ejecución. No habilites registros de acceso que incluyan tokens.

## Conectar el celular

1. Instala `app-movil/dist/velenito-comprobantes-debug.apk`.
2. En **Configurar servidor**, guarda `https://comprobantes.TU-DOMINIO` (sin `/api` ni rutas).
3. Escanea un QR real emitido por el POS o pega su enlace. La app toma su token y consulta el servidor configurado, aunque el QR anterior tenga `localhost`.
4. Verifica una venta vigente, una fiada sin datos personales ni costos, una anulada y un enlace inválido.

El enlace con `#comprobante/` sirve para extraer el token en la app. Este despliegue no publica el frontend web; abrirlo en un navegador no muestra la interfaz del POS. Para que los QR también funcionen fuera de la app habrá que desplegar la vista web pública.

La caja local puede seguir generando ventas si conecta a la misma base y conserva el mismo secreto. Para usar el backend remoto desde el POS sin publicar las rutas privadas, abre un túnel SSH:

```sh
ssh -N -L 18080:127.0.0.1:8080 USUARIO@SERVIDOR
```

Después apunta el proxy del frontend local a `http://host.docker.internal:18080` si usa Docker Desktop, o a `http://127.0.0.1:18080` si usa Vite directamente. No publiques la API completa antes de implementar autenticación y permisos de operador.

## Verificación y reversión

Con un token ficticio de 64 caracteres hexadecimales, la ruta pública debe responder con error genérico si el comprobante no existe. `/api/products`, `/api/credits` y los métodos de escritura deben devolver 404 en el dominio público. La consulta con un comprobante real verifica además la conexión a MongoDB y su vigencia.

Conserva la imagen anterior antes de actualizar. `BACKEND_TAG` permite seleccionar una versión de imagen. Una reversión de código no debe borrar los datos ni los volúmenes de MongoDB o certificados.

Documentación del proxy: [HTTPS automático](https://caddyserver.com/docs/automatic-https), [rutas](https://caddyserver.com/docs/caddyfile/matchers) y [reverse proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy).
