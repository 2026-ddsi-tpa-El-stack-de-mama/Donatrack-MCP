# donatrack-mcp

MCP server **local (stdio)** para operar DonaTrack desde Claude Desktop. Claude Desktop lanza el proceso
(`java -jar`) y habla JSON-RPC por stdin/stdout; el servidor delega por HTTP en los cuatro módulos y en el
Servicio de Consultas, **siempre a través del API Gateway**.
No contiene reglas de negocio: cada tool es un adaptador fino y los errores de los módulos se propagan tal cual.

## Build
    mvn clean package          # genera target/donatrack-mcp.jar

## Verificar ANTES de tocar Claude Desktop
    npx @modelcontextprotocol/inspector java -jar target/donatrack-mcp.jar
Debe listar 57 tools (una por intención de negocio, ver abajo). Si el Inspector no conecta, el problema es del
server (casi seguro stdout contaminado o la propiedad stdio), no de Claude Desktop.

## API Gateway
El MCP solo conoce una URL: `GATEWAY_BASE_URL`. Cada request sale como `<gateway>/<prefijo><path del módulo>`.
El gateway (`API-Gateway`) tiene un método por endpoint, así que una tool nueva necesita su ruta allá.
El prefijo es obligatorio porque hay paths que se repiten entre módulos (por ejemplo `/donadores/...` existe
en Donadores y Entidades y en Incentivos).

| Servicio | Prefijo por defecto | Variable para cambiarlo |
|---|---|---|
| Logística | `/logistica` | `PREFIJO_LOGISTICA` |
| Donaciones | `/donaciones` | `PREFIJO_DONACIONES` |
| Incentivos | `/incentivos` | `PREFIJO_INCENTIVOS` |
| Donadores y Entidades | `/donadoresyentidades` | `PREFIJO_DONADORES_ENTIDADES` |
| Servicio de Consultas | `/consultas` | `PREFIJO_CONSULTAS` |

`estado_servicios` pide `HEALTH_PATH` (por defecto `/actuator/health`) solo al gateway. A cada servicio lo prueba con
un GET real (`SistemaTools.sonda`), porque el gateway no reenvía su actuator.

## Claude Desktop
1. Copiar `claude_desktop_config.example.json` dentro de `%APPDATA%\Claude\claude_desktop_config.json`
   (fusionando con `mcpServers` si ya existe) y completar las rutas y `GATEWAY_BASE_URL`.
2. Usar ruta ABSOLUTA a `java.exe`: Claude Desktop no hereda el PATH de tu terminal.
3. Cerrar Claude Desktop por completo (bandeja / Administrador de tareas) y abrirlo de nuevo.
4. Logs: `%APPDATA%\Claude\logs\mcp-server-donatrack.log` (acá aparece todo lo que sale por stderr).

## Reglas para agregar tools
- Una clase `*Tools` por módulo/área, un método por intención de negocio (no por endpoint).
- `description` = el prompt de la tool: qué hace, qué ids necesita y de qué otra tool salen.
- Sin validaciones de dominio: si el módulo rechaza, el mensaje de `DownstreamException` llega al modelo.
- Jamás `System.out`: stdout es el protocolo.
- Las tools corren de a una (`McpServerConfig`): el transporte stdio no admite dos respuestas simultáneas y, sin esto, dos tools en paralelo tiran abajo el proceso. Una tool lenta demora a las siguientes.

## Catálogo de tools (57)
| Clase | Tools |
|---|---|
| `FlujosTools` (los 6 flujos de la consigna) | realizar_donacion, reportar_entrega_paquete, registrar_queja_donacion, procesar_donador, registrar_necesidad, obtener_estadisticas_donador |
| `DonacionesTools` | consultar_donaciones, buscar_donaciones_de_donador_desde, cambiar_estado_donacion, consultar_historial_donaciones, consultar_productos, crear_producto, consultar_categorias, crear_categoria, consultar_identificadores, crear_identificador, modificar_producto, eliminar_donacion, eliminar_producto, eliminar_categoria, eliminar_identificador |
| `LogisticaTools` | consultar_depositos, crear_deposito, consultar_asignaciones, consultar_historial_asignaciones, consultar_paquete, consultar_stock, eliminar_deposito |
| `IncentivosTools` | consultar/crear/modificar_insignia(s), asignar_insignia_a_donador, consultar/crear/modificar_mision(es), asignar_mision_a_donador, eliminar_insignia, eliminar_mision |
| `DonadoresEntidadesTools` | consultar_donadores, crear_donador, cambiar_estado_donador, consultar_si_puede_donar, consultar_historial_donador, consultar/crear/modificar_entidad(es), consultar_necesidades, consultar_necesidades_insatisfechas, satisfacer_necesidad, modificar_necesidad, eliminar_necesidad |
| `ConsultasTools` (modelo de lectura) | ficha_integral_donador, necesidades_urgentes_sin_cubrir, estado_depositos, calidad_donaciones |
| `SistemaTools` | estado_servicios, resumen_sistema |

## Contrato con el Servicio de Consultas
La URL final es `/consultas` + el path de la tabla. Los parámetros son opcionales. Si cambia, se ajusta en `ConsultasTools`.
Las consultas 2 y 4 son una PROPUESTA: todavía no existen ni en el Servicio de Consultas ni en el gateway.

| Tool | Endpoint | Query |
|---|---|---|
| `ficha_integral_donador` | `GET /fichaIntegralDonador/{id}` | |
| `necesidades_urgentes_sin_cubrir` | `GET /necesidadesUrgentes` | `urgenciaMinima`, `tipo`, `categoriaId`, `subcategoriaId` |
| `estado_depositos` | `GET /estadoDepositos` o `GET /estadoDepositos/{id}` | |
| `calidad_donaciones` | `GET /calidadDonacion` | `topDonadores` |

## Endpoints deliberadamente NO expuestos
- `DELETE /donaciones` sin id (borra todas) y `POST /admin/reset`: un typo de lenguaje natural no debe poder vaciar la base. Usar Postman.
- `POST /depositos/{id}/donacion`, `POST /asignaciones`, `POST /asignacionesDirecta`, `POST /stock/{id}`: pasos internos del flujo de donación; exponerlos permite saltearse la orquestación de Donaciones.
- `PATCH /donadores/{id}/categoria` (ambos módulos): la categoría es consecuencia de `procesar_donador`.
- `POST /donadores/{id}/quejas`: duplicaría semánticamente a `registrar_queja_donacion`; el modelo elegiría mal.

## Bajas y modificaciones que no existen en los módulos
No hay tool porque no hay endpoint: `PUT /depositos/{id}`, `PUT /donadores/{id}`, `DELETE /donadores/{id}`,
`DELETE /entidades/{id}`, `PUT /categorias/{id}`, `PUT /identificadores/{id}`. La baja de un donador hoy se resuelve con
`cambiar_estado_donador`.

## Alarmas
Monitores de Datadog en [`alarmas/`](alarmas/README.md). No forman parte del proceso del MCP.

## Convenciones de parámetros
- Todos los parámetros son escalares tipados con los valores de los enums en la `description` (EstadoDonacionEnum, EstadoDonadorEnum, TipoNecesidadMaterialEnum, TipoAlgoritmoEnum, CategoriaDonadorEnum, TipoMisionEnum, TipoIdentificadorEnum). Si un enum cambia en un módulo, actualizar la descripción de la tool correspondiente.
- Los campos opcionales nulos se omiten del JSON (`Args.body`): el valor por defecto lo decide el módulo, no el MCP.
- Supuesto sin verificar: `POST /depositos` recibe la entidad `Deposito` (no el DTO); `crear_deposito` asume que sus campos coinciden con `DepositoDTO`. Probarlo primero.
