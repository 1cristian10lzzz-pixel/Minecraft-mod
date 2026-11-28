## 0.14.0 — Contacto ambiental de criaturas y rendimiento local

- Añade pasos con partículas de material para entidades vivas cercanas (criaturas, animales y jugadores remotos) sobre nieve, arena, tierra, grava y vegetación.
- Añade impactos de aterrizaje por material para entidades cercanas tras caídas suficientes, manteniendo intactos daño, IA, movimiento y colisiones.
- Añade salpicaduras pequeñas al entrar/salir del agua; la superficie se obtiene con `BlockLiquid.getLiquidHeight` y se actualiza de forma espaciada.
- Limita los efectos al radio cercano de la cámara, usa seguimiento con referencias débiles y borra el estado al descargar el mundo.
- Mejora la detección de reemplazo del jugador tras reaparecer para reiniciar las distancias y transiciones de contacto sin falsos efectos.
- Restablece el temporizador interno de agua cuando se abandona un mundo y fuerza un nuevo escaneo cuando cambia la entidad del jugador.

## 0.13.0 — Impactos de aterrizaje y transiciones de agua

- Añade partículas de aterrizaje según el material: nieve desprende polvo; piedra, tierra, arena y otros suelos sólidos levantan fragmentos discretos según la altura de la caída.
- El efecto usa la distancia de caída acumulada y respeta la calidad de partículas. No cambia el daño de caída, las colisiones, el rebote del slime ni la física vanilla.
- Corrige el seguimiento del estado de inmersión para que encontrar tarde una superficie cercana no provoque una salpicadura falsa de entrada.
- Añade una salpicadura pequeña al salir del agua, limitada a una superficie realmente cercana y a una diferencia de altura razonable.
- Evita consultar el bloque bajo el jugador en cada tick: la lectura de apoyo se realiza solo al detectar un aterrizaje.

## 0.12.0 — Realismo de materiales, fluidos e interacción

- Corrige la altura de estelas, salpicaduras al entrar al agua e impactos de lluvia usando `BlockLiquid.getLiquidHeight`; los efectos siguen el nivel real del agua corriente.
- Interpola la espuma por vértice y la adhiere a la altura suavizada de cada lado de la superficie.
- Añade pequeños impactos de lluvia en superficies sólidas expuestas cerca del jugador.
- Reemplaza el temporizador fijo de pasos por una cadencia basada en distancia, alternancia de pie y partículas específicas de nieve, arena, tierra, madera, hielo, roca, hojas y enredaderas.
- Separa los impactos al golpear materiales duros del interruptor de detalles minerales procedurales.
- Amplía los acentos de superficie a arena roja, variantes de dirt, césped, micelio, soul sand, magma, nether bricks, nether wart block, hielo, packed/frosted ice, bone block, glowstone, sea lantern, end bricks y purpur.
- Añade microbalanceo de brazos en reposo solo con las manos libres, sin intervenir en ataques o uso de objetos.
- Limpia las referencias de mundo de los controladores de vegetación/materiales al descargar mundos.
- Reduce las consultas OpenGL guardando únicamente los estados modificados por las pasadas de renderizado de Minefade.
- Mantiene vanilla water/lava, física de bloques, IDs, contenido Resonite y simulación de mundo. No crea ni modifica imágenes, texturas ni resource packs.
- Objetivo de compilación: Minecraft 1.12.2 / Forge 14.23.5.2864 / Java 8 / ForgeGradle 3.x / Gradle 5.6.4.

## 0.11.0 — Mejoras generales de estabilidad y rendimiento

- Corrige el contador de chunks para separar correctamente las coordenadas por dimensión (Overworld, Nether, End).
- Limpia las cachés de agua, lava y detalles minerales al descargar mundos/dimensiones; restablece el monitor de rendimiento y la preferencia original de partículas.
- Conecta el modo de partículas mínimas al ciclo de ticks del cliente; lo aplica solo durante la partida y restaura/guarda la preferencia del usuario al desactivarlo o salir del mundo.
- Cachea la profundidad submarina durante cuatro ticks y comparte el resultado entre eventos de color/densidad de niebla.
- Reutiliza BlockPos.MutableBlockPos en las búsquedas de agua, cascadas, vecinos de materiales y física de objetos en fluidos para reducir asignaciones temporales.
- Adapta radio e intervalo de escaneo de las superficies de agua y lava a la calidad seleccionada y a la reducción adaptativa por FPS.
- Alinea los brillos y la espuma del agua con las alturas suavizadas de la superficie en vez de dibujarlos a una altura plana.
- Precalcula las franjas laterales de las grietas de lava para quitar raíces cuadradas del renderizado por fotograma; limita el escaneo al tope del mundo y añade un leve tinte cálido al entrar en lava sin sustituir la densidad de niebla vanilla.
- Corrige los contactos de slime para detectar la transición de caída a suelo y mantiene actualizado el estado del jugador aunque los efectos estén desactivados.
- Corrige la limpieza del enfriamiento de partículas al cambiar de mundo o retroceder el tiempo del mundo.
- Reduce a la mitad la frecuencia de la recuperación de recogida de objetos en servidor y hace estable el intervalo de flotación por mundo.
- Mantiene el mod ID `voxelcore`, todos los IDs registrados, el contenido Resonite y la simulación vanilla de fluidos. No crea ni modifica imágenes, texturas ni resource packs.
- Compila contra Minecraft 1.12.2 / Forge 14.23.5.2864 con ForgeGradle 3.x, Gradle 5.6.4 y Java 8. Las pruebas visuales dentro del juego, GPU y multijugador siguen siendo necesarias.

## 0.10.0 — Superficie de fluidos, renderizado y estabilidad

- Actualiza la configuración de compilación a Minecraft Forge 1.12.2-14.23.5.2864 y migra a ForgeGradle 3.x/UserDev 3 con Gradle 5.6.4, ya que ForgeGradle 2.3 no resuelve el artefacto UserDev publicado para Forge 2864.

- Replaces the flat water overlay with a gently varying micro-surface. Uses vanilla `BlockLiquid.getLiquidHeight` for source/falling-fluid heights and softly shares corners between adjacent columns only when their heights are close; it does not bridge banks or separate pools.
- Adds small multi-directional surface relief and analytical normals for directional specular response. The effect is deliberately subtle and retains vanilla water texture/flow beneath the low-alpha tint.
- Moves the frame-wide sun vector calculation out of per-vertex work and uses analytical wave gradients rather than finite-difference sampling; simplifies the Fresnel/specular power operations to reduce repeated expensive math.
- Replaces sliding lava stripes with precomputed, stationary branching fissure geometry, dark red rims, hot orange cores and slow pulsing junctions. Uses vanilla fluid-height calculations and reuses mutable block positions during scans.
- Adds shared OpenGL state backup/restore for blend factors, texture, lighting, culling, depth, alpha test, colour material, rescale normal and current colour so Minefade overlays do not leak their render state into later renders.
- Fixes vegetation contact to inspect plant blocks above the ground support block. Limits each step to one tiny nearby contact puff.
- Reduces mineral-detail scan radius, vertical range, cache size and refresh frequency.
- Precomputes lava fissure layout when rebuilding its cache so per-frame work only animates the glow; limits ember sampling attempts.
- Refines swimming animation so paddling fades out when the player is stationary and synchronizes the headwear overlay with the head pose.
- Improves grounded item renderer cleanup while retaining vanilla baked models and item atlas.
- Keeps vanilla fluid simulation, block/item IDs, Resonite content, dropped-item rules and world generation. No images, textures or resource packs are created or modified.
- CI compilation checks Forge/API compatibility. Visual checks in a live client, GPU profiling, multiplayer and long-session testing remain separate tasks.

## 0.9.0 — Fluidos, optimización y ajustes de vídeo

- Reworks the water overlay after the prior screenshot showed an overly opaque, pale surface: alpha is reduced to a subtle tint so vanilla water texture and flow remain visible.
- Replaces the 4x4 subdivided water-cell grid with one batched quad per cached surface column; bounds the surface search/cache and shortens the vertical scan.
- Adds restrained sky-colour/specular response, small animated streak highlights and low-alpha shoreline foam without geometric waves or expanding ring geometry.
- Fixes repeated nearby-water scans on dry land by caching empty results and rechecking only after movement thresholds or a timer.
- Adds a distinct client-only lava renderer with moving molten vein accents and sparse smoke/ember particles. Lava quality, scan radius and ember frequency adapt to sustained low FPS. Vanilla lava remains the base texture and retains its flow, light, damage, collision and server state.
- Reworks player animation into a conservative additive layer that preserves vanilla crouch, jump, Elytra and fall base poses; adds measured walking weight shift, sprint follow-through, climbing, swimming and idle breathing.
- Replaces the old settings centre with an OptiFine-inspired Video Settings layout: Quality, Details, Performance, Animations, Water/Lava, Items, World and Other pages. Adds controls for new water, lava, material and animation options plus selected vanilla graphics options.
- Prevents the global GUI skin handler from restyling the new video-settings screen.
- Adds persistent config values for water surface quality/highlights/foam, lava quality/radius/embers, surface-detail density, material contacts and player animation strength.
- Reduces optional water/stone/lava render work; caches empty water lookups, uses reusable frame-time percentile storage and packed chunk coordinates.
- Keeps vanilla fluid simulation, existing item physics, Resonite content, world generation and registered IDs. No images, textures or resource packs are created or modified.
- Water reflections remain an approximation; lava details are a low-alpha overlay, not full PBR/refraction/heat distortion. In-game visual and multiplayer testing are still required.

## 0.8.0 — Realismo ambiental, materiales y animaciones

- Adds deterministic procedural mineral-grain highlights to exposed faces of stone (including granite, diorite and andesite variants), cobblestone, gravel, netherrack, end stone, sandstone, red sandstone, obsidian, prismarine, stone bricks, hardened clay, bricks, quartz blocks and common ores.
- Extends the additive player animation layer with improved weight transfer, torso counter-rotation, sprint arm pumping, crouch posture, ladder climbing, swimming paddles, elytra posture, falling pose and subtle idle breathing.
- Adds lightweight vegetation contact particles when the player walks through tall grass, ferns, vines, dead bushes and foliage.
- Adds small block-coloured chips when striking hard surfaces and restrained material particles when walking over stone, dirt, grass, sand, clay and snow.
- Adds a small block-fragment contact burst when the player lands and receives the vanilla bounce from a slime block; vanilla bounce force and collision are unchanged.
- Adds a calm custom water surface with biome/depth colour, sky-reflection approximation, view-angle Fresnel response, daylight highlights and shoreline foam; no geometric wave displacement or expanding ring geometry.
- Adds underwater colour absorption and depth fog, rising bubbles, local splashes/wakes and waterfall mist.
- Smooths dropped-item buoyancy near fluid surfaces and adds alternating swimming leg kicks.
- Keeps vanilla blocks, fluid flow, collision, slime bounce, harvesting, drops and server simulation intact.
- No GUI/interface files changed. No images, textures or resource packs created or modified.
- Stone detail and water surface scans are bounded and cached; render work is batched.

## 0.7.1 — Animaciones, recogida y rendimiento visual

- Installs an additive player model before vanilla calculates its pose; preserves default/slim skin geometry and armour overlays. Models are cached per renderer to avoid per-frame allocation.
- Binds the vanilla block texture atlas explicitly and displays each dropped stack as one smaller baked model on the ground.
- Adds a server-side pickup recovery that delegates to vanilla EntityItem pickup logic and retains inventory-capacity rules.
- Reduces nearby water-surface search frequency/radius and smooths ripple rings while retaining adaptive quality and batched drawing.
- Throttles optional liquid buoyancy work to reduce world-tick scanning.
- Keeps existing PNG files untouched; does not generate, edit or add images.
- Preserves vanilla water blocks, vanilla chunk rendering, all registered item/block IDs and the mod id "voxelcore".

## 0.6.0 — Física de objetos, render y estilo visual

- Added server-authoritative dropped-item handling: right-click pickup, sprint toss, airborne catch interaction and gentle liquid buoyancy/drag.
- Added a custom EntityItem renderer that places resting item models horizontally while preserving vanilla baked models and lighting.
- Added optional dropped-item distance culling, disabled by default.
- Added the OBJETOS settings tab and item-physics controls.
- Added original 32x32 Resonite item/block icons and leather/aged-metal GUI textures.
- Kept the 0.5.0 batched water ripple renderer, surface cache and adaptive water quality.
- Applied selective Sodium-inspired optimization without replacing the vanilla chunk renderer.
- Preserved the internal mod id "voxelcore", Minecraft 1.12.2 and Forge 14.23.5.2847.

## 0.5.0 — Optimización del renderizado de agua

- Batched all active ripple rings into one render buffer and one draw call per frame.
- Cached nearby water-surface lookups and reduced waterfall scans.
- Added adaptive water quality with FPS hysteresis.
- Capped active ripples and reduced optional visual work at low FPS.
- Cleared cached surfaces and ripples on world changes and when effects are disabled.

## 0.4.0 — Realismo ambiental y agua

- Added client-side surface rings, wakes, water-entry splashes, rain ripples, waterfall mist and biome-aware underwater fog colour.
- Added the Agua settings tab and the K water-effects key.
- Preserved vanilla fluid blocks and server-side water state.

## 0.3.0 — Minefade UI and settings overhaul

- Added shared GUI styling and a settings centre with diagnostics, performance and content tabs.

## 0.2.0

- Added performance diagnostics and original Resonite content, tools, armour and world generation.
