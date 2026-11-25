# VoxelCore 0.2.0 — Minecraft Java 1.12.2

VoxelCore es un mod Forge 1.12.2 que combina herramientas de diagnóstico, optimizaciones cliente conservadoras y contenido de juego propio. Está pensado como una plataforma ampliable: el sistema de rendimiento puede desactivarse sin desactivar los bloques, objetos, herramientas o armaduras.

> **Estado de esta entrega:** fuente y recursos de la versión 0.2.0. No se incluye un JAR compilado porque el entorno de creación no tiene JDK 8, Gradle ni acceso de red al Maven de Forge. No se debe instalar el ZIP como si fuera un mod: primero hay que compilarlo.

## Novedades de 0.2.0

### Rendimiento y diagnóstico

- HUD de rendimiento con FPS, tiempo medio suavizado por fotograma y percentil 95 de las muestras recientes.
- Uso de memoria Java actual y máxima.
- Recuento de entidades, tile entities y chunks cliente, cada uno configurable.
- El contador de chunks identifica también la dimensión para no mezclar coordenadas iguales de dimensiones distintas.
- Culling visual opcional para criaturas lejanas no jugadoras. Desactivado de forma predeterminada; no elimina entidades del mundo ni detiene su IA.
- Límite opcional de partículas mediante el ajuste nativo de Minecraft. Se restaura el ajuste anterior al desactivarlo.
- Opciones persistentes en `config/voxelcore.cfg` y pantalla de configuración dentro del juego.
- Teclas independientes para abrir ajustes y alternar las dos optimizaciones opcionales.

### Contenido jugable

- Mena de resonita que aparece en chunks nuevos del Overworld.
- Cristal y lingote de resonita; el cristal se funde en lingotes.
- Bloque de resonita y lámpara luminosa.
- Juego completo de herramientas: pico, espada, pala, hacha y azada.
- Armadura completa: casco, pechera, grebas y botas, con texturas propias.
- Recetas de fabricación, pestaña creativa, blockstates, modelos, texturas y nombres en español e inglés.

### Controles

| Tecla | Acción |
|---|---|
| `O` | Abrir configuración de VoxelCore cuando no hay una pantalla abierta. |
| `F8` | Activar/desactivar el HUD de rendimiento. |
| `F7` | Activar/desactivar el recorte visual de criaturas lejanas. |
| `F6` | Activar/desactivar el límite de partículas. |

Las teclas se pueden reasignar desde `Opciones → Controles → VoxelCore`.

## Instalación (una vez compilado)

1. Instala Minecraft Java 1.12.2.
2. Instala Minecraft Forge 1.12.2, preferiblemente `14.23.5.2860`.
3. Copia `voxelcore-0.2.0.jar` desde `build/libs/` a la carpeta `.minecraft/mods`.
4. Inicia el perfil de Forge 1.12.2.

La mena solo aparecerá en chunks generados después de instalar el mod. No modifica los chunks antiguos. La generación se ejecuta en el lado del mundo/servidor para mantener la consistencia en multijugador.

## Compilar

Esta es una rama antigua de Forge; se requiere el entorno compatible:

- JDK 8 de 64 bits.
- Gradle 4.10.3.
- Conexión a Maven Central y al Maven de Forge para descargar dependencias durante la primera compilación.

Linux/macOS:

```bash
./build.sh
```

Windows:

```bat
build.bat
```

El comando ejecutado es `gradle setupDecompWorkspace build`. El artefacto se genera en `build/libs/voxelcore-0.2.0.jar`.

También hay un workflow en `.github/workflows/build.yml` que prepara JDK 8 y Gradle 4.10.3 y publica el JAR como artefacto de GitHub Actions cuando el proyecto se sube a un repositorio.

## Opciones

| Categoría | Opción | Predeterminado | Descripción |
|---|---|---:|---|
| diagnostics | `showOverlay` | true | Mostrar el HUD. |
| diagnostics | `showChunkCount` | true | Mostrar chunks cliente cargados. |
| diagnostics | `showMemory` | true | Mostrar uso de heap Java. |
| diagnostics | `showEntityCount` | true | Mostrar entidades cargadas. |
| diagnostics | `showTileEntityCount` | false | Mostrar tile entities cargadas. |
| performance | `enableEntityDistanceCulling` | false | Omitir el render de criaturas vivas lejanas. |
| performance | `entityDistancePercent` | 85 | Umbral relativo a la distancia de visión. |
| performance | `forceMinimalParticles` | false | Forzar el ajuste nativo de partículas mínimas. |
| world_generation | `generateResoniteOre` | true | Activar generación de mena en chunks nuevos. |
| world_generation | `oreVeinsPerChunk` | 4 | Intentos de vetas por chunk. |
| world_generation | `oreVeinSize` | 5 | Bloques por veta. |
| world_generation | `oreMinY` | 5 | Altura mínima de generación. |
| world_generation | `oreMaxY` | 48 | Altura máxima de generación. |

## Validación de recursos

Se incluye `tools/validate_assets.py` para comprobar JSON de modelos/blockstates/recetas, referencias a texturas, dimensiones PNG y claves duplicadas de traducción. Puede ejecutarse con `python3 tools/validate_assets.py`; el workflow de CI la ejecuta antes de la compilación. Si Pillow no está instalado, se omite únicamente la comprobación de decodificación/dimensiones de PNG.

## Límites técnicos conocidos

Esta versión no anuncia como implementados sistemas que aún necesitan una transformación profunda del motor:

- No reemplaza el renderizador de chunks ni implementa VBO/Render Regions propios.
- No hace carga paralela real de chunks ni cambia la cola de actualización interna de Minecraft.
- No incluye shaders propios, sombras avanzadas, iluminación dinámica de objetos ni un renderizador de agua personalizado.
- El recorte de entidades es solo visual: no reduce su IA ni su lógica de servidor.
- El ajuste de partículas usa la opción nativa `Minimal`; no añade un administrador nuevo de partículas.

Estas limitaciones son deliberadas: es preferible una base pequeña que arranque y se pueda probar antes que introducir parches ASM no verificados que puedan causar pantallas negras o incompatibilidades.

## Arquitectura

- `VoxelCore`: punto de entrada, ciclo de vida y registro de generación/recetas de horno.
- `proxy`: separación cliente/servidor.
- `config`: opciones persistentes y validación de valores.
- `client/handler`: telemetría, eventos visuales, teclas, modelos y control de partículas.
- `client/gui`: pantalla de configuración de Forge.
- `registry` y `content`: bloques, objetos, herramientas y armaduras.
- `world`: generación de mena en chunks nuevos.

## Compatibilidad y seguridad

- Minecraft Java 1.12.2.
- Minecraft Forge 1.12.2 (`14.23.5.2860`).
- Java 8 para compilar y ejecutar esta versión de Forge.
- No incorpora ni redistribuye OptiFine.
- La optimización de entidades y partículas está desactivada por defecto.
- El contenido puede usarse en mundo individual o servidor con VoxelCore instalado en el servidor y en los clientes.
