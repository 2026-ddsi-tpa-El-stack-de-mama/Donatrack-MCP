# donatrack-mcp

MCP server **local (stdio)** para operar DonaTrack desde Claude Desktop. Claude Desktop lanza el proceso
(`java -jar`) y habla JSON-RPC por stdin/stdout; el servidor delega por HTTP en los cuatro módulos.
No contiene reglas de negocio: cada tool es un adaptador fino y los errores de los módulos se propagan tal cual.

## Build
    mvn clean package          # genera target/donatrack-mcp.jar

## Verificar ANTES de tocar Claude Desktop
    npx @modelcontextprotocol/inspector java -jar target/donatrack-mcp.jar
Debe listar 44 tools (una por intención de negocio, ver abajo). Si el Inspector no conecta, el problema es del
server (casi seguro stdout contaminado o la propiedad stdio), no de Claude Desktop.

## Claude Desktop
1. Copiar `claude_desktop_config.example.json` dentro de `%APPDATA%\Claude\claude_desktop_config.json`
   (fusionando con `mcpServers` si ya existe) y completar rutas y URLs.
2. Usar ruta ABSOLUTA a `java.exe`: Claude Desktop no hereda el PATH de tu terminal.
3. Cerrar Claude Desktop por completo (bandeja / Administrador de tareas) y abrirlo de nuevo.
4. Logs: `%APPDATA%\Claude\logs\mcp-server-donatrack.log` (acá aparece todo lo que sale por stderr).

## Reglas para agregar tools
- Una clase `*Tools` por módulo/área, un método por intención de negocio (no por endpoint).
- `description` = el prompt de la tool: qué hace, qué ids necesita y de qué otra tool salen.
- Sin validaciones de dominio: si el módulo rechaza, el mensaje de `DownstreamException` llega al modelo.
- Jamás `System.out`: stdout es el protocolo.

## Catálogo de tools (44)
| Clase | Tools |
|---|---|
| `FlujosTools` (los 6 flujos de la consigna) | realizar_donacion, reportar_entrega_paquete, registrar_queja_donacion, procesar_donador, registrar_necesidad, obtener_estadisticas_donador |
| `DonacionesTools` | consultar_donaciones, buscar_donaciones_de_donador_desde, cambiar_estado_donacion, consultar_historial_donaciones, consultar_productos, crear_producto, consultar_categorias, crear_categoria, consultar_identificadores, crear_identificador |
| `LogisticaTools` | consultar_depositos, crear_deposito, consultar_asignaciones, consultar_historial_asignaciones, consultar_paquete, consultar_stock |
| `IncentivosTools` | consultar/crear/modificar_insignia(s), asignar_insignia_a_donador, consultar/crear/modificar_mision(es), asignar_mision_a_donador |
| `DonadoresEntidadesTools` | consultar_donadores, crear_donador, cambiar_estado_donador, consultar_si_puede_donar, consultar_historial_donador, consultar/crear/modificar_entidad(es), consultar_necesidades, consultar_necesidades_insatisfechas, satisfacer_necesidad, modificar_necesidad, eliminar_necesidad |
| `SistemaTools` | estado_servicios |

## Endpoints deliberadamente NO expuestos
- `DELETE /donaciones` (borra todas) y `POST /admin/reset`: un typo de lenguaje natural no debe poder vaciar la base. Usar Postman.
- `POST /depositos/{id}/donacion`, `POST /asignaciones`, `POST /asignacionesDirecta`, `POST /stock/{id}`: pasos internos del flujo de donación; exponerlos permite saltearse la orquestación de Donaciones.
- `PATCH /donadores/{id}/categoria` (ambos módulos): la categoría es consecuencia de `procesar_donador`.
- `POST /donadores/{id}/quejas`: duplicaría semánticamente a `registrar_queja_donacion`; el modelo elegiría mal.
- Bajas de donaciones, productos, categorías, insignias, misiones y depósitos; `PUT /productos/{id}` (ver bug en el controller).

## Convenciones de parámetros
- Todos los parámetros son escalares tipados con los valores de los enums en la `description` (EstadoDonacionEnum, EstadoDonadorEnum, TipoNecesidadMaterialEnum, TipoAlgoritmoEnum, CategoriaDonadorEnum, TipoMisionEnum, TipoIdentificadorEnum). Si un enum cambia en un módulo, actualizar la descripción de la tool correspondiente.
- Los campos opcionales nulos se omiten del JSON (`Args.body`): el valor por defecto lo decide el módulo, no el MCP.
- Supuesto sin verificar: `POST /depositos` recibe la entidad `Deposito` (no el DTO); `crear_deposito` asume que sus campos coinciden con `DepositoDTO`. Probarlo primero.
