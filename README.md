# ObraTrack

Aplicación Android nativa para gestionar obras y sus relevamientos de campo: registrá cada visita con nota, foto y ubicación, planificá tareas con un cronograma (Gantt) y llevá una agenda de próximas visitas.

## Funcionalidades

- **Obras:** alta, edición y estado (activa, pausada, finalizada). Pueden ser privadas o públicas; las públicas las ven y las siguen otros usuarios.
- **Relevamientos:** nota, fecha, foto, ubicación en mapa y vínculo opcional con una tarea. Propuesta de la próxima visita al crearlos.
- **Tareas y cronograma:** estados, categorías, fechas de inicio y fin, detección de tareas atrasadas y vista Gantt. Son privadas del dueño de la obra.
- **Agenda de visitas:** próxima visita con fecha y hora, avisos con anticipación configurable y pronóstico del tiempo.
- **Informes en PDF** de un relevamiento o de toda la obra.
- **Uso sin conexión:** los cambios se guardan en el teléfono y se sincronizan al volver la red; las fotos se suben solas cuando hay conexión.
- **Idiomas:** español, inglés e italiano. Tema claro y oscuro.

## Tecnologías

Kotlin, Jetpack Compose (Material 3), MVVM, Hilt, Navigation 3, Coroutines y Flow, WorkManager, Firebase Authentication (correo y Google), Cloud Firestore, Cloudinary (imágenes), osmdroid (mapas) y Coil.

`minSdk` 26, `targetSdk` 37.

## Estructura

```
app/                Punto de entrada, navegación, Hilt y Workers
core/domain/        Modelos, contratos de repositorio y casos de uso (Kotlin puro)
core/data/          Implementaciones de los repositorios y mapeos
core/firebase/      Acceso a Auth y Firestore, DTOs
core/ui/            Componentes y textos compartidos
features/auth/      Inicio de sesión, registro y perfil
features/home/      Inicio y agenda de visitas
features/obras/     Obras, relevamientos, tareas y cronograma
```

Las features dependen del dominio, nunca de Firebase ni de las implementaciones de los repositorios.

## Configuración

1. **Requisitos:** Android Studio con JDK 11 o superior y Android SDK 37.
2. **Firebase:** creá un proyecto, activá Authentication (correo y Google) y Cloud Firestore, y copiá su `google-services.json` en `app/`. Ese archivo no se versiona.
3. **Reglas de Firestore:** `firestore.rules` y `firestore.indexes.json` están en la raíz; publicalos con Firebase CLI (`firebase deploy --only firestore`).
4. **Cloudinary:** el nombre de la nube y el preset de subida sin firma se definen en `app/build.gradle.kts` (`CLOUDINARY_CLOUD_NAME` y `CLOUDINARY_UPLOAD_PRESET`).
5. Creá `local.properties` con la ruta del SDK (Android Studio lo genera solo).

## Compilar y probar

```bash
./gradlew assembleDebug        # compilar
./gradlew test                 # tests unitarios
./gradlew lintDebug            # lint
```

En Windows usá `.\gradlew.bat` en lugar de `./gradlew`. Para probar un solo módulo: `./gradlew :core:domain:test`.

Los tests instrumentados necesitan un emulador o dispositivo: `./gradlew connectedDebugAndroidTest`.
