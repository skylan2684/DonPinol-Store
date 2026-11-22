# Universo Pinol Store

Tienda de apps de Pinol, estilo Play Store: muestra sus apps, las instala y avisa e instala las actualizaciones con un toque.

**Descargar la tienda (versión con firma DonPinol):** https://raw.githubusercontent.com/skylan2684/DonPinol-Store/main/apks/UniversoPinolStore-1.0.8.apk
Si tenías la tienda anterior, desinstálala primero (la firma cambió y Android no deja actualizar encima). Se instala una sola vez; después se actualiza desde ella misma.

## Cómo funciona
- `catalog.json`: la lista de apps (nombre, descripción, novedades, versión, enlace de la APK, SHA-256 y tamaño). La tienda lo lee de este repositorio.
- `icons/`: íconos del catálogo.
- `app/`: el código de la tienda (Android, Kotlin + Compose).
- Las APK públicas de cada app van en la carpeta `apks/` de este repositorio. El código de las apps sigue privado en sus propios repositorios.
- La tienda revisa el catálogo cada pocas horas y avisa con una notificación si hay actualización. Al instalar comprueba el SHA-256 y que el paquete sea el esperado.

## Publicar una nueva versión de una app
```
python3 tools/publicar.py skylan2684/LecoLite v1.0.9 lecolite --cambios "• Lo nuevo"
git add -A && git commit -m "Catálogo: Leco Lite 1.0.9" && git push
```

## Firma
La tienda se firma con una llave que vive en los secretos del repositorio (`STORE_KEYSTORE_B64`, `STORE_KEYSTORE_PASSWORD`, `STORE_KEY_ALIAS`, `STORE_KEY_PASSWORD`), nunca en el código. Todas las versiones usan la misma llave para que se instalen una encima de otra.

## Aviso de Play Protect
Como las apps no vienen de Google Play, Android puede mostrar «Play Protect no vio una app de este desarrollador». Se instala con **Más detalles → Instalar de todas formas**. Pasa normalmente solo la primera vez.
