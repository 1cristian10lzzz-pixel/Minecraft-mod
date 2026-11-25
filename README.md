# Minefade Mod

Proyecto de mod para **Minecraft Java 1.12.2 + Forge**.

La primera entrega importada es **VoxelCore 0.2.0**, base inicial del mod Minefade: configuración de rendimiento, métricas de cliente, control de partículas, seguimiento de chunks, contenido de resonita y recursos propios.

## Código fuente

El archivo `MinefadeMod-VoxelCore-0.2.0.tar.gz` contiene el proyecto completo de esta versión: código Java, configuración Forge, recursos, modelos, recetas, traducciones, texturas, scripts de compilación, validador y flujo de GitHub Actions.

Para extraerlo:
```bash
tar -xzf MinefadeMod-VoxelCore-0.2.0.tar.gz
```

En Windows puedes utilizar 7-Zip para extraer el archivo.

## Compilación

Requisitos: JDK 8 y Gradle 4.10.3.

En Windows:
```bat
build.bat
```

En Linux/macOS:
```bash
./build.sh
```

El artefacto esperado se genera en `build/libs/`.

## Estado de la entrega

Esta importación contiene el **código fuente**, no un JAR precompilado. La compilación debe ejecutarse en un entorno compatible con Java 8 y ForgeGradle 2.3. No se afirma que el JAR haya sido compilado ni probado todavía.

## Objetivo del proyecto

Minefade Mod no será únicamente un optimizador: la arquitectura debe permitir ampliar Minecraft con bloques, objetos, herramientas, armaduras, entidades, recetas, estructuras, mecánicas, interfaces y módulos gráficos, además de las funciones de rendimiento.
