# Inventory Indicator (Fabric 1.20.1)

Mostra o inventario completo dos outros jogadores num painel ao lado deles (estilo health indicator).

- Tecla K liga/desliga (mude em Controles).
- O mod precisa estar no SERVIDOR e no CLIENTE (o cliente so conhece mao/armadura dos outros; o servidor envia o resto).
- Alcance: 24 blocos (InvIndicator.RANGE). Visual: constantes no topo de InvRenderer.java.

## Compilar (JDK 17)
1. Copie `gradle/`, `gradlew`, `gradlew.bat` do fabric-example-mod (ou rode `gradle wrapper` se tiver Gradle instalado).
2. `./gradlew build`  -> `build/libs/invindicator-1.0.0.jar`
3. Coloque o jar em `mods/` (servidor e clientes) junto com o Fabric API 0.92.2+1.20.1.
