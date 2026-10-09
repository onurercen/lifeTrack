# LifeTrack

Koşu, kitap ve medya (film/dizi) takibini tek yerde toplayan kişisel takip uygulaması.

| Katman  | Teknoloji                                        |
|---------|--------------------------------------------------|
| Backend | Java 17, Spring Boot 3.5, Spring Security + JWT, JPA |
| Veritabanı | PostgreSQL 16 (Docker)                        |
| Mobil   | Flutter (Dart 3): Android, iOS ve web            |

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

`JWT_SECRET` tanımlı değilse uygulama başlamaz.

Kayıt sonrası e-posta doğrulama ve şifre sıfırlama kodları e-postayla gönderilir. Yerelde SMTP ayarlamak
zorunlu değildir: `SPRING_MAIL_HOST` boşsa e-postalar gönderilmez, içerikleri (kod dahil) backend loguna yazılır.
Gerçek gönderim için `.env` içindeki `SPRING_MAIL_*` satırlarını doldurun. API `http://localhost:8080/api` adresinde,
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

`flutter run` bağlı cihazı ya da emülatörü seçer (`-d chrome`, `-d android`, `-d ios` ile belirtilebilir).
Android için Android Studio/SDK, iOS için macOS ve Xcode gerekir.

API adresi Android emülatöründe otomatik olarak `10.0.2.2`, diğer platformlarda `localhost` olur.
Gerçek cihaz için:

```bash
flutter run --dart-define=API_BASE_URL=http://<bilgisayar-ip>:8080/api
```

## Yayına alma

Prod ayarları, `docker-compose.prod.yml` (HTTPS, web uygulaması, günlük yedek) ve platform seçenekleri:
[docs/deployment.md](docs/deployment.md)

## CI

`.github/workflows/ci.yml` her push (main) ve pull request'te çalışır:

- Backend: `mvn verify` ve Docker imajı derlemesi
- Mobil: `flutter analyze`, `flutter test`, web derlemesi ve Android (debug APK) derlemesi

## Veritabanı

Şema Flyway migration'larıyla yönetilir. Ayrıntılar ve yeni migration ekleme adımları:
[docs/database.md](docs/database.md)

## API

Endpoint listesi için [docs/api.md](docs/api.md) dosyasına bakın.
