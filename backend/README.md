# Backend

## Unit tests

```bash
cd /home/mreyes/Desktop/wa-project/backend
mvn test
```

## Integration tests (Testcontainers)

These tests start a temporary MySQL container and run DAO operations against it. You need Docker running.

```bash
cd /home/mreyes/Desktop/wa-project/backend
mvn -DskipITs=false verify
```

