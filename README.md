# Minefade Mod — Minecraft Java 1.12.2

Current source line: **0.14.0** · Forge 14.23.5.2864 · ForgeGradle 3.x · Gradle 5.6.4 · Java 8

Minefade improves existing Minecraft 1.12.2 rendering and gameplay systems while retaining the `voxelcore` mod ID, registered content IDs, vanilla fluid simulation and original gameplay content.

## 0.14.0 — Contacto ambiental de criaturas

- Añade pasos discretos para criaturas y jugadores remotos cuando caminan sobre nieve, arena, tierra, grava o vegetación; las partículas utilizan el estado real del bloque.
- Añade pequeños impactos de aterrizaje después de caídas y salpicaduras al entrar o salir del agua, calculando la altura visible del fluido.
- Los efectos se ejecutan únicamente en el cliente, cerca de la cámara, respetan los ajustes existentes de partículas/pasos/agua y limpian referencias al descargar el mundo.
- No altera IA, movimiento, colisiones, daño, combate, inventarios ni simulación del servidor.

## 0.13.0 — Impactos de aterrizaje y transiciones de agua

- **Aterrizajes por material:** tras una caída suficiente aparecen partículas discretas del suelo real; la nieve levanta polvo y los suelos sólidos desprenden fragmentos.
- **Física vanilla intacta:** no se cambia el daño de caída, los rebotes de slime, las colisiones ni el movimiento del jugador.
- **Salidas del agua:** añade una salpicadura breve al salir cerca de una superficie; el seguimiento de inmersión evita entradas falsas cuando el escaneo encuentra tarde el agua.

## 0.12.0 — Realismo de materiales, fluidos e interacción

- **Agua por nivel real:** estelas, salpicaduras de entrada e impactos de lluvia ahora usan `BlockLiquid.getLiquidHeight`; ya no aparecen a una altura fija sobre los bloques de agua corriente.
- **Espuma adherida al terreno:** cada vértice de la espuma se interpola con la superficie suavizada del agua, incluso en orillas con desnivel.
- **Lluvia en tierra:** añade pequeños impactos sobre suelo expuesto bajo la lluvia, además de los impactos en agua.
- **Pasos con ritmo real:** el paso se calcula por distancia recorrida y alterna pie izquierdo/derecho, en lugar de activarse con un temporizador fijo.
- **Partículas según material:** nieve con polvo de nieve; arena y tierra con restos de bloque; madera, hielo, roca, hojas y enredaderas con partículas derivadas de su bloque real.
- **Impactos independientes:** los fragmentos al golpear roca, hierro, vidrio o hielo tienen su propio ajuste y ya no dependen de los detalles minerales decorativos.
- **Más bloques con detalles procedurales:** añade arena roja, dirt/coarse dirt/podzol, césped, micelio, soul sand, magma, ladrillos del Nether, wart block, hielo normal/compacto/frágil, bone block, glowstone, sea lantern, end bricks y purpur.
- **Animación de reposo:** añade un balanceo muy leve de brazos cuando el jugador está quieto y tiene las manos libres; no interviene durante ataques ni al usar objetos.
- **Memoria y cambios de mundo:** los controladores de vegetación, impactos, materiales y detalles liberan sus referencias al descargar un mundo.
- **Renderizado:** el guardado de estado OpenGL consulta solo los estados que los pases de Minefade modifican, reduciendo lecturas innecesarias del controlador gráfico.

## 0.11.0 — Estabilidad y optimización general

- El contador de chunks distingue dimensiones.
- Restaura la preferencia original de partículas al salir del mundo.
- Cachea la profundidad submarina y optimiza los escaneos de agua, cascadas, lava, materiales y objetos.
- Corrige los impactos de slime y la limpieza de cachés por dimensión.
- Escala el radio de los efectos de agua y lava según calidad y FPS.

## 0.10.0 — Realismo visual y actualización de Forge

- Mejoras de agua y lava, páginas de ajustes inspiradas en OptiFine y animaciones aditivas del jugador.
- Objetivo: Forge 14.23.5.2864 con ForgeGradle 3.x (UserDev 3), Gradle 5.6.4 y Java 8.

## Enfoque de renderizado y límites

El agua y la lava siguen siendo fluidos vanilla con detalles de superficie del lado del cliente. El agua utiliza microvariación de altura y luz sutil, pero los reflejos son una aproximación basada en el color del cielo, no reflejos en pantalla ni refracción física. La lava utiliza grietas ramificadas precalculadas, no un shader de distorsión térmica ni simulación física del magma. No se añaden ni modifican imágenes, texturas o resource packs, y no se reemplaza el renderizador vanilla de chunks.

El menú sigue la organización de categorías de OptiFine, pero es una implementación independiente de Minefade, no código de OptiFine ni paridad completa de funciones.

## Compatibilidad y verificación

Objetivo: Minecraft Java 1.12.2 · Forge 14.23.5.2864 · ForgeGradle 3.x · Gradle 5.6.4 · Java 8.

La compilación de GitHub Actions valida compatibilidad de fuente/API. No sustituye las pruebas visuales en una partida, las mediciones en GPU distintas ni las pruebas multijugador y de sesiones prolongadas. El JAR validado se instala en la carpeta Forge `.minecraft/mods`.
