# LifeTrack

Koşu, kitap ve medya (film/dizi) takibini tek yerde toplayan kişisel takip uygulaması.

| Katman  | Teknoloji                                        |
|---------|--------------------------------------------------|
| Backend | Java 17, Spring Boot 3.3, Spring Security + JWT, JPA |
| Veritabanı | PostgreSQL 16 (Docker)                        |
| Mobil   | Flutter (Dart 3)                                 |

## Proje yapısı

```
backend/   Spring Boot REST API (auth, user, run, book, media, dashboard modülleri)
mobile/    Flutter istemcisi
docs/      API, mimari ve veritabanı notları
```

## Kurulum

### 1. Ortam değişkenleri

```bash
cp .env.example .env
# .env içinde POSTGRES_PASSWORD, DB_PASSWORD ve JWT_SECRET değerlerini doldurun
# JWT_SECRET üretmek için: openssl rand -base64 48
```

### 2. Veritabanı

```bash
docker compose up -d
```

### 3. Backend

```bash
cd backend
set -a; source ../.env; set +a
mvn spring-boot:run
```

`JWT_SECRET` tanımlı değilse uygulama başlamaz. API `http://localhost:8080/api` adresinde,
Swagger arayüzü `http://localhost:8080/swagger-ui.html` adresinde çalışır.

Testler gömülü H2 veritabanı ile çalışır, PostgreSQL gerektirmez:

```bash
cd backend && mvn test
```

### Alternatif: Backend'i de Docker ile çalıştırma

```bash
docker compose --profile app up -d --build
```

Profil verilmezse (`docker compose up -d`) yalnızca PostgreSQL başlar.

> `POSTGRES_*` değerleri yalnızca veritabanı volume'ü **ilk kez** oluşturulurken uygulanır.
> Mevcut bir volume'ün şifresini `.env` ile değiştiremezsiniz; ya eski şifreyi kullanın ya da
> PostgreSQL içinde `ALTER USER lifetrack PASSWORD '...'` çalıştırın.

### 4. Mobil

```bash
cd mobile
flutter pub get
flutter run
```

API adresi Android emülatöründe otomatik olarak `10.0.2.2`, diğer platformlarda `localhost` olur.
Gerçek cihaz için:

```bash
flutter run --dart-define=API_BASE_URL=http://<bilgisayar-ip>:8080/api
```

## CI

`.github/workflows/ci.yml` her push (main) ve pull request'te çalışır:

- Backend: `mvn verify` ve Docker imajı derlemesi
- Mobil: `flutter analyze` ve `flutter test`

## Veritabanı

Şema Flyway migration'larıyla yönetilir. Ayrıntılar ve yeni migration ekleme adımları:
[docs/database.md](docs/database.md)

## API

Endpoint listesi için [docs/api.md](docs/api.md) dosyasına bakın.
