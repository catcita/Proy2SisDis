# ⛽ Proyecto 2: Sistema Distribuido de Gestión de Combustibles (3-Tier)

**Curso:** Sistemas Distribuidos
**Integrantes:** Catalina Herrera González

---

## Resumen del Proyecto

Implementación de un sistema distribuido en **Java** basado en una arquitectura de tres niveles para la gestión remota de precios y la recolección de transacciones en tiempo real en una red de estaciones de servicio.

El proyecto aborda la comunicación transversal mediante **Sockets TCP** y garantiza la resiliencia y consistencia de los datos frente a fallos de conexión y réplicas.

## Arquitectura y Tecnologías

La solución se estructura en una arquitectura 3-Tier, con las siguientes responsabilidades y tecnologías:

| Nivel | Componente | Rol | Protocolo |
| :--- | :--- | :--- | :--- |
| **Nivel 3** | Administración Central | Servidor Maestro. Define precios base y consolida reportes. | TCP (Internet) |
| **Nivel 2** | Distribuidor | Cliente de N3 y Servidor de N1. Aplica factor de utilidad, gestiona colas de precios y mantiene la **Base de Datos con Redundancia Local**. | TCP (Internet/Local) |
| **Nivel 1** | Cliente (Surtidor) | Cliente terminal. Ejecuta cargas y registra transacciones. Implementa la **restricción de no actualizar precio en operación**. | TCP (Local) |

**Tecnologías Utilizadas:**
* **Lenguaje:** Java (JDK 8+)
* **Comunicación:** Sockets TCP (para garantizar fiabilidad y orden de los mensajes).
* **Persistencia:** Simulación de Base de Datos local mediante archivos CSV (para demostrar Redundancia y Detección de Inconsistencia).
* **Interfaz:** Java Swing (para la configuración y visualización de logs).

---

## Configuración y Ejecución

Para poner en marcha la aplicación en modo local (`localhost`), siga los siguientes pasos:

### 1. Requisitos

* Java Development Kit (JDK 8 o superior).
* Un IDE que soporte Java (IntelliJ IDEA recomendado).

### 2. Secuencia de Inicio

Es **CRÍTICO** iniciar los componentes en el siguiente orden para establecer la comunicación jerárquica:

1.  **Nivel 3 (Administración):** Ejecutar `nivel3.AdministracionGUI.main()`.
    * Presionar **`Iniciar`** (Puerto por defecto: 6000).
2.  **Nivel 2 (Distribuidor):** Ejecutar `nivel2.DistribuidorGUI.main()`.
    * Presionar **`Iniciar Servidor`** (Puerto local por defecto: 5001).
    * Presionar **`Conectar Admin`** para establecer la conexión con N3.
3.  **Nivel 1 (Cliente/Surtidor):** Ejecutar `nivel1.ClienteGUI.main()`.
    * Presionar **`Conectar`** (IP Distribuidor: `localhost`, Puerto: 5001).

---
Link Video: https://youtu.be/MMLxriNv5mM