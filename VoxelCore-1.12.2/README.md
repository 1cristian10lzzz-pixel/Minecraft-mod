# Minefade Mod 0.12.0 — Minecraft Java 1.12.2

Forge 14.23.5.2864 · Java 8 · ForgeGradle 3.x (UserDev 3) · Gradle 5.6.4

Minefade 0.12.0 es una actualización de realismo ambiental, interacción con materiales y rendimiento. Mantiene el mod ID `voxelcore`, los IDs registrados de bloques/objetos, la simulación vanilla de fluidos y el contenido existente.

## Realismo de agua y lluvia

- Estelas, salpicaduras de entrada e impactos de lluvia calculan la altura de la superficie con el método vanilla `BlockLiquid.getLiquidHeight`. Los efectos siguen los niveles de agua fuente y corriente en lugar de usar una altura aproximada fija.
- La espuma de orilla interpola la altura por vértice para permanecer pegada a superficies suavizadas y bordes inclinados.
- Añade pequeños impactos de lluvia en suelo expuesto cerca del jugador; los impactos en superficies de agua siguen siendo gestionados por el sistema de agua.
- La profundidad submarina se reutiliza entre eventos de niebla y los escaneos de agua/cascadas usan posiciones mutables.

**Límite de renderizado:** los reflejos siguen siendo una aproximación de color del cielo y ángulo de visión; no hay SSR, refracción físicamente exacta ni pipeline completo de shaders.

## Interacción con materiales

- Los pasos se disparan por distancia recorrida y alternan los pies, en vez de depender de un intervalo fijo de ticks.
- Nieve utiliza partículas de polvo de nieve; arena y tierra generan restos de su bloque; roca, madera, hielo, hojas y enredaderas generan partículas derivadas de su estado real.
- Los fragmentos al golpear materiales duros tienen una opción independiente de los detalles procedurales minerales.
- Los contactos de vegetación y slime limpian su estado cuando se descarga el mundo.

## Detalles procedurales de bloques

Sin cambiar texturas ni crear imágenes, el renderizador añade pequeñas facetas de color a más bloques vanilla: variantes de piedra, cobblestone, gravel, netherrack, end stone, sandstone, red sandstone, obsidian, prismarine, stone bricks, hardened clay, bricks, quartz, arena roja, dirt/coarse dirt/podzol, grass block/path, mycelium, soul sand, magma, nether bricks, nether wart block, hielo, packed/frosted ice, bone block, glowstone, sea lantern, end bricks y purpur.

Los acentos son geometría ligera de color por vértice. No sustituyen modelos, texturas ni estados de bloque.

## Animaciones

La capa del jugador sigue siendo aditiva y deja las posturas base de Minecraft bajo control vanilla. Se añaden pequeños movimientos de peso, carrera, escalada, natación, respiración y un balanceo de brazos muy leve durante el reposo cuando las manos están libres. El movimiento de reposo no se aplica durante el uso de objetos ni los ataques.

## Rendimiento y estabilidad

- Los pases de renderizado consultan y restauran solo los estados OpenGL que realmente modifican Minefade.
- Se liberan las referencias de mundo de los controladores de vegetación, materiales e impactos en WorldEvent.Unload.
- El contador de chunks incluye la dimensión.
- El ajuste de partículas mínimas restaura la preferencia del usuario al salir del mundo.
- Los radios/intervalos de agua y lava siguen la calidad seleccionada y el modo adaptativo.
- Física de objetos, recogida, lanzamiento, Resonite y generación del mundo se mantienen.

## Lava

La lava mantiene vetas incandescentes ramificadas precalculadas con núcleo luminoso, borde oscuro, pulsación suave y pocas partículas de humo/brasas. Se añade un leve tinte cálido al entrar en lava sin sustituir la niebla, textura, flujo, daño o colisión vanilla.

**Límite de renderizado:** las vetas son un overlay procedural, no un material reemplazado ni un shader de distorsión térmica.

## Ajustes de vídeo

La pantalla existente conserva su distribución inspirada en OptiFine: Quality, Details, Performance, Animations, Water/Lava, Items, World y Other. No se crea una interfaz nueva ni se añaden imágenes.

## Compatibilidad y pruebas

Minecraft Java 1.12.2 · Forge 14.23.5.2864 · Java 8 · ForgeGradle 3.x · Gradle 5.6.4.

CI comprueba compilación y compatibilidad fuente/API. La comprobación visual dentro del cliente, medición en diferentes GPU, multijugador y pruebas prolongadas siguen siendo necesarias.
