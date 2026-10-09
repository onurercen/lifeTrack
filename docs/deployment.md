# Yayına alma

Uygulama tek bir adresten sunulur:

| Adres | Ne |
|-------|----|
| `https://<alan-adı>/` | Flutter web uygulaması (telefonda "Ana ekrana ekle" ile uygulama gibi açılır) |
| `https://<alan-adı>/api/...` | Backend API |

Web uygulaması ve API aynı origin'de olduğu için CORS gerekmez. Android ve iOS uygulamaları da aynı API'yi kullanır
(bkz. [Android ve iOS uygulaması](#android-ve-ios-uygulaması)).

## Ortam değişkenleri

| Değişken | Zorunlu | Açıklama |
|----------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | evet | `prod` olmalı (`application-prod.yml`) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | evet | PostgreSQL bağlantısı, ör. `jdbc:postgresql://host:5432/lifetrack` |
| `JWT_SECRET` | evet | En az 32 bayt. Üret: `openssl rand -base64 48`. Değişirse herkes yeniden giriş yapar |
| `SPRING_MAIL_HOST` | evet | SMTP sunucusu. Doğrulama ve şifre sıfırlama kodları e-postayla gider; prod profili bu olmadan başlamaz |
| `SPRING_MAIL_PORT` | hayır | Varsayılan `587` (STARTTLS) |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | sağlayıcıya göre | SMTP kullanıcı adı ve şifresi (Gmail'de "uygulama şifresi") |
| `MAIL_FROM` | evet | Gönderen, ör. `LifeTrack <no-reply@alan-adi.com>`. Çoğu sağlayıcı doğrulanmış bir adres/alan adı ister |
| `TZ` | evet | `Europe/Istanbul`. Koşu tarihleri saat dilimsiz saklanır; sunucu kullanıcıyla aynı saat diliminde olmalı |
| `JWT_EXPIRATION_MS` | hayır | Access token süresi, varsayılan `900000` (15 dk) |
| `JWT_REFRESH_EXPIRATION_DAYS` | hayır | Refresh token süresi, varsayılan `30` |
| `TRUSTED_PROXIES` | hayır | `X-Forwarded-For`'una güvenilecek proxy IP'leri (regex). Varsayılan: özel ağ adresleri |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | hayır | Prod'da varsayılan boş. Yalnızca API'yi başka bir web adresinden çağıracaksan doldur |
| `LOGIN_MAX_FAILURES_PER_ACCOUNT`, `LOGIN_MAX_FAILURES_PER_IP`, `LOGIN_FAILURE_WINDOW_MINUTES` | hayır | Hatalı şifre sınırları (5 / 20 / 15 dk) |

Prod profili şunları yapar: Swagger/API dokümanını kapatır, hata yanıtlarında iç ayrıntı göstermez, istemci IP'sini
reverse proxy'nin `X-Forwarded-For` başlığından alır (giriş denemesi sınırı için gerekli) ve
`/actuator/health` sağlık kontrolünü açar. Backend **doğrudan internete açılmamalı**; yalnızca proxy'nin arkasında
çalışmalı, yoksa istemciler `X-Forwarded-For` ile IP'lerini taklit edebilir.

## Seçenek A: Kendi sunucun (VPS) — `docker-compose.prod.yml`

Hazır yapı: Caddy (otomatik HTTPS + web uygulaması + `/api` yönlendirmesi), backend, PostgreSQL ve günlük yedek.
Backend ve veritabanı dışarıya port açmaz.

1. Docker kurulu bir Linux sunucu al (1 GB RAM yeterli, 2 GB rahat). 80 ve 443 portlarını aç.
2. Alan adının DNS'inde bir `A` kaydını sunucunun IP'sine yönlendir.
3. Sunucuda:
   ```bash
   git clone https://github.com/onurercen/lifeTrack.git && cd lifeTrack
   cp deploy/.env.prod.example .env.prod
   # .env.prod içinde DOMAIN, POSTGRES_PASSWORD ve JWT_SECRET değerlerini doldur
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
   ```
4. `https://<alan-adı>` adresini aç. Caddy sertifikayı ilk istekte alır (birkaç saniye sürebilir).

Güncelleme:
```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```
Flyway migration'ları backend açılırken otomatik çalışır.

Loglar: `docker compose -f docker-compose.prod.yml logs -f backend`

## Seçenek B: Railway / Fly.io gibi platformlar

Bu platformlar HTTPS'i ve proxy'yi kendileri sağlar.

- **Backend**: `backend/Dockerfile` ile deploy et. Ortam değişkenlerini yukarıdaki tablodan gir.
  Sağlık kontrolü yolu: `/actuator/health`. Port: `8080`.
- **Veritabanı**: platformun yönetilen PostgreSQL 16'sını ekle ve bağlantı bilgisini `DB_URL` biçimine çevir
  (`jdbc:postgresql://<host>:<port>/<db>`), kullanıcı adı ve şifreyi ayrı değişkenlere yaz.
- **Web uygulaması**: `mobile/Dockerfile` Caddy ile statik dosyaları sunar, ama `/api` yönlendirmesi için
  `deploy/Caddyfile` ve `DOMAIN` gerekir. Platformda en kolayı web uygulamasını ayrı bir servis yerine, platformun
  yönlendirme özelliğiyle aynı alan adında `/api`'yi backend'e göndermektir. Aynı alan adı mümkün değilse web
  uygulamasını `--dart-define=API_BASE_URL=https://<api-adresi>/api` ile derle ve backend'de
  `CORS_ALLOWED_ORIGIN_PATTERNS=https://<web-adresi>` ayarla.
- **Proxy IP'si**: platformun proxy'si özel ağ adresinden gelmiyorsa `TRUSTED_PROXIES` ayarlanmalı, yoksa giriş
  sınırı tüm kullanıcılar için ortak işler. Platformun dokümanında proxy adres aralığına bak.
- **Yedek**: yönetilen PostgreSQL'in otomatik yedeğini aç (`backup` servisi yalnızca Seçenek A'da var).

## Yedekleme ve geri yükleme (Seçenek A)

`backup` servisi açılışta ve sonra her `BACKUP_INTERVAL_HOURS` saatte bir `pg_dump` alır, `./backups` altına
`lifetrack-YYYY-MM-DD_HHMM.dump` olarak yazar ve `BACKUP_KEEP_DAYS` günden eski dosyaları siler.

Yedekler aynı sunucuda durur; sunucu kaybolursa yedekler de gider. Düzenli olarak başka bir yere kopyala, örneğin
kendi bilgisayarından:
```bash
rsync -av kullanici@sunucu:lifeTrack/backups/ ~/lifetrack-backups/
```

Geri yükleme (mevcut verinin **üzerine yazar**):
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod stop backend
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres \
  pg_restore --clean --if-exists -U lifetrack -d lifetrack < backups/lifetrack-2026-10-07_0300.dump
docker compose -f docker-compose.prod.yml --env-file .env.prod start backend
```

## E-posta (SMTP)

Kayıt sonrası doğrulama ve şifre sıfırlama kodları e-postayla gönderilir, bu yüzden prod'da SMTP zorunludur.
Herhangi bir SMTP sağlayıcısı olur:

- **Gmail**: `smtp.gmail.com`, port `587`, kullanıcı adı Gmail adresi, şifre olarak Google hesabından alınan
  [uygulama şifresi](https://myaccount.google.com/apppasswords) (2 adımlı doğrulama açık olmalı). Kişisel kullanım
  için yeterli, günlük gönderim sınırı var.
- **Brevo, Mailgun, Amazon SES, Resend** gibi servisler: kendi alan adından göndermek için uygundur. Sağlayıcının
  verdiği SPF/DKIM DNS kayıtlarını eklemezsen e-postalar spam'e düşebilir.

E-posta gönderimi arka planda yapılır. SMTP hatası isteği bozmaz, yalnızca backend loguna yazılır:
```bash
docker compose -f docker-compose.prod.yml logs backend | grep "Could not send e-mail"
```
SMTP sunucusuna ulaşılamaması sağlık kontrolünü etkilemez.

Geliştirmede `SPRING_MAIL_HOST` boş bırakılabilir. Bu durumda e-postalar gönderilmez, içerikleri (kod dahil)
backend loguna yazılır.

## Android ve iOS uygulaması

`mobile/` altında Android ve iOS projeleri var (paket adı `com.lifetrack.lifetrack_mobile`). Uygulama API adresini
derleme sırasında alır:

```bash
cd mobile
flutter build apk --release --dart-define=API_BASE_URL=https://<alan-adı>/api        # Android APK
flutter build appbundle --release --dart-define=API_BASE_URL=https://<alan-adı>/api  # Google Play
flutter build ipa --release --dart-define=API_BASE_URL=https://<alan-adı>/api        # iOS (macOS + Xcode)
```

- Release derlemeleri yalnızca HTTPS ile çalışır. Düz HTTP'ye yalnızca Android debug derlemesinde ve iOS'ta yerel
  ağ adreslerinde izin verilir (geliştirme backend'i için).
- Mağazaya yüklemeden önce yapılacaklar: Android için imzalama anahtarı oluşturup `android/app/build.gradle.kts`
  içindeki `release` imzalama ayarını değiştirmek (şu an debug anahtarıyla imzalanıyor), iOS için Xcode'da
  takım (Team) ve bundle id seçmek. İkon için `android/app/src/main/res/mipmap-*` ve
  `ios/Runner/Assets.xcassets/AppIcon.appiconset` altındaki varsayılan Flutter ikonları değiştirilmeli.
- Native uygulama sunucuya farklı bir origin'den gelmez, bu yüzden CORS ayarı gerekmez.

## Yerelde deneme

Aynı yapı bilgisayarda `DOMAIN=localhost` ile çalışır (Caddy kendi sertifikasını üretir, tarayıcı uyarı verir):
```bash
cp deploy/.env.prod.example .env.prod   # DOMAIN=localhost, HTTP_PORT=8081, HTTPS_PORT=8443 ekle; şifreleri doldur
docker compose -p lifetrack-prod -f docker-compose.prod.yml --env-file .env.prod up -d --build
open https://localhost:8443
```
`-p` ile ayrı bir proje adı vermek, geliştirme için kullanılan `docker compose` veritabanına dokunmamasını sağlar.

## Yayın öncesi kontrol listesi

- [ ] `JWT_SECRET` ve `POSTGRES_PASSWORD` rastgele ve güçlü, `.env.prod` repoya girmiyor
- [ ] `SPRING_PROFILES_ACTIVE=prod` ve `TZ=Europe/Istanbul`
- [ ] SMTP ayarlı; yeni bir hesapla kayıt olunca doğrulama kodu geliyor (spam klasörüne de bak)
- [ ] Backend ve PostgreSQL portları internete kapalı (yalnızca 80/443 açık)
- [ ] `https://<alan-adı>` açılıyor; sağlık kontrolü dışarıdan erişilemiyor (`/api/actuator/health` → `401`)
- [ ] İlk yedek `./backups` altında oluştu; bir kez geri yükleme denendi
- [ ] Yedekler düzenli olarak sunucu dışına kopyalanıyor
