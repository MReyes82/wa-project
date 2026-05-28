## Pruebas Unitarias

```bash
cd /home/mreyes/Desktop/wa-project/backend
mvn test
```

## Pruebas de Integración con Testcontainers

Estas pruebas levantan un contenedor temporal de MySQL y ejecutan operaciones DAO contra esa base de datos. Se necesita Docker corriendo.

```bash
cd /home/mreyes/Desktop/wa-project/backend
mvn -DskipITs=false verify
```