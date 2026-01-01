# TP 27 : Test de charge & Observabilité : Concurrence + Verrou DB + Resilience4j + Actuator Metrics

**Cours :** Architecture Microservices : Conception, Déploiement et Orchestration

<img width="959" height="284" alt="image" src="https://github.com/user-attachments/assets/04680dbb-f918-419b-beb2-fd9b61da39ec" />
<img width="958" height="291" alt="image" src="https://github.com/user-attachments/assets/9190963d-f308-4893-9c70-73cca3800517" />
<img width="956" height="364" alt="image" src="https://github.com/user-attachments/assets/f11febb9-d791-4eb1-a569-4a61077ea002" />
<img width="959" height="290" alt="image" src="https://github.com/user-attachments/assets/50b2369e-85f5-4de5-b19f-3d2bfc79c689" />
<img width="959" height="476" alt="image" src="https://github.com/user-attachments/assets/01dca445-87fc-40cc-9c1c-d0cd72397472" />


## Ce que ce TP permet de vérifier

- Des emprunts concurrents arrivent sur 3 instances (8081/8083/8084).
- Le verrou DB empêche le stock de devenir négatif.
- Quand pricing-service tombe, book-service continue grâce au fallback.
- Les métriques Actuator confirment que Retry et CircuitBreaker se déclenchent.

## Prérequis (avant de commencer)

1. Le stack Docker doit être démarré (MySQL + pricing + book-service-1/2/3) :
```bash
docker compose up -d --build
```

2. Vérifier que tout est UP :
```bash
curl -s http://localhost:8082/actuator/health
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8083/actuator/health
curl -s http://localhost:8084/actuator/health
```

### Checkpoint
Chaque commande doit renvoyer `"status":"UP"`.

**Remarque (débutant)**  
Si un service n'est pas UP, ne pas lancer le test de charge : on va juste produire des erreurs "Other".

---

## Partie A — Préparer le terrain (données de test)

### Étape A1 — Créer un livre avec stock connu

On crée un livre avec un stock petit (ex : 10) pour voir rapidement les "épuisés".

```bash
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"TP-Concurrency","author":"Demo","stock":10}'
```

**Résultat attendu**  
HTTP 201 + JSON du livre (avec id).

### Étape A2 — Récupérer l'ID du livre

```bash
curl -s http://localhost:8081/api/books
```

Repérer l'id du livre "TP-Concurrency".

Dans la suite, on note : **BOOK_ID = ...**

**Remarque**  
Le test marche aussi si l'ID n'est pas 1. Il faut juste utiliser le bon ID.

---

## Partie B — Étape "sanity check" : 1 emprunt simple

### Étape B1 — Tester borrow une fois (sans concurrence)

```bash
curl -X POST http://localhost:8081/api/books/BOOK_ID/borrow
```

**Attendu**  
Réponse 200 avec un JSON du type :
- stockLeft décrémenté
- price > 0 (si pricing-service est up)

**Remarque (débutant)**  
Cette étape confirme que :
- l'API fonctionne,
- le livre existe,
- l'appel pricing est opérationnel.

---

## Partie C — Test de charge : 50 emprunts en parallèle (Bash)

### Étape C1 — Créer le script loadtest.sh

Le script `loadtest.sh` est déjà créé à la racine du lab.

Rendre exécutable :
```bash
chmod +x loadtest.sh
```

### Étape C2 — Lancer le test

Remplacer BOOK_ID :
```bash
./loadtest.sh BOOK_ID 50
```

**Résultats attendus**  
Si stock initial = 10 et requests = 50 :
- Success (200) ≈ 10
- Conflict (409) ≈ 40
- Other ≈ 0

**Remarque (débutant)**
- 200 = emprunt réussi
- 409 = plus d'exemplaires (comportement correct)
- Other = problème (service down, mauvais ID, healthcheck KO, etc.)

---

## Partie D — Test de charge Windows (PowerShell)

### Étape D1 — Créer loadtest.ps1

Le script `loadtest.ps1` est déjà créé à la racine du lab.

Exécuter :
```powershell
.\loadtest.ps1 -BookId BOOK_ID -Requests 50
```

---

## Partie E — Vérifier "Stock jamais négatif" (preuve de verrou DB)

### Étape E1 — Lire l'état du stock final

```bash
curl -s http://localhost:8081/api/books
```

**Attendu**
- le livre TP-Concurrency a stock = 0
- jamais stock < 0

**Pourquoi ça marche ?**  
Parce que `findByIdForUpdate()` met un verrou MySQL sur la ligne du livre pendant `@Transactional`.

**Remarque importante**  
Sans verrou DB, sous charge, tu risques :
- stock incohérent
- ou stock négatif (selon implémentation)

---

## Partie F — Résilience en charge : pricing down → fallback

### Étape F1 — Stop pricing-service

```bash
docker compose stop pricing-service
```

**Checkpoint**
```bash
curl -s http://localhost:8082/actuator/health
```
Cela peut échouer (normal : service stop).

### Étape F2 — Créer un nouveau livre avec stock 10

```bash
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"TP-Fallback","author":"Demo","stock":10}'
```

Récupérer l'ID :
```bash
curl -s http://localhost:8081/api/books
```

### Étape F3 — Relancer le test de charge (30 requêtes)

```bash
./loadtest.sh ID_FALLBACK 30
```

**Attendu**
- succès ≈ 10
- conflits ≈ 20
- dans les succès, price doit être **0.0** (fallback)

**Remarque**  
Tu peux ouvrir le fichier success.txt (dans le dossier tmp affiché) pour vérifier les JSON.

### Étape F4 — Relancer pricing-service

```bash
docker compose start pricing-service
```

---

## Partie G — Observabilité : Actuator Metrics (Retry + CircuitBreaker)

### Étape G1 — Exposer /actuator/metrics dans book-service

Dans `book-service/src/main/resources/application.yml` (déjà configuré) :
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics"
```

Rebuild + restart stack :
```bash
docker compose up -d --build
```

**Checkpoint**
```bash
curl -s http://localhost:8081/actuator/metrics
```
Attendu : liste de métriques.

### Étape G2 — Trouver les métriques Resilience4j

**Linux/Mac :**
```bash
curl -s http://localhost:8081/actuator/metrics | grep -i resilience
```

**Windows PowerShell :**
```powershell
(Invoke-RestMethod http://localhost:8081/actuator/metrics).names | Select-String -Pattern "resilience"
```

**Remarque**  
Les noms exacts peuvent varier selon versions, mais tu obtiens la liste réelle disponible.

### Étape G3 — Interpréter (simple)

Pendant que pricing est down et que tu lances loadtest, tu dois observer :
- augmentation des métriques retry (tentatives)
- circuit breaker qui refuse des appels (OPEN) après un seuil

**Astuce pédagogique**  
Activer logs Resilience4j (déjà configuré) :
```yaml
logging:
  level:
    io.github.resilience4j: INFO
```

Puis :
```bash
docker compose logs -f book-service-1
```
Tu verras souvent les transitions (OPEN/HALF_OPEN/CLOSED).

---

## Travail demandé

### 1. Captures / preuves :
- résultat `./loadtest.sh BOOK_ID 50` (succès/conflits)
- `curl /api/books` montrant stock final = 0
- test fallback : pricing stop + loadtest + preuve price=0.0
- `/actuator/metrics` montrant la présence de métriques resilience

### 2. Conclusion (5 lignes)
- expliquer pourquoi le verrou DB est nécessaire en multi-instances
- expliquer le rôle du circuit breaker et du fallback

---

## Architecture du projet

```
tp27/
├── docker-compose.yml
├── loadtest.sh
├── loadtest.ps1
├── pricing-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/tp27/pricing/
│           │   ├── PricingServiceApplication.java
│           │   └── controller/PricingController.java
│           └── resources/
│               └── application.yml
└── book-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/
        └── main/
            ├── java/com/tp27/book/
            │   ├── BookServiceApplication.java
            │   ├── model/Book.java
            │   ├── repository/BookRepository.java
            │   ├── service/BookService.java
            │   ├── controller/BookController.java
            │   ├── client/PricingClient.java
            │   ├── dto/BorrowResponse.java
            │   └── exception/
            │       ├── BookNotFoundException.java
            │       └── OutOfStockException.java
            └── resources/
                └── application.yml
```

## Points techniques clés

### 1. Verrou DB (Pessimistic Locking)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Book b WHERE b.id = :id")
Optional<Book> findByIdForUpdate(@Param("id") Long id);
```
- Crée un `SELECT ... FOR UPDATE` en MySQL
- Empêche les modifications concurrentes
- Garantit que le stock ne devient jamais négatif

### 2. Resilience4j - Circuit Breaker
- `slidingWindowSize: 10` - Fenêtre de 10 appels
- `failureRateThreshold: 50` - Ouvre le circuit si 50% d'échecs
- `waitDurationInOpenState: 10s` - Attend 10s avant de passer en HALF_OPEN

### 3. Resilience4j - Retry
- `maxAttempts: 3` - 3 tentatives maximum
- `waitDuration: 1s` - Délai de 1s entre tentatives
- `exponentialBackoffMultiplier: 2` - Backoff exponentiel

### 4. Fallback
Quand pricing-service est down, retourne `price = 0.0` au lieu d'échouer.

---

## Dépannage

### Services ne démarrent pas
```bash
docker compose logs pricing-service
docker compose logs book-service-1
```

### MySQL connection refused
Attendre que MySQL soit prêt :
```bash
docker compose logs mysql
```

### Tests échouent avec "Other" errors
Vérifier le health de tous les services :
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

### Reconstruire complètement
```bash
docker compose down -v
docker compose up -d --build
```

---

## Bon travail ! 🚀
